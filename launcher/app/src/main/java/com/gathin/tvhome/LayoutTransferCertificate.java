package com.gathin.tvhome;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Encodes the fixed fields of the ephemeral X.509 certificate for one TLS share. */
final class LayoutTransferCertificate {
    private static final int SEQUENCE = 0x30;
    private static final long DAY = 24 * 60 * 60_000L;

    private LayoutTransferCertificate() {}

    static X509Certificate create(KeyPair key) throws GeneralSecurityException {
        byte[] algorithm = der(SEQUENCE, der(0x06,
                new byte[]{0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 1, 1, 11}), der(0x05));
        byte[] name = der(SEQUENCE, der(0x31, der(SEQUENCE, der(0x06, new byte[]{0x55, 0x04, 0x03}),
                der(0x0c, "Luma Dock".getBytes(StandardCharsets.UTF_8)))));
        BigInteger serial = new BigInteger(128, new SecureRandom()).add(BigInteger.ONE);
        long now = System.currentTimeMillis();
        byte[] tbs = der(SEQUENCE, der(0xa0, der(0x02, new byte[]{2})), der(0x02, serial.toByteArray()),
                algorithm, name, der(SEQUENCE, time(now - DAY), time(now + DAY)), name, key.getPublic().getEncoded());
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(key.getPrivate());
        signer.update(tbs);
        byte[] encoded = der(SEQUENCE, tbs, algorithm, der(0x03, new byte[]{0}, signer.sign()));
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(encoded));
    }

    private static byte[] time(long millis) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss'Z'", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return der(0x18, format.format(new Date(millis)).getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] der(int tag, byte[]... parts) {
        int length = 0;
        for (byte[] part : parts) length += part.length;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(length + 4);
        bytes.write(tag);
        if (length < 128) {
            bytes.write(length);
        } else {
            int count = length <= 255 ? 1 : 2;
            bytes.write(0x80 | count);
            if (count == 2) bytes.write(length >>> 8);
            bytes.write(length & 0xff);
        }
        for (byte[] part : parts) bytes.write(part, 0, part.length);
        return bytes.toByteArray();
    }
}
