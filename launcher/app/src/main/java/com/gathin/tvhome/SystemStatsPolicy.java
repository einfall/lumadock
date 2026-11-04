package com.gathin.tvhome;
/** Total CPU counters and ratios; missing or reset counters never become a fabricated percentage. */
final class SystemStatsPolicy {
    static final class Cpu {final long total,idle;Cpu(long t,long i){total=t;idle=i;}}
    static Cpu parse(String line){
        if(line==null)return null;String[] fields=line.trim().split("\\s+");
        if(fields.length<5||!"cpu".equals(fields[0]))return null;
        try{long total=0,idle=0;for(int i=1;i<Math.min(fields.length,9);i++){long n=Long.parseLong(fields[i]);if(n<0)return null;total=Math.addExact(total,n);if(i==4||i==5)idle=Math.addExact(idle,n);}return new Cpu(total,idle);}catch(NumberFormatException|ArithmeticException ex){return null;}
    }
    static int cpu(Cpu before,Cpu after){
        if(before==null||after==null)return -1;long dt=after.total-before.total,di=after.idle-before.idle;
        if(dt<=0||di<0||di>dt)return -1;return (int)Math.round(100.0*(dt-di)/dt);
    }
    static int used(long available,long total){if(total<=0||available<0)return -1;return (int)Math.round(100.0*(total-Math.min(available,total))/total);}
    static int seconds(int value){return Math.max(5,Math.min(60,value));}
}
