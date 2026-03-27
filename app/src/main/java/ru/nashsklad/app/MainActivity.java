package ru.nashsklad.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
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
    private static final int REQ_PERMS = 1;

    private WebView webView;
    private SpeechRecognizer sr;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    // ── JS Bridge ────────────────────────────────────────────
    class JSBridge {

        @JavascriptInterface
        public void startVoice(final String fieldId, final String memKey) {
            uiHandler.post(() -> {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    jsEval("onVoiceError('" + fieldId + "','permission')");
                    return;
                }
                launchSpeech(fieldId, memKey);
            });
        }

        @JavascriptInterface
        public void stopVoice() {
            uiHandler.post(() -> { if (sr != null) sr.stopListening(); });
        }

        @JavascriptInterface
        public void sendEmailWithFile(String base64, String filename,
                                      String subject, String body) {
            try {
                byte[] data = Base64.decode(base64, Base64.DEFAULT);
                File file = new File(getCacheDir(), filename);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data); fos.close();
                Uri uri = FileProvider.getUriForFile(MainActivity.this,
                        getPackageName() + ".provider", file);
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                i.putExtra(Intent.EXTRA_SUBJECT, subject);
                i.putExtra(Intent.EXTRA_TEXT, body);
                i.putExtra(Intent.EXTRA_STREAM, uri);
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(i, "Отправить отчёт"));
            } catch (Exception e) {
                Log.e(TAG, "sendEmailWithFile", e);
                uiHandler.post(() -> Toast.makeText(MainActivity.this,
                        "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }

        @JavascriptInterface
        public void shareXlsx(String base64, String filename) {
            try {
                byte[] data = Base64.decode(base64, Base64.DEFAULT);
                File file = new File(getCacheDir(), filename);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data); fos.close();
                Uri uri = FileProvider.getUriForFile(MainActivity.this,
                        getPackageName() + ".provider", file);
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("*/*");
                i.putExtra(Intent.EXTRA_STREAM, uri);
                i.putExtra(Intent.EXTRA_SUBJECT, "Отчёт Наш Склад");
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(i, "Открыть или отправить"));
            } catch (Exception e) {
                Log.e(TAG, "shareXlsx", e);
                uiHandler.post(() -> Toast.makeText(MainActivity.this,
                        "Ошибка экспорта: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }

        @JavascriptInterface
        public void sendEmail(String subject, String body) {
            try {
                Intent i = new Intent(Intent.ACTION_SENDTO);
                i.setData(Uri.parse("mailto:"));
                i.putExtra(Intent.EXTRA_SUBJECT, subject);
                i.putExtra(Intent.EXTRA_TEXT, body);
                startActivity(Intent.createChooser(i, "Отправить отчёт"));
            } catch (Exception e) {
                uiHandler.post(() -> Toast.makeText(MainActivity.this,
                        "Почтовый клиент не найден", Toast.LENGTH_SHORT).show());
            }
        }
    }

    // ── SpeechRecognizer ─────────────────────────────────────
    private void launchSpeech(final String fieldId, final String memKey) {
        if (sr != null) { sr.destroy(); sr = null; }
        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(Bundle b)  { jsEval("onVoiceReady('" + fieldId + "','')"); }
            public void onBeginningOfSpeech()       {}
            public void onRmsChanged(float v)       {}
            public void onBufferReceived(byte[] b)  {}
            public void onEndOfSpeech()             {}
            public void onPartialResults(Bundle b)  {}
            public void onEvent(int t, Bundle b)    {}

            public void onResults(Bundle results) {
                ArrayList<String> m =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (m != null && !m.isEmpty()) {
                    String text = m.get(0).replace("'", "\\'");
                    jsEval("onVoiceResult('" + fieldId + "','" + text + "')");
                } else {
                    jsEval("onVoiceError('" + fieldId + "','no-speech')");
                }
            }

            public void onError(int error) {
                String msg;
                switch (error) {
                    case SpeechRecognizer.ERROR_NO_MATCH:
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:  msg = "no-speech";  break;
                    case SpeechRecognizer.ERROR_NETWORK:
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: msg = "network";    break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                                                                  msg = "permission"; break;
                    case SpeechRecognizer.ERROR_AUDIO:            msg = "audio";      break;
                    default:                                       msg = "err-" + error;
                }
                jsEval("onVoiceError('" + fieldId + "','" + msg + "')");
            }
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU");
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false);
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true); // офлайн!
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L);
        sr.startListening(intent);
    }

    private void jsEval(final String js) {
        uiHandler.post(() -> { if (webView != null) webView.evaluateJavascript(js, null); });
    }

    // ── onCreate ─────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Запрашиваем камеру + микрофон при первом запуске
        java.util.List<String> need = new java.util.ArrayList<>();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            need.add(Manifest.permission.CAMERA);
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            need.add(Manifest.permission.RECORD_AUDIO);
        if (!need.isEmpty())
            requestPermissions(need.toArray(new String[0]), REQ_PERMS);

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
                        startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(url)));
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
                uiHandler.post(() -> request.grant(request.getResources()));
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
        Log.d(TAG, "loaded");
    }

    @Override
    protected void onDestroy() {
        if (sr != null) { sr.destroy(); sr = null; }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
