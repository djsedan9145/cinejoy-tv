package pk.cinejoy.tv;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayInputStream;
import java.util.Locale;

public class MainActivity extends Activity {
    private WebView web;

    private boolean isBlockedHost(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(Locale.US);

        // Advertising, pop-up and redirect networks.
        String[] blocked = {
            "doubleclick.net", "googlesyndication.com", "googleadservices.com",
            "adservice.google.com", "googletagmanager.com", "googletagservices.com",
            "amazon-adsystem.com", "adsrvr.org", "adnxs.com", "taboola.com",
            "outbrain.com", "popads.net", "popcash.net", "propellerads.com",
            "exoclick.com", "onclickperformance.com", "trafficjunky.com",
            "pypo.com", "pyppo.com"
        };

        for (String host : blocked) {
            if (u.contains(host)) return true;
        }
        return false;
    }

    private boolean shouldBlockNavigation(String url) {
        if (url == null) return true;
        String u = url.trim().toLowerCase(Locale.US);

        // Prevent Android WebView from trying to resolve intent://, market://,
        // custom ad schemes and other external-app redirects.
        if (u.startsWith("intent:") ||
            u.startsWith("market:") ||
            u.startsWith("javascript:") ||
            u.startsWith("tel:") ||
            u.startsWith("mailto:") ||
            u.startsWith("whatsapp:")) {
            return true;
        }

        // Block known advertising/redirect hosts before WebView loads them.
        return isBlockedHost(u);
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                if (shouldBlockNavigation(url)) {
                    return true;
                }

                // Keep all normal web navigation inside Cinejoy.
                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (shouldBlockNavigation(url)) {
                    return true;
                }
                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(
                    WebView view, WebResourceRequest request) {

                String url = request.getUrl().toString();

                if (isBlockedHost(url)) {
                    return new WebResourceResponse(
                        "text/plain",
                        "UTF-8",
                        new ByteArrayInputStream(new byte[0])
                    );
                }

                return super.shouldInterceptRequest(view, request);
            }
        });

        web.setWebChromeClient(new WebChromeClient());
        web.loadUrl("https://cinejoy.pk/");
        web.requestFocus(View.FOCUS_DOWN);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
