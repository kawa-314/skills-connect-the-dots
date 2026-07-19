package io.github.kawa314.formationmaker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final int REQ_PICK_FILE = 1;
    private static final int REQ_SAVE_FILE = 2;

    private WebView web;
    private ValueCallback<Uri[]> fileChooserCallback;
    private byte[] pendingSaveData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(null);
                fileChooserCallback = callback;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(i, "ファイルを選択"), REQ_PICK_FILE);
                } catch (Exception e) {
                    fileChooserCallback.onReceiveValue(null);
                    fileChooserCallback = null;
                }
                return true;
            }
        });

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);
    }

    private class Bridge {
        @JavascriptInterface
        public void saveFile(String name, String mime, String base64) {
            pendingSaveData = Base64.decode(base64, Base64.DEFAULT);
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType(mime);
            i.putExtra(Intent.EXTRA_TITLE, name);
            runOnUiThread(() -> {
                try {
                    startActivityForResult(i, REQ_SAVE_FILE);
                } catch (Exception ignored) {
                    pendingSaveData = null;
                }
            });
        }

        @JavascriptInterface
        public void shareText(String subject, String text) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, subject);
            i.putExtra(Intent.EXTRA_TEXT, text);
            runOnUiThread(() -> startActivity(Intent.createChooser(i, "共有")));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_FILE) {
            if (fileChooserCallback != null) {
                Uri[] result = (resultCode == RESULT_OK && data != null && data.getData() != null)
                        ? new Uri[]{data.getData()} : null;
                fileChooserCallback.onReceiveValue(result);
                fileChooserCallback = null;
            }
        } else if (requestCode == REQ_SAVE_FILE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null
                    && pendingSaveData != null) {
                try (OutputStream os = getContentResolver().openOutputStream(data.getData())) {
                    os.write(pendingSaveData);
                } catch (Exception ignored) {
                }
            }
            pendingSaveData = null;
        }
    }

    @Override
    public void onBackPressed() {
        // モーダルが開いていればJS側で閉じる。閉じるものが無ければアプリを終了。
        web.evaluateJavascript("window.appBack ? window.appBack() : 'exit'", value -> {
            if (value == null || "\"exit\"".equals(value) || "null".equals(value)) {
                finish();
            }
        });
    }
}
