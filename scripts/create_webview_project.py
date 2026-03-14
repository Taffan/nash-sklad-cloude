import os, shutil

PKG = "ru.nashsklad.app"
NAME = "Наш Склад"
PKG_PATH = PKG.replace(".", "/")

dirs = [
    "app/src/main/java/" + PKG_PATH,
    "app/src/main/assets",
    "app/src/main/res/values",
    "app/src/main/res/xml",
] + ["app/src/main/res/mipmap-" + x for x in ["mdpi","hdpi","xhdpi","xxhdpi","xxxhdpi"]]

for d in dirs:
    os.makedirs(d, exist_ok=True)

shutil.copy("index.html", "app/src/main/assets/index.html")

open("settings.gradle","w").write("include ':app'\n")

open("build.gradle","w").write("""
buildscript {
 repositories { google(); mavenCentral() }
 dependencies { classpath 'com.android.tools.build:gradle:8.2.2' }
}
allprojects { repositories { google(); mavenCentral() } }
""")

open("gradle.properties","w").write("""
android.useAndroidX=true
android.enableJetifier=true
org.gradle.jvmargs=-Xmx2048m
kotlin.stdlib.default.dependency=false
""")

open("app/build.gradle","w").write("""
plugins { id 'com.android.application' }

android {
 namespace 'ru.nashsklad.app'
 compileSdk 34

 defaultConfig {
  applicationId 'ru.nashsklad.app'
  minSdk 21
  targetSdk 34
  versionCode 1
  versionName "1.0"
 }

 buildTypes {
  release {
   minifyEnabled false
  }
 }

 compileOptions {
  sourceCompatibility JavaVersion.VERSION_11
  targetCompatibility JavaVersion.VERSION_11
 }
}

dependencies {
 implementation 'androidx.appcompat:appcompat:1.6.1'
 implementation 'androidx.core:core:1.12.0'
 implementation 'androidx.webkit:webkit:1.8.0'
}
""")

manifest = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="ru.nashsklad.app">

    <uses-permission android:name="android.permission.CAMERA"/>
    <uses-permission android:name="android.permission.INTERNET"/>

    <application
        android:label="Наш Склад"
        android:icon="@mipmap/ic_launcher"
        android:theme="@style/AppTheme"
        android:usesCleartextTraffic="true">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize">

            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>

        </activity>

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="ru.nashsklad.app.provider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths"/>
        </provider>

    </application>

</manifest>
"""


open("app/src/main/AndroidManifest.xml","w").write(manifest)

open("app/src/main/res/xml/file_paths.xml","w").write("""
<paths>
 <cache-path name="cache" path="."/>
</paths>
""")

open("app/src/main/res/values/styles.xml","w").write("""
<resources>
<style name="AppTheme" parent="Theme.AppCompat.Light.NoActionBar"/>
</resources>
""")

open("app/src/main/res/values/strings.xml","w").write("""
<resources>
<string name="app_name">Наш Склад</string>
</resources>
""")

main = """
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
  public void shareXlsx(String base64, String filename){

   try{

    byte[] data = Base64.decode(base64, Base64.DEFAULT);

    File file = new File(activity.getCacheDir(), filename);
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
    intent.putExtra(Intent.EXTRA_STREAM, uri);
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

    activity.startActivity(Intent.createChooser(intent,"Отправить файл"));

   }catch(Exception e){
    e.printStackTrace();
   }

  }

 }

 protected void onCreate(Bundle savedInstanceState) {
  super.onCreate(savedInstanceState);

  webView = new WebView(this);
  setContentView(webView);

  webView.addJavascriptInterface(new JSBridge(this), "Android");

  WebSettings s = webView.getSettings();
  s.setJavaScriptEnabled(true);
  s.setDomStorageEnabled(true);
  s.setAllowFileAccess(true);
  s.setAllowContentAccess(true);
  s.setMediaPlaybackRequiresUserGesture(false);

  webView.setWebViewClient(new WebViewClient());

  webView.setWebChromeClient(new WebChromeClient() {
   @Override
   public void onPermissionRequest(final PermissionRequest request) {
    request.grant(request.getResources());
   }
  });

  webView.loadUrl("file:///android_asset/index.html");
 }

}
"""

open("app/src/main/java/" + PKG_PATH + "/MainActivity.java","w").write(main)

print("Android project generated")
