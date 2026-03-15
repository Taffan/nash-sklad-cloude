package ru.nashsklad.app;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebChromeClient;
import android.webkit.PermissionRequest;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;

import android.app.Activity;
import android.util.Base64;

import java.io.File;
import java.io.FileOutputStream;

import android.net.Uri;
import android.content.Intent;
import androidx.core.content.FileProvider;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;

    // --- JS Bridge для shareXlsx ---
    class JSBridge {

        Activity activity;

        JSBridge(Activity a) {
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
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- Runtime permission для камеры ---
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 1);
            }
        }

        // --- Инициализация WebView ---
        webView = new WebView(this);
        setContentView(webView);

        // JSBridge для shareXlsx
        webView.addJavascriptInterface(new JSBridge(this), "Android");

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());

        // WebChromeClient для запроса разрешений камеры в WebView
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }
        });

        // Загружаем локальный HTML
        webView.loadUrl("file:///android_asset/index.html");
    }

    // --- Обработчик результата разрешения камеры ---
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение на камеру обязательно", Toast.LENGTH_LONG).show();
            }
        }
    }
}
