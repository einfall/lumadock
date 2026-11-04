package com.gathin.tvhome;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPublicKey;

/** Authenticated loopback ADB. No host or command can be supplied by UI, intents, or network data. */
final class LocalCecReader {
    private static final String ALIAS="tvhome_local_cec_adb_v1";
    private static final byte[] EMPTY=new byte[0];
    private static final byte[] DIGEST_INFO={0x30,0x21,0x30,0x09,0x06,0x05,0x2b,0x0e,0x03,0x02,0x1a,0x05,0x00,0x04,0x14};
    static final class AuthorizationNeeded extends IOException { }

    static String read(Runnable awaitingAuthorization) throws Exception {return readFixed(awaitingAuthorization,false);}
    /** Only the aggregate CPU counter line; same already authorized, device-local ADB identity. */
    static String readCpu() throws Exception {return readFixed(()->{},true);}
    private static String readFixed(Runnable awaitingAuthorization,boolean cpu) throws Exception {
        try (Socket socket=new Socket()) {
            socket.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),5555),800);
            socket.setSoTimeout(2500);
            InputStream in=socket.getInputStream(); OutputStream out=socket.getOutputStream();
            LocalAdbWire.write(out,"CNXN",0x01000000,4096,bytes("host::\0"));
            boolean signed=false, offered=false;
            KeyStore keys=null;
            for(int attempt=0;attempt<5;attempt++) {
                LocalAdbWire.Packet packet;
                try { packet=LocalAdbWire.read(in); }
                catch (SocketTimeoutException e) { if(offered) throw new AuthorizationNeeded(); throw e; }
                if(packet.command==LocalAdbWire.command("CNXN")) {
                    socket.setSoTimeout(1800);
                    return dump(in,out,cpu);
                }
                if(packet.command!=LocalAdbWire.command("AUTH") || packet.arg0!=1 || packet.body.length!=20)
                    throw new IOException("Unexpected ADB handshake");
                if(keys==null) keys=keyStore();
                if(!signed) {
                    Signature signature=Signature.getInstance("NONEwithRSA");
                    signature.initSign((PrivateKey)keys.getKey(ALIAS,null));
                    signature.update(DIGEST_INFO); signature.update(packet.body);
                    LocalAdbWire.write(out,"AUTH",2,0,signature.sign()); signed=true;
                } else if(!offered) {
                    RSAPublicKey key=(RSAPublicKey)keys.getCertificate(ALIAS).getPublicKey();
                    String publicKey=Base64.encodeToString(LocalAdbWire.publicKey(key),Base64.NO_WRAP)+" tvhome-local-cec\0";
                    LocalAdbWire.write(out,"AUTH",3,0,bytes(publicKey)); offered=true;
                    socket.setSoTimeout(15000); awaitingAuthorization.run();
                } else throw new AuthorizationNeeded();
            }
            throw new IOException("ADB handshake did not complete");
        }
    }

    private static String dump(InputStream in, OutputStream out,boolean cpu) throws IOException {
        // Two fixed read-only services. No caller-controlled shell command or path.
        LocalAdbWire.write(out,"OPEN",1,0,bytes(cpu?"shell:head -n 1 /proc/stat\0":"shell:dumpsys hdmi_control\0"));
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        long deadline=System.nanoTime()+5_000_000_000L;
        while(System.nanoTime()<deadline) {
            LocalAdbWire.Packet p=LocalAdbWire.read(in);
            if(p.arg1!=1) throw new IOException("Unexpected ADB stream");
            if(p.command==LocalAdbWire.command("WRTE")) {
                if(output.size()+p.body.length>131072) throw new IOException("CEC dump too large");
                output.write(p.body);
                LocalAdbWire.write(out,"OKAY",1,p.arg0,EMPTY);
            } else if(p.command==LocalAdbWire.command("CLSE")) {
                LocalAdbWire.write(out,"CLSE",1,p.arg0,EMPTY);
                return output.toString("UTF-8");
            } else if(p.command!=LocalAdbWire.command("OKAY")) throw new IOException("Unexpected ADB packet");
        }
        throw new IOException("CEC read timed out");
    }

    private static KeyStore keyStore() throws Exception {
        KeyStore keys=KeyStore.getInstance("AndroidKeyStore"); keys.load(null);
        if(!keys.containsAlias(ALIAS)) {
            KeyPairGenerator generator=KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA,"AndroidKeyStore");
            generator.initialize(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_SIGN)
                    .setKeySize(2048).setDigests(KeyProperties.DIGEST_NONE)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    .setUserAuthenticationRequired(false).build());
            generator.generateKeyPair();
        }
        return keys;
    }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
}
