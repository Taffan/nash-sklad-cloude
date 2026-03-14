package ru.nashsklad.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private TextRecognizer textRecognizer;
    private ExecutorService cameraExecutor;
    private boolean ocrActive = false;
    private boolean ocrRunning = false;
    private static final int REQ_CAMERA = 101;

    public class AndroidBridge {
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
        public void showToast(String msg) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        }

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        cameraExecutor = Executors.newSingleThreadExecutor();

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.addJavascriptInterface(new AndroidBridge(), "Android");
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void startCameraForOCR() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (!ocrActive || ocrRunning) {
                        imageProxy.close();
                        return;
                    }
                    ocrRunning = true;
                    processImage(imageProxy);
                });

                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, analysis);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processImage(ImageProxy imageProxy) {
        try {
            InputImage image = InputImage.fromMediaImage(
                    imageProxy.getImage(),
                    imageProxy.getImageInfo().getRotationDegrees()
            );
            textRecognizer.process(image)
                    .addOnSuccessListener(result -> {
                        handleTextResult(result);
                        ocrRunning = false;
                        imageProxy.close();
                    })
                    .addOnFailureListener(e -> {
                        ocrRunning = false;
                        imageProxy.close();
                    });
        } catch (Exception e) {
            ocrRunning = false;
            imageProxy.close();
        }
    }

    private void handleTextResult(Text result) {
        String raw = result.getText().toUpperCase();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if ((ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) sb.append(ch);
        }
        String text = sb.toString();

        Pattern p = Pattern.compile("(INC|REQ)([0-9O]{12})");
        Matcher m = p.matcher(text);
        if (m.find()) {
            ocrActive = false;
            String digits = m.group(2).replace("O", "0");
            String found = m.group(1) + digits;
            runOnUiThread(() -> webView.evaluateJavascript("onOcrResult('" + found + "')", null));
        } else {
            runOnUiThread(() -> webView.evaluateJavascript("onOcrResult('SCANNING')", null));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        if (textRecognizer != null) textRecognizer.close();
    }
}
