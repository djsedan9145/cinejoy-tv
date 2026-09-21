package pk.cinejoy.tv;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.io.ByteArrayInputStream;
import java.util.Locale;

public class MainActivity extends Activity {
    private WebView web;
    private TextView errorView;

    private boolean isCinejoyHost(String url) {
        if (url == null) return false;
        try {
            String host = Uri.parse(url).getHost();
            return host != null && (host.equalsIgnoreCase("cinejoy.pk") || host.equalsIgnoreCase("www.cinejoy.pk"));
        } catch (Exception e) { return false; }
    }

    private boolean isBlockedHost(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(Locale.US);
        String[] blocked = {"doubleclick.net","googlesyndication.com","googleadservices.com","adservice.google.com",
            "googletagmanager.com","googletagservices.com","amazon-adsystem.com","adsrvr.org","adnxs.com",
            "taboola.com","outbrain.com","popads.net","popcash.net","propellerads.com","exoclick.com",
            "onclickperformance.com","trafficjunky.com","pypo.com","pyppo.com","adf.ly","adfly","ouo.io",
            "ouo.press","shrinkme.io","shrinkearn.com"};
        for (String h : blocked) if (u.contains(h)) return true;
        return false;
    }

    private boolean isBadScheme(String url) {
        if (url == null) return true;
        String u = url.trim().toLowerCase(Locale.US);
        return u.startsWith("intent:") || u.startsWith("market:") || u.startsWith("javascript:") ||
               u.startsWith("tel:") || u.startsWith("mailto:") || u.startsWith("whatsapp:") ||
               u.startsWith("tg:") || u.startsWith("viber:");
    }

    private boolean blockMainNavigation(String url) {
        return isBadScheme(url) || isBlockedHost(url) || !isCinejoyHost(url);
    }

    private void showError(String msg) {
        errorView.setText("Cinejoy could not load.\n\n" + msg + "\n\nPress OK to retry.");
        errorView.setVisibility(View.VISIBLE);
        errorView.requestFocus();
    }

    private void retry() {
        errorView.setVisibility(View.GONE);
        web.clearCache(false);
        web.loadUrl("https://cinejoy.pk/");
        web.requestFocus();
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));

        errorView = new TextView(this);
        errorView.setTextColor(Color.WHITE);
        errorView.setTextSize(20);
        errorView.setGravity(17);
        errorView.setFocusable(true);
        errorView.setFocusableInTouchMode(true);
        errorView.setOnClickListener(v -> retry());
        errorView.setOnKeyListener((v,key,event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_UP &&
                (key == android.view.KeyEvent.KEYCODE_DPAD_CENTER || key == android.view.KeyEvent.KEYCODE_ENTER)) {
                retry(); return true;
            }
            return false;
        });
        errorView.setVisibility(View.GONE);
        root.addView(errorView, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        String ua = s.getUserAgentString();
        s.setUserAgentString(ua.replace("; wv", ""));

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);

        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String url = r.getUrl().toString();
                if (!r.isForMainFrame()) return false;
                return blockMainNavigation(url);
            }

            @Override public boolean shouldOverrideUrlLoading(WebView v, String url) {
                return blockMainNavigation(url);
            }

            @Override public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) showError(e.getDescription() + "\nError code: " + e.getErrorCode());
                super.onReceivedError(v,r,e);
            }

            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                String url = r.getUrl().toString();
                if (isBlockedHost(url) || isBadScheme(url))
                    return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));
                return super.shouldInterceptRequest(v,r);
            }
        });
        web.setWebChromeClient(new WebChromeClient());
        web.loadUrl("https://cinejoy.pk/");
        web.requestFocus(View.FOCUS_DOWN);
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (web != null) {
            web.stopLoading();
            web.loadUrl("about:blank");
            web.clearHistory();
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
