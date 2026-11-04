package com.gathin.tvhome;
import org.junit.Test;import static org.junit.Assert.*;
public class ContrastColorTest {
 @Test public void whiteLinesGetDarkContoursAndDarkLinesLightContours(){assertEquals(ContrastColor.DARK,ContrastColor.against(0xFFFFFFFF));assertEquals(ContrastColor.LIGHT,ContrastColor.against(0xFF000000));assertEquals(ContrastColor.DARK,ContrastColor.against(0xFFF8F8F8));assertEquals(ContrastColor.LIGHT,ContrastColor.against(0xFF102033));}
 @Test public void opacityDoesNotAlterContrastChoice(){assertEquals(ContrastColor.against(0xFF519EFF),ContrastColor.against(0x12519EFF));}
 @Test public void selectedNeutralAlwaysHasTheBetterContrast(){for(int c=0;c<=0xFFFFFF;c+=7919){int chosen=ContrastColor.against(c);int other=chosen==ContrastColor.DARK?ContrastColor.LIGHT:ContrastColor.DARK;assertTrue(ContrastColor.ratio(c,chosen)>=ContrastColor.ratio(c,other));assertTrue(ContrastColor.ratio(c,chosen)>3.9);}}
 @Test public void addingContourDoesNotEnableAnOtherwiseDisabledFrame(){TileEffects e=new TileEffects(false,false,false,false,"dominant",3,true,9);assertFalse(e.line());assertTrue(e.contrast);assertEquals(3,e.contrastWidth);assertEquals(3,e.gap);assertEquals(1,new TileEffects(true,false,false,true,"white",0,true,-1).contrastWidth);}
}
