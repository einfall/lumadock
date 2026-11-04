package com.gathin.tvhome;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
/** Salted PIN verifier; plaintext is never persisted. */
final class PinCode {
    static boolean valid(String value) { return value!=null&&value.matches("[0-9]{4}"); }
    static String create(String pin) {
        if(!valid(pin))throw new IllegalArgumentException("Four digits required");
        byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);
        return "1:"+Base64.getEncoder().encodeToString(salt)+":"+Base64.getEncoder().encodeToString(derive(pin,salt));
    }
    static boolean verify(String pin,String record) {
        if(!valid(pin)||record==null)return false;
        try {
            String[] f=record.split(":");if(f.length!=3||!f[0].equals("1"))return false;
            byte[] salt=Base64.getDecoder().decode(f[1]),hash=Base64.getDecoder().decode(f[2]);
            return salt.length==16&&hash.length==32&&MessageDigest.isEqual(hash,derive(pin,salt));
        }catch(IllegalArgumentException e){return false;}
    }
    private static byte[] derive(String pin,byte[] salt) {
        PBEKeySpec spec=new PBEKeySpec(pin.toCharArray(),salt,60000,256);
        try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}
        catch(java.security.GeneralSecurityException e){throw new IllegalStateException(e);}
        finally{spec.clearPassword();}
    }
}
