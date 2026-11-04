package com.gathin.tvhome;
/** Internal widget source registry. Instances share data; a new source has one stable ID and metric mask. */
enum WidgetSource {
    CPU("cpu",1,"cpu"), RAM("ram",2,"memory"), STORAGE("storage",4,"storage"), SYSTEM("system",7,"cpu"), QUOTE("quote",0,"text");
    final String id,glyph;final int mask;
    WidgetSource(String id,int mask,String glyph){this.id=id;this.mask=mask;this.glyph=glyph;}
    static WidgetSource find(String id){for(WidgetSource s:values())if(s.id.equals(id))return s;return null;}
    static int demand(boolean legacy,int mask,int external){return (legacy?mask:0)|(external&7);}
    static int width(String source,int value,int screen){return Math.max(1,Math.min(Math.max(1,screen),Math.max(("system".equals(source)||"quote".equals(source))?240:120,Math.min("android".equals(source)?Math.max(1,screen):480,value))));}
}
