package com.gathin.tvhome;
/** One authoritative switch for the focused overlay; older boolean settings still migrate lazily. */
final class FocusFrameMode {
    static String parse(String value,boolean legacy){
        if("moving".equals(value)||"fade".equals(value)||"instant".equals(value)||"none".equals(value))return value;
        return legacy?"moving":"none";
    }
}
