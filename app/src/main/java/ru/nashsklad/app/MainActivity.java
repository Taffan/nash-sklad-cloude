package ru.nashsklad.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "NashSklad";
    private static final int REQ_CAMERA = 1;

    private WebView webView;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    class JSBridge {

        @JavascriptInterface
        public void sendEmailWithFile(String base64, String filename, String subject, String body) {
            try {
                byte[] data = Base64.decode(base64, Base64.DEFAULT);
                File file = new File(getCacheDir(), filename);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.close();

                Uri uri = FileProvider.getUriForFile(
                        MainActivity.this,
                        getPackageName() + ".provider",
                        file
                );

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                intent.putExtra(Intent.EXTRA_SUBJECT, subject);
                intent.putExtra(Intent.EXTRA_TEXT, body);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "Отправить отчёт"));
            } catch (Exception e) {
                Log.e(TAG, "sendEmailWithFile error", e);
                mainHandler.post(() -> Toast.makeText(MainActivity.this,
                        "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }

        @JavascriptInterface
        public void shareXlsx(String base64, String filename) {
            try {
                byte[] data = Base64.decode(base64, Base64.DEFAULT);
                File file = new File(getCacheDir(), filename);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.close();

                Uri uri = FileProvider.getUriForFile(
                        MainActivity.this,
                        getPackageName() + ".provider",
                        file
                );

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.putExtra(Intent.EXTRA_SUBJECT, "Отчёт Наш Склад");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "Открыть или отправить"));
            } catch (Exception e) {
                Log.e(TAG, "shareXlsx error", e);
                mainHandler.post(() -> Toast.makeText(MainActivity.this,
                        "Ошибка экспорта: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }

        @JavascriptInterface
        public void sendEmail(String subject, String body) {
            try {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:"));
                intent.putExtra(Intent.EXTRA_SUBJECT, subject);
                intent.putExtra(Intent.EXTRA_TEXT, body);
                startActivity(Intent.createChooser(intent, "Отправить отчёт"));
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(MainActivity.this,
                        "Почтовый клиент не найден", Toast.LENGTH_SHORT).show());
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        }

        webView = new WebView(this);
        setContentView(webView);

        webView.addJavascriptInterface(new JSBridge(), "Android");

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("mailto:")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_SENDTO);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this,
                                "Нет почтового приложения", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                mainHandler.post(() -> request.grant(request.getResources()));
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
        Log.d(TAG, "WebView loaded");
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == REQ_CAMERA && results.length > 0
                && results[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Разрешите доступ к камере в настройках",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
