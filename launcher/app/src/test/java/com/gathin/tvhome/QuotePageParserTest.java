package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class QuotePageParserTest {
    private static final String URL="https://www.spruch-des-tages.de/wp-content/uploads/quotes/first.jpg";
    private String quote(String text,String image){return "<article class='card quote mb-5'><img src='"+image+"' alt='fallback'><h2 class='quote-loop-title'><a href='/q'>"+text+"</a></h2></article>";}
    @Test public void choosesFirstQuoteNotLogoAdOrSecondQuote(){QuotePageParser.Entry e=QuotePageParser.parse("<img src='logo.svg'><article class='ad'>ad</article>"+quote("Erster Spruch",URL)+quote("Zweiter Spruch",URL.replace("first","second")));assertEquals("Erster Spruch",e.text);assertEquals(URL,e.image);}
    @Test public void imageAltIsFallback(){assertEquals("Schöner Tag",QuotePageParser.parse("<article class=\"quote\"><img src=\""+URL+"\" alt=\"Sch&ouml;ner Tag\"></article>").text);}
    @Test public void decodesEntitiesAndNormalizesTitleMarkup(){assertEquals("Glück & Freude ‘A’ …",QuotePageParser.parse(quote(" Gl&uuml;ck &amp; <em>Freude</em> &#8216;A&#8217; &hellip; ",URL)).text);}
    @Test public void onlyExactQuoteClassMatches(){assertEquals("Echt",QuotePageParser.parse("<article class='quote-ad'><img src='logo'></article>"+quote("Echt",URL)).text);}
    @Test public void externalAndTrickyImageSourcesAreRejected(){for(String url:new String[]{"http://www.spruch-des-tages.de/wp-content/uploads/quotes/x.jpg","https://evil.example/x.jpg","https://www.spruch-des-tages.de.evil.test/wp-content/uploads/quotes/x.jpg","https://u@www.spruch-des-tages.de/wp-content/uploads/quotes/x.jpg","https://www.spruch-des-tages.de:444/wp-content/uploads/quotes/x.jpg","https://www.spruch-des-tages.de/wp-content/uploads/quotes/../secret"}){assertFalse(url,QuotePageParser.safeImage(url));try{QuotePageParser.parse(quote("Text",url));fail(url);}catch(IllegalArgumentException expected){}}}
    @Test public void malformedFirstQuoteDoesNotSilentlySkipToSecond(){try{QuotePageParser.parse("<article class='quote'><img src='bad'></article>"+quote("Zweiter",URL));fail();}catch(IllegalArgumentException expected){}}
    @Test public void boundedPageAndTextAreRequired(){for(String page:new String[]{"",null,"x".repeat(QuotePageParser.MAX_HTML+1),quote(" ",URL),quote("a".repeat(1601),URL)})try{QuotePageParser.parse(page);fail();}catch(IllegalArgumentException expected){}}
    @Test public void quotesDoNotStartSystemSamplingAndPixelFontIsAvailable(){assertEquals(0,WidgetSource.QUOTE.mask);assertEquals("pixelify_sans",FontPolicy.id("pixelify_sans"));assertEquals(240,WidgetSource.width("quote",1,960));}
}
