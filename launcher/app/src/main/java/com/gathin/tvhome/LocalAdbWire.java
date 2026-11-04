package com.gathin.tvhome;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.interfaces.RSAPublicKey;

/** Small ADB packet codec. The caller, not incoming data, chooses the sole allowed service. */
final class LocalAdbWire {
    static int command(String name) {
        return name.charAt(0) | name.charAt(1) << 8 | name.charAt(2) << 16 | name.charAt(3) << 24;
    }
    static final class Packet {
        final int command, arg0, arg1;
        final byte[] body;
        Packet(int command, int arg0, int arg1, byte[] body) {
            this.command=command; this.arg0=arg0; this.arg1=arg1; this.body=body;
        }
    }
    static int checksum(byte[] bytes) { int sum=0; for(byte b:bytes) sum+=b&255; return sum; }
    static void write(OutputStream out, String name, int arg0, int arg1, byte[] body) throws IOException {
        int cmd=command(name);
        byte[] header=ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(cmd).putInt(arg0).putInt(arg1).putInt(body.length).putInt(checksum(body)).putInt(~cmd).array();
        out.write(header); out.write(body); out.flush();
    }
    static Packet read(InputStream in) throws IOException {
        DataInputStream data=new DataInputStream(in); byte[] header=new byte[24]; data.readFully(header);
        ByteBuffer h=ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        int cmd=h.getInt(), a=h.getInt(), b=h.getInt(), size=h.getInt(), crc=h.getInt(), magic=h.getInt();
        if (magic!=~cmd || size<0 || size>262144) throw new IOException("Invalid ADB header");
        byte[] body=new byte[size]; data.readFully(body);
        if (checksum(body)!=crc) throw new IOException("Invalid ADB checksum");
        return new Packet(cmd,a,b,body);
    }
    static byte[] publicKey(RSAPublicKey key) {
        BigInteger n=key.getModulus(), radix=BigInteger.ONE.shiftLeft(32);
        if (n.bitLength()!=2048 || !key.getPublicExponent().equals(BigInteger.valueOf(65537)))
            throw new IllegalArgumentException("ADB requires the configured RSA key size");
        int inverse=radix.subtract(n.modInverse(radix)).intValue();
        return ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(64).putInt(inverse).put(little(n,256))
                .put(little(BigInteger.ONE.shiftLeft(4096).mod(n),256)).putInt(65537).array();
    }
    private static byte[] little(BigInteger value, int count) {
        byte[] source=value.toByteArray(), result=new byte[count];
        for(int i=0;i<count && i<source.length;i++) result[i]=source[source.length-1-i];
        return result;
    }
}
