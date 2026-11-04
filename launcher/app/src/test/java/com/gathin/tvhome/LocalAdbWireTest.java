package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.*;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.math.BigInteger;

public class LocalAdbWireTest {
    @Test public void framingMatchesAdbLittleEndianHeader() throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        LocalAdbWire.write(out,"WRTE",7,1,new byte[]{1,2,(byte)255});
        byte[] wire=out.toByteArray();
        assertEquals(27,wire.length); assertEquals('W',wire[0]); assertEquals(7,wire[4]);
        assertEquals(3,wire[12]); assertEquals(2,wire[16]); assertEquals(1,wire[17]);
        LocalAdbWire.Packet p=LocalAdbWire.read(new ByteArrayInputStream(wire));
        assertEquals(7,p.arg0); assertEquals(1,p.arg1); assertArrayEquals(new byte[]{1,2,(byte)255},p.body);
    }
    @Test(expected=IOException.class) public void corruptedPayloadRejected() throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        LocalAdbWire.write(out,"AUTH",1,0,new byte[]{1,2});
        byte[] wire=out.toByteArray();wire[25]=3;
        LocalAdbWire.read(new ByteArrayInputStream(wire));
    }
    @Test(expected=IOException.class) public void hugeLengthRejectedBeforeAllocation() throws Exception {
        byte[] header=ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(LocalAdbWire.command("WRTE")).putInt(1).putInt(1).putInt(Integer.MAX_VALUE)
            .putInt(0).putInt(~LocalAdbWire.command("WRTE")).array();
        LocalAdbWire.read(new ByteArrayInputStream(header));
    }
    @Test(expected=IOException.class) public void wrongMagicRejected() throws Exception {
        LocalAdbWire.read(new ByteArrayInputStream(new byte[24]));
    }
    @Test public void rsaPublicKeyUsesAndroidAdbRepresentation() throws Exception {
        KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);
        RSAPublicKey key=(RSAPublicKey)generator.generateKeyPair().getPublic();
        byte[] encoded=LocalAdbWire.publicKey(key);
        ByteBuffer data=ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(524,encoded.length); assertEquals(64,data.getInt());
        long inverse=Integer.toUnsignedLong(data.getInt());
        byte[] modulus=new byte[256];data.get(modulus);
        byte[] big=new byte[256];for(int i=0;i<256;i++)big[i]=modulus[255-i];
        assertEquals(key.getModulus(),new BigInteger(1,big));
        assertEquals(0xFFFFFFFFL,(inverse*key.getModulus().longValue()) & 0xFFFFFFFFL);
        data.position(520);assertEquals(65537,data.getInt());
    }
}
