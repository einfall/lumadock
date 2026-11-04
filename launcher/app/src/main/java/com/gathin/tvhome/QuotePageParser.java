package com.gathin.tvhome;
import java.net.URI;
import java.util.*;
import java.util.regex.*;
/** Only the first quote article is used; logos, advertisements and later quotes are ignored. */
final class QuotePageParser {
    static final int MAX_HTML=524288;
    static final class Entry {final String text,image;Entry(String text,String image){this.text=text;this.image=image;}}
    static Entry parse(String html){
        if(html==null||html.length()>MAX_HTML)throw new IllegalArgumentException("Ungültige Seitengröße");
        Matcher articles=Pattern.compile("<article\\b([^>]*)>([\\s\\S]*?)</article\\s*>",Pattern.CASE_INSENSITIVE).matcher(html);
        while(articles.find()){
            String classes=attr(articles.group(1),"class");if(!Arrays.asList(classes.split("\\s+")).contains("quote"))continue;
            String body=articles.group(2);if(body.length()>65536)throw new IllegalArgumentException("Spruch zu groß");
            Matcher image=Pattern.compile("<img\\b([^>]*)>",Pattern.CASE_INSENSITIVE).matcher(body);if(!image.find())throw new IllegalArgumentException("Spruchbild fehlt");
            String imageUrl=decode(attr(image.group(1),"src"));if(!safeImage(imageUrl))throw new IllegalArgumentException("Unbekannte Bildquelle");
            Matcher h=Pattern.compile("<h2\\b[^>]*>([\\s\\S]*?)</h2\\s*>",Pattern.CASE_INSENSITIVE).matcher(body);
            String text=decode(h.find()?h.group(1).replaceAll("<[^>]*>"," "):attr(image.group(1),"alt")).replace('\u00a0',' ').replaceAll("\\s+"," ").trim();
            if(text.isEmpty()||text.length()>1600)throw new IllegalArgumentException("Spruchtext fehlt oder zu lang");
            return new Entry(text,imageUrl);
        }throw new IllegalArgumentException("Kein Spruch gefunden");
    }
    private static String attr(String tag,String name){Matcher m=Pattern.compile("(?:^|\\s)"+name+"\\s*=\\s*([\"'])([\\s\\S]*?)\\1",Pattern.CASE_INSENSITIVE).matcher(tag);return m.find()?m.group(2):"";}
    static boolean safeImage(String url){try{URI u=new URI(url);return "https".equalsIgnoreCase(u.getScheme())&&"www.spruch-des-tages.de".equalsIgnoreCase(u.getHost())&&u.getRawUserInfo()==null&&(u.getPort()==-1||u.getPort()==443)&&u.getPath().startsWith("/wp-content/uploads/quotes/")&&!u.getPath().contains("..");}catch(Exception e){return false;}}
    static String decode(String text){Matcher m=Pattern.compile("&(#x[0-9a-fA-F]+|#[0-9]+|[A-Za-z]+);").matcher(text);StringBuffer out=new StringBuffer();while(m.find()){String e=m.group(1),v=null;try{if(e.startsWith("#")){int cp=Integer.parseInt(e.substring(e.startsWith("#x")?2:1),e.startsWith("#x")?16:10);if(Character.isValidCodePoint(cp)&&cp>=32)v=new String(Character.toChars(cp));}}catch(RuntimeException ignored){}if(v==null){switch(e){case "amp":v="&";break;case "quot":v="\"";break;case "apos":v="'";break;case "lt":v="<";break;case "gt":v=">";break;case "nbsp":v=" ";break;case "auml":v="ä";break;case "ouml":v="ö";break;case "uuml":v="ü";break;case "Auml":v="Ä";break;case "Ouml":v="Ö";break;case "Uuml":v="Ü";break;case "szlig":v="ß";break;case "ndash":v="–";break;case "mdash":v="—";break;case "hellip":v="…";break;case "lsquo":case "rsquo":v="’";break;case "ldquo":case "rdquo":v="“";break;default:v=m.group();}}m.appendReplacement(out,Matcher.quoteReplacement(v));}m.appendTail(out);return out.toString();}
}
