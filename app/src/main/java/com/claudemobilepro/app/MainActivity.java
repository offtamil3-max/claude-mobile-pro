package com.claudemobilepro.app;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.speech.*;
import android.webkit.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
  private WebView web;
  private ValueCallback<Uri[]> fileCallback;
  private SpeechRecognizer recognizer;
  private static final int FILE_PICKER = 7001;
  private static final int AUDIO_PERMISSION = 7002;

  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    getWindow().setStatusBarColor(android.graphics.Color.rgb(23,23,22));
    getWindow().setNavigationBarColor(android.graphics.Color.rgb(23,23,22));
    web = new WebView(this);
    web.setBackgroundColor(android.graphics.Color.rgb(23,23,22));
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setAllowFileAccess(true);
    s.setAllowContentAccess(true);
    s.setMediaPlaybackRequiresUserGesture(false);
    s.setBuiltInZoomControls(false);
    s.setDisplayZoomControls(false);

    web.setWebViewClient(new WebViewClient() {
      @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) { return loadAsset(request.getUrl().toString()); }
      @Override public WebResourceResponse shouldInterceptRequest(WebView view, String url) { return loadAsset(url); }
      @Override public boolean shouldOverrideUrlLoading(WebView v, String u) {
        if (u.startsWith("http://") || u.startsWith("https://")) { v.loadUrl(u); return true; }
        return false;
      }
    });

    web.setWebChromeClient(new WebChromeClient() {
      @Override public void onPermissionRequest(final PermissionRequest r) { runOnUiThread(() -> r.grant(r.getResources())); }
      @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams params) {
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        fileCallback = cb;
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        try { startActivityForResult(i, FILE_PICKER); } catch (Exception e) { fileCallback.onReceiveValue(null); fileCallback=null; }
        return true;
      }
    });
    web.addJavascriptInterface(new Bridge(this), "AndroidBridge");
    setContentView(web);
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

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == FILE_PICKER && fileCallback != null) {
      Uri[] results = null;
      if (resultCode == RESULT_OK && data != null) {
        if (data.getClipData() != null) {
          int n = data.getClipData().getItemCount(); results = new Uri[n];
          for (int x=0;x<n;x++) results[x]=data.getClipData().getItemAt(x).getUri();
        } else if (data.getData() != null) results = new Uri[]{data.getData()};
      }
      fileCallback.onReceiveValue(results); fileCallback=null;
    }
  }

  private void startVoice() {
    if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION); return;
    }
    if (!SpeechRecognizer.isRecognitionAvailable(this)) { Toast.makeText(this,"Voice recognition unavailable",Toast.LENGTH_SHORT).show(); return; }
    if (recognizer != null) recognizer.destroy();
    recognizer = SpeechRecognizer.createSpeechRecognizer(this);
    recognizer.setRecognitionListener(new RecognitionListener() {
      public void onReadyForSpeech(Bundle p) {}
      public void onBeginningOfSpeech() {}
      public void onRmsChanged(float r) {}
      public void onBufferReceived(byte[] b) {}
      public void onEndOfSpeech() {}
      public void onError(int e) { web.evaluateJavascript("window.setVoiceText && window.setVoiceText('')",null); }
      public void onResults(Bundle r) { ArrayList<String> a=r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION); if(a!=null&&!a.isEmpty()) web.evaluateJavascript("window.setVoiceText && window.setVoiceText("+org.json.JSONObject.quote(a.get(0))+")",null); }
      public void onPartialResults(Bundle r) {}
      public void onEvent(int t, Bundle p) {}
    });
    Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ta-IN"); i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false); recognizer.startListening(i);
  }

  private void stopVoice() { if(recognizer!=null){recognizer.stopListening();recognizer.destroy();recognizer=null;} }
  @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==AUDIO_PERMISSION&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)startVoice();}
  @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); setIntent(i); handleIntent(i); }
  private void handleIntent(Intent i) { Uri u=i.getData(); if(u!=null&&"claudemobilepro".equals(u.getScheme())){String code=u.getQueryParameter("code");if(code!=null)web.evaluateJavascript("window.handleGithubCallback && window.handleGithubCallback("+org.json.JSONObject.quote(code)+")",null);} }
  @Override public void onBackPressed() { if(web.canGoBack()) web.goBack(); else super.onBackPressed(); }
  @Override protected void onDestroy(){stopVoice();super.onDestroy();}

  public static class Bridge {
    final MainActivity a; Bridge(MainActivity a){this.a=a;}
    @JavascriptInterface public void toast(String m){Toast.makeText(a,m,Toast.LENGTH_SHORT).show();}
    @JavascriptInterface public void share(String t){Intent s=new Intent(Intent.ACTION_SEND);s.setType("text/plain");s.putExtra(Intent.EXTRA_TEXT,t);a.startActivity(Intent.createChooser(s,"Share"));}
    @JavascriptInterface public void startVoice(){a.runOnUiThread(a::startVoice);}
    @JavascriptInterface public void stopVoice(){a.runOnUiThread(a::stopVoice);}
  }
}
