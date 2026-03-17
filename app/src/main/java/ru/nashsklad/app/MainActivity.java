package ru.nashsklad.app;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebChromeClient;
import android.webkit.PermissionRequest;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;

import androidx.appcompat.app.AppCompatActivity;  // ← изменили здесь
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;

import android.util.Base64;
import java.io.File;
import java.io.FileOutputStream;

import android.net.Uri;
import android.content.Intent;
import androidx.core.content.FileProvider;

public class MainActivity extends AppCompatActivity {  // ← AppCompatActivity

    private WebView webView;

    // --- JS Bridge для shareXlsx (и потенциально других методов) ---
    class JSBridge {
        MainActivity activity;  // ← изменили тип на MainActivity

        JSBridge(MainActivity a) {
            activity = a;
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
                e.printStackTrace();
                Toast.makeText(activity, "Ошибка экспорта: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        // JS Bridge
        webView.addJavascriptInterface(new JSBridge(this), "Android");

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);          // критично для Tesseract/WASM + ZXing
        s.setAllowUniversalAccessFromFileURLs(true);     // критично для file:// + локальные ресурсы
        s.setMediaPlaybackRequiresUserGesture(false);    // камера/видео без жеста

        webView.setWebViewClient(new WebViewClient());

        // WebChromeClient для getUserMedia (камера)
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }
        });

        // Загрузка локального HTML
        try {
            webView.loadUrl("file:///android_asset/index.html");
            webView.setBackgroundColor(0xFF00FF00);  // ярко-зелёный фон
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка загрузки страницы", Toast.LENGTH_LONG).show();
        }
    }

    // Обработчик результата разрешения
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение на камеру обязательно для сканирования", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Обработка кнопки "Назад" — возвращает в историю WebView
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
