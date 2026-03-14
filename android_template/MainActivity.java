package ru.nashsklad.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements LifecycleOwner {

    private WebView webView;
    private TextRecognizer textRecognizer;
    private ExecutorService cameraExecutor;
    private volatile boolean ocrActive = false;
    private volatile boolean ocrRunning = false;

    public class AndroidBridge {
        @JavascriptInterface
        public void showToast(String msg) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
        }

        @JavascriptInterface
        public void startTextOCR() {
            ocrActive = true;
            runOnUiThread(() -> startCameraForOCR());
        }

        @JavascriptInterface
        public void stopTextOCR() {
            ocrActive = false;
        }

        @JavascriptInterface
        public void saveXlsx(String base64Data, String fileName) {
            try {
                byte[] bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
                File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                if (dir == null) dir = getFilesDir();
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, fileName);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(bytes);
                fos.close();
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "Сохранено: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show());

                Uri uri = FileProvider.getUriForFile(MainActivity.this, "ru.nashsklad.app.provider", file);
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                share.putExtra(Intent.EXTRA_STREAM, uri);
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(share, "Сохранить / отправить"));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "Ошибка Excel: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }
    }

    private void startCameraForOCR() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                provider.unbindAll();

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (!ocrActive || ocrRunning) {
                        imageProxy.close();
                        return;
                    }
                    ocrRunning = true;
                    processImageForText(imageProxy);
                });

                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, analysis);
            } catch (Exception e) {
                sendOcrResult("ERROR:" + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processImageForText(androidx.camera.core.ImageProxy imageProxy) {
        try {
            InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
            textRecognizer.process(image)
                    .addOnSuccessListener(result -> {
                        String raw = result.getText().toUpperCase();
                        StringBuilder sb = new StringBuilder();
                        for (int ci = 0; ci < raw.length(); ci++) {
                            char ch = raw.charAt(ci);
                            if ((ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) sb.append(ch);
                        }
                        String text = sb.toString();
                        Pattern p = Pattern.compile("(INC|REQ)([0-9O]{12})");
                        Matcher m = p.matcher(text);
                        if (m.find()) {
                            String digits = m.group(2).replace("O", "0");
                            String found = m.group(1) + digits;
                            ocrActive = false;
                            sendOcrResult("FOUND:" + found);
                        } else {
                            sendOcrResult("SCANNING");
                        }
                        ocrRunning = false;
                        imageProxy.close();
                    })
                    .addOnFailureListener(e -> {
                        ocrRunning = false;
                        imageProxy.close();
                        sendOcrResult("ERROR:" + e.getMessage());
                    });
        } catch (Exception e) {
            ocrRunning = false;
            imageProxy.close();
            sendOcrResult("ERROR:" + e.getMessage());
        }
    }

    private void sendOcrResult(String result) {
        String js = "onOcrResult('" + result + "')";
        runOnUiThread(() -> webView.evaluateJavascript(js, null));
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1001);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        requestCameraPermission();
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        cameraExecutor = Executors.newSingleThreadExecutor();

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);

        webView.addJavascriptInterface(new AndroidBridge(), "Android");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {});

        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        if (textRecognizer != null) textRecognizer.close();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
