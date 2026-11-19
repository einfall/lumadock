package com.gathin.tvhome;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONObject;

/** Official iframe playback with the installed app identity, shared by preview and Home. */
final class YouTubeEmbed {
    interface Listener {
        void update(int state, double position, double duration);
        void error(String message);
    }

    final WebView view;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private boolean closed, failed;
    private final Runnable timeout = () -> fail("YouTube lädt zu lange. Internet und Android System WebView prüfen.");

    @SuppressLint("SetJavaScriptEnabled")
    YouTubeEmbed(Context context, String id, boolean controls, boolean loop, Listener listener) {
        if (!YouTubeVideo.validId(id)) throw new IllegalArgumentException("Invalid video");
        this.listener = listener;
        view = new WebView(context);
        view.setBackgroundColor(Color.BLACK);
        view.setFocusable(controls);
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        view.addJavascriptInterface(new Bridge(), "LumaYouTube");
        view.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView web, WebResourceRequest request) {
                return request.isForMainFrame();
            }
            @Override public boolean onRenderProcessGone(WebView web, RenderProcessGoneDetail detail) {
                fail("Der YouTube-Player wurde von Android beendet.");
                return true;
            }
        });
        String origin = "https://" + context.getPackageName().toLowerCase(java.util.Locale.ROOT);
        String html = """
                <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
                <meta name="referrer" content="strict-origin-when-cross-origin">
                <style>html,body,#player{margin:0;width:100%;height:100%;background:#000;overflow:hidden}</style>
                </head><body><div id="player"></div><script>
                var player,ready=false;
                function report(){if(!ready)return;try{LumaYouTube.state(player.getPlayerState(),
                  player.getCurrentTime(),player.getDuration());}catch(e){}}
                function onYouTubeIframeAPIReady(){player=new YT.Player('player',{
                  videoId:VIDEO_ID,width:'100%',height:'100%',
                  playerVars:{autoplay:1,controls:CONTROLS,playsinline:1,rel:0,origin:ORIGIN,
                    loop:LOOP,playlist:VIDEO_ID},events:{
                    onReady:function(e){ready=true;e.target.mute();e.target.playVideo();report();},
                    onStateChange:report,
                    onError:function(e){LumaYouTube.error(Number(e.data));},
                    onAutoplayBlocked:function(){LumaYouTube.error(-2);}
                  }});}
                function toggle(){if(!ready)return;if(player.getPlayerState()===1)player.pauseVideo();else player.playVideo();}
                function seekBy(seconds){if(!ready)return;var end=player.getDuration();
                  var target=Math.max(0,player.getCurrentTime()+seconds);
                  if(end>0)target=Math.min(end-0.2,target);player.seekTo(target,true);report();}
                function sound(on){if(ready){if(on)player.unMute();else player.mute();}}
                setInterval(report,750);
                </script><script src="https://www.youtube.com/iframe_api"></script></body></html>
                """.replace("VIDEO_ID", JSONObject.quote(id))
                .replace("CONTROLS", controls ? "1" : "0")
                .replace("LOOP", loop ? "1" : "0")
                .replace("ORIGIN", JSONObject.quote(origin));
        view.loadDataWithBaseURL(origin + "/", html, "text/html", "UTF-8", null);
        main.postDelayed(timeout, 30000);
    }

    void toggle() { script("toggle()"); }
    void seek(int seconds) { script("seekBy(" + seconds + ")"); }
    void sound(boolean enabled) { script("sound(" + enabled + ")"); }
    private void script(String script) { if (!closed) view.evaluateJavascript(script, null); }

    private void fail(String message) {
        main.removeCallbacks(timeout);
        failed = true;
        if (!closed) listener.error(message);
    }

    final class Bridge {
        @JavascriptInterface public void state(int state, double position, double duration) {
            if (!Double.isFinite(position) || !Double.isFinite(duration) || position < 0 || duration < 0) return;
            main.post(() -> {
                if (closed) return;
                if (state == 1) failed = false;
                if (failed) return;
                if (state == 1 || state == 2 || state == 0) main.removeCallbacks(timeout);
                listener.update(state, position, duration);
            });
        }
        @JavascriptInterface public void error(int code) {
            main.post(() -> {
                String message;
                switch (code) {
                    case 100: message = "Das Video ist nicht mehr verfügbar oder privat."; break;
                    case 101: case 150: message = "Dieses Video erlaubt keine eingebettete Wiedergabe."; break;
                    case 153: message = "YouTube hat die Player-Identifikation abgelehnt. Android System WebView aktualisieren."; break;
                    case -2: message = "YouTube wartet auf einen Start. Bitte Wiedergabe drücken."; break;
                    default: message = "YouTube kann das Video auf diesem Gerät nicht abspielen (" + code + ").";
                }
                fail(message);
            });
        }
    }

    void close() {
        if (closed) return;
        closed = true;
        main.removeCallbacksAndMessages(null);
        view.removeJavascriptInterface("LumaYouTube");
        view.stopLoading();
        view.onPause();
        if (view.getParent() instanceof ViewGroup) ((ViewGroup) view.getParent()).removeView(view);
        view.destroy();
    }
}
