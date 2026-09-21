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
        String[] blocked = {
            "doubleclick.net", "googlesyndication.com", "googleadservices.com",
            "adservice.google.com", "googletagmanager.com", "googletagservices.com",
            "amazon-adsystem.com", "adsrvr.org", "adnxs.com", "taboola.com",
            "outbrain.com", "popads.net", "popcash.net", "propellerads.com",
            "exoclick.com", "onclickperformance.com", "trafficjunky.com"
        };
        for (String host : blocked) if (u.contains(host)) return true;
        return false;
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

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
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (isBlockedHost(request.getUrl().toString())) {
                    return new WebResourceResponse(
                        "text/plain", "UTF-8",
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

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
