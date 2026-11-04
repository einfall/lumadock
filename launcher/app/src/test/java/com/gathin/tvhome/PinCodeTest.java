package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class PinCodeTest {
 @Test public void acceptsOnlyFourAsciiDigits(){assertTrue(PinCode.valid("0123"));assertFalse(PinCode.valid("123"));assertFalse(PinCode.valid("12345"));assertFalse(PinCode.valid("１２３４"));assertFalse(PinCode.valid(null));}
 @Test public void saltedVerificationAndWrongPin(){String a=PinCode.create("0123"),b=PinCode.create("0123");assertNotEquals(a,b);assertTrue(PinCode.verify("0123",a));assertFalse(PinCode.verify("0124",a));assertTrue(a.startsWith("1:"));}
 @Test public void malformedVerifiersFailClosed(){for(String s:new String[]{"","1:a:b","2:AAAA:AAAA","1:AAAA:AAAA","garbage"})assertFalse(PinCode.verify("0123",s));assertFalse(PinCode.verify("0000",null));}
}
