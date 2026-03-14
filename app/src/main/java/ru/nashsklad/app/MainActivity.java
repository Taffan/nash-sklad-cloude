package ru.nashsklad.app;

import android.app.Activity;
import android.os.Bundle;

import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebChromeClient;
import android.webkit.PermissionRequest;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;

import android.util.Base64;

import java.io.File;
import java.io.FileOutputStream;

import android.net.Uri;
import android.content.Intent;

import androidx.core.content.FileProvider;

public class MainActivity extends Activity {

    WebView webView;

    class JSBridge {

        Activity activity;

        JSBridge(Activity a){
            activity = a;
        }

        @JavascriptInterface
        public void shareXlsx(String base64,String filename){

            try{

                byte[] data = Base64.decode(base64,Base64.DEFAULT);

                File file = new File(activity.getCacheDir(),filename);
                FileOutputStream fos = new FileOutputStream(file);

                fos.write(data);
                fos.close();

                Uri uri = FileProvider.getUriForFile(
                        activity,
                        activity.getPackageName()+".provider",
                        file
                );

                Intent intent = new Intent(Intent.ACTION_SEND);

                intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                intent.putExtra(Intent.EXTRA_STREAM,uri);

                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                activity.startActivity(Intent.createChooser(intent,"Отправить файл"));

            }catch(Exception e){
                e.printStackTrace();
            }

        }

    }

    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        webView.addJavascriptInterface(new JSBridge(this),"Android");

        WebSettings s = webView.getSettings();

        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient(){

            @Override
            public void onPermissionRequest(PermissionRequest request){
                request.grant(request.getResources());
            }

        });

        webView.loadUrl("file:///android_asset/index.html");

    }

}
