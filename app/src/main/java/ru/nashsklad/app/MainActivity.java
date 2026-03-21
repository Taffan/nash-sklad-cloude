package ru.nashsklad.app;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebChromeClient;
import android.webkit.PermissionRequest;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;

import android.util.Base64;
import java.io.File;
import java.io.FileOutputStream;

import android.net.Uri;
import android.content.Intent;
import androidx.core.content.FileProvider;

import android.util.Log;  // ← добавили для логов

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "NashSkladWebView";
    private WebView webView;

    // JS Bridge
    class JSBridge {
        MainActivity activity;

        JSBridge(MainActivity a) {
            activity = a;
        }

        @JavascriptInterface
        public void sendEmail(String subject, String body) {
            try {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("message/rfc822");

                intent.putExtra(Intent.EXTRA_SUBJECT, subject);
                intent.putExtra(Intent.EXTRA_TEXT, body);
                activity.startActivity(Intent.createChooser(intent, "Отправить отчёт"));
            } catch (Exception e) {
                Log.e(TAG, "Ошибка отправки почты", e);
                Toast.makeText(activity, "Почтовый клиент не найден", Toast.LENGTH_SHORT).show();
            }
        }

        @JavascriptInterface
        public void shareXlsx(String base64, String filename) {
            try {
                byte[] data = Base64.decode(base64, Base64.DEFAULT);
                File file = new File(activity.getCacheDir(), filename);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.close();

                Uri uri = FileProvider.getUriForFile(
                        activity,
                        activity.getPackageName() + ".provider",
                        file
                );

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                activity.startActivity(Intent.createChooser(intent, "Отправить файл"));
            } catch (Exception e) {
                Log.e(TAG, "Ошибка экспорта XLSX", e);
                Toast.makeText(activity, "Ошибка экспорта: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate started");

        // Runtime permission для камеры
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
            }
        }

        // Инициализация WebView
        webView = new WebView(this);
        setContentView(webView);

        Log.d(TAG, "WebView created");

        // JS Bridge
        webView.addJavascriptInterface(new JSBridge(this), "Android");

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
                Toast.makeText(MainActivity.this, "Нет почтового приложения", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        return false;
    }
});
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                Log.d(TAG, "PermissionRequest: granting " + request.getOrigin());
                runOnUiThread(() -> request.grant(request.getResources()));
            }
        });

        // Загрузка локального HTML
        try {
            webView.loadUrl("file:///android_asset/index.html");
            // webView.setBackgroundColor(0xFF00FF00);  // ← закомментировать после теста
            Log.d(TAG, "loadUrl called: file:///android_asset/index.html");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка загрузки страницы", e);
            Toast.makeText(this, "Ошибка загрузки страницы: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение на камеру обязательно для сканирования", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
