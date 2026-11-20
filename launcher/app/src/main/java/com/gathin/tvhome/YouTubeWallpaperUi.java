package com.gathin.tvhome;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import androidx.webkit.ScriptHandler;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import java.util.Arrays;
import java.util.HashSet;

/** Cosmetic background-only presentation, installed in the YouTube frame before its first paint. */
final class YouTubeWallpaperUi {
    private static final String SCRIPT = """
            (() => {
              const css = '.html5-video-player:not(.ad-showing):not(.ad-interrupting) :is(' +
                '.ytp-chrome-top,.ytp-bezel,.ytp-large-play-button,.ytp-pause-overlay,' +
                '.ytp-chrome-bottom,.ytp-gradient-top,.ytp-gradient-bottom)' +
                ',body:has(.html5-video-player:not(.ad-showing):not(.ad-interrupting)) ' +
                '.ytPlayerControlsContainerHost :is(.ytwPlayerTopControlsHost,.ytwPlayerMiddleControlsHost,' +
                '.ytwPlayerBottomControlsHost,.ytPlayerProgressBarHost,.ytwPlayerFullscreenControlsHost,' +
                '.fullscreen-action-menu,.ytFullscreenVideoRecommendationsHost,.ytmCuedOverlayHost,' +
                '.player-controls-background-container,.player-controls-background,' +
                '.ytmWatchPlayerControlsBackgroundActionItems)' +
                '{display:none!important;pointer-events:none!important}';
              let adopted = false;
              try {
                const sheet = new CSSStyleSheet();
                sheet.replaceSync(css);
                document.adoptedStyleSheets = [...document.adoptedStyleSheets, sheet];
                adopted = true;
              } catch (unavailable) {}
              const style = document.createElement('style');
              style.id = 'luma-wallpaper-ui';
              style.textContent = css;
              const observer = new MutationObserver(install);
              let root = null, parent = null;
              function install() {
                const nextRoot = document.documentElement;
                const nextParent = document.head || nextRoot;
                if (style.isConnected && root === nextRoot && parent === nextParent) return;
                observer.disconnect();
                root = nextRoot; parent = nextParent;
                if (parent) parent.appendChild(style);
                observer.observe(document, {childList:true});
                if (root) observer.observe(root, {childList:true});
                if (parent && parent !== root) observer.observe(parent, {childList:true});
              }
              if (!adopted) install();
            })();
            """;

    static ScriptHandler install(View root) {
        try {
            WebView web = findWebView(root);
            if (web != null && WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                ScriptHandler script = WebViewCompat.addDocumentStartJavaScript(web, SCRIPT,
                        new HashSet<>(Arrays.asList("https://www.youtube.com", "https://www.youtube-nocookie.com")));
                Log.i("TvHome.YouTube", "Wallpaper UI styling registered");
                return script;
            }
        } catch (RuntimeException unavailable) {
            // Cosmetic styling must not prevent playback on an unsupported/broken provider.
            Log.w("TvHome.YouTube", "Wallpaper UI styling could not be registered");
        }
        Log.i("TvHome.YouTube", "Wallpaper UI styling unavailable on this WebView");
        return null;
    }

    private static WebView findWebView(View root) {
        if (root instanceof WebView) return (WebView) root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                WebView result = findWebView(group.getChildAt(i));
                if (result != null) return result;
            }
        }
        return null;
    }
}
