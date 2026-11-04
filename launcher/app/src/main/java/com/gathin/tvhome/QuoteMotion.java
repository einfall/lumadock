package com.gathin.tvhome;
/** Coordinates remain in widget space; screen paths cross the real root bounds before disappearing. */
final class QuoteMotion {
    static String path(String id){return "screen".equals(id)?"screen":"widget";}
    static float enter(float target,float origin,float screen,float spread,float q){float ease=1-(1-q)*(1-q)*(1-q);return (-origin-4-spread)*(1-ease)+target*ease;}
    static float exit(float target,float origin,float screen,float spread,float q){return target+(screen-origin+4+spread-target)*q*q;}
    static float alpha(boolean entering,boolean screen,float q){if(entering)return Math.min(1,q*8);return screen?Math.min(1,(1-q)*10):1-q;}
}
