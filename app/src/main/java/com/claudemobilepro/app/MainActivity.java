package com.claudemobilepro.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.webkit.*;
import android.widget.*;
import java.io.*;

public class MainActivity extends Activity {
  private WebView web;

  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    getWindow().setStatusBarColor(android.graphics.Color.rgb(15,16,18));
    web = new WebView(this);
    web.setBackgroundColor(android.graphics.Color.rgb(15,16,18));
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setAllowFileAccess(true);
    s.setAllowContentAccess(true);
    s.setMediaPlaybackRequiresUserGesture(false);

    web.setWebViewClient(new WebViewClient() {
      @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return loadAsset(request.getUrl().toString());
      }
      @Override public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        return loadAsset(url);
      }
      @Override public boolean shouldOverrideUrlLoading(WebView v, String u) {
        if (u.startsWith("http://") || u.startsWith("https://")) { v.loadUrl(u); return true; }
        return false;
      }
    });

    web.setWebChromeClient(new WebChromeClient() {
      @Override public void onPermissionRequest(final PermissionRequest r) { r.grant(r.getResources()); }
    });
    web.addJavascriptInterface(new Bridge(this), "AndroidBridge");
    setContentView(web);
    // Puter.js rejects file://. Serve bundled assets from an HTTPS appassets origin.
    web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    handleIntent(getIntent());
  }

  private WebResourceResponse loadAsset(String url) {
    final String prefix = "https://appassets.androidplatform.net/assets/";
    if (!url.startsWith(prefix)) return null;
    String path = url.substring(prefix.length());
    try {
      InputStream in = getAssets().open(path);
      String mime = "text/plain";
      if (path.endsWith(".html")) mime = "text/html";
      else if (path.endsWith(".js")) mime = "application/javascript";
      else if (path.endsWith(".css")) mime = "text/css";
      else if (path.endsWith(".json")) mime = "application/json";
      else if (path.endsWith(".png")) mime = "image/png";
      else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) mime = "image/jpeg";
      else if (path.endsWith(".svg")) mime = "image/svg+xml";
      return new WebResourceResponse(mime, "UTF-8", in);
    } catch (IOException e) { return null; }
  }

  @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); setIntent(i); handleIntent(i); }
  private void handleIntent(Intent i) {
    Uri u = i.getData();
    if (u != null && "claudemobilepro".equals(u.getScheme())) {
      String code = u.getQueryParameter("code");
      if (code != null) web.evaluateJavascript("window.handleGithubCallback && window.handleGithubCallback(" + org.json.JSONObject.quote(code) + ")", null);
    }
  }
  @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }

  public static class Bridge {
    Context c; Bridge(Context c) { this.c = c; }
    @JavascriptInterface public void toast(String m) { Toast.makeText(c, m, Toast.LENGTH_SHORT).show(); }
    @JavascriptInterface public void share(String t) {
      Intent s = new Intent(Intent.ACTION_SEND); s.setType("text/plain"); s.putExtra(Intent.EXTRA_TEXT, t);
      c.startActivity(Intent.createChooser(s, "Share"));
    }
  }
}
