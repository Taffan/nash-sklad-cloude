import os, shutil

PKG = "ru.nashsklad.app"
NAME = "Наш Склад"
PKG_PATH = PKG.replace(".", "/")
KS_PASS = os.environ.get("KS_PASS", "nashsklad123")

dirs = [
    f"app/src/main/java/{PKG_PATH}",
    "app/src/main/assets",
    "app/src/main/res/values",
] + [f"app/src/main/res/mipmap-{x}" for x in ["mdpi","hdpi","xhdpi","xxhdpi","xxxhdpi"]]

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
""")

open("app/build.gradle","w").write(f"""
plugins {{ id 'com.android.application' }}

android {{
 namespace '{PKG}'
 compileSdk 34

 defaultConfig {{
  applicationId '{PKG}'
  minSdk 21
  targetSdk 34
  versionCode 1
  versionName "1.0"
 }}

 signingConfigs {{
  release {{
   storeFile file('../keystore.jks')
   storePassword '{KS_PASS}'
   keyAlias 'upload'
   keyPassword '{KS_PASS}'
  }}
 }}

 buildTypes {{
  release {{
   signingConfig signingConfigs.release
   minifyEnabled true
   shrinkResources true
  }}
 }}

 compileOptions {{
  sourceCompatibility JavaVersion.VERSION_11
  targetCompatibility JavaVersion.VERSION_11
 }}
}}

dependencies {{

 implementation 'androidx.appcompat:appcompat:1.6.1'
 implementation 'androidx.core:core:1.12.0'

 configurations.all {{
     exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk7'
     exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk8'
 }}

}}
""")

manifest = f"""<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

<uses-permission android:name="android.permission.CAMERA"/>
<uses-permission android:name="android.permission.INTERNET"/>

<application
 android:label="{NAME}"
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

</application>
</manifest>
"""

open("app/src/main/AndroidManifest.xml","w").write(manifest)

open("app/src/main/res/values/strings.xml","w").write(f"""
<resources>
<style name="AppTheme" parent="Theme.AppCompat.Light.NoActionBar"/>
</resources>
""")

main = f"""
package {PKG};

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {{

 protected void onCreate(Bundle savedInstanceState) {{
  super.onCreate(savedInstanceState);

  WebView webView = new WebView(this);
  setContentView(webView);

  WebSettings s = webView.getSettings();
  s.setJavaScriptEnabled(true);
  s.setDomStorageEnabled(true);
  s.setAllowFileAccess(true);

  webView.setWebViewClient(new WebViewClient());
  webView.loadUrl("file:///android_asset/index.html");
 }}

}}
"""

open(f"app/src/main/java/{PKG_PATH}/MainActivity.java","w").write(main)

print("Android project generated")
