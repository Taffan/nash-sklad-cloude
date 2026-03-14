import os, shutil

PKG      = "ru.nashsklad.app"
NAME     = "Наш Склад"
KS_PASS  = os.environ.get("KS_PASS", "nashsklad123")
PKG_PATH = PKG.replace('.', '/')

# Создаем папки
DIRS = [
    f"app/app/src/main/java/{PKG_PATH}/",
    "app/app/src/main/assets/",
    "app/app/src/main/res/values/",
] + [f"app/app/src/main/res/mipmap-{x}/" for x in ["mdpi","hdpi","xhdpi","xxhdpi","xxxhdpi"]]

for d in DIRS:
    os.makedirs(d, exist_ok=True)

# Копируем index.html
shutil.copy("index.html", "app/app/src/main/assets/index.html")

# settings.gradle
open("app/settings.gradle","w").write("include ':app'\n")

# build.gradle в корне
open("app/build.gradle","w").write(
    "buildscript {\n"
    "    repositories { google(); mavenCentral() }\n"
    "    dependencies { classpath 'com.android.tools.build:gradle:8.2.2' }\n"
    "}\n"
    "allprojects {\n"
    "    repositories { google(); mavenCentral() }\n"
    "}\n"
)

# gradle.properties
open("app/gradle.properties","w").write(
    "android.useAndroidX=true\n"
    "android.enableJetifier=true\n"
)

# app/build.gradle
open("app/app/build.gradle","w").write(f"""plugins {{ id 'com.android.application' }}
android {{
    namespace '{PKG}'
    compileSdk 34
    defaultConfig {{
        applicationId '{PKG}'
        minSdk 21
        targetSdk 34
        versionCode 1
        versionName '1.0'
    }}
    signingConfigs {{
        release {{
            storeFile file('../../keystore.jks')
            storePassword '{KS_PASS}'
            keyAlias 'upload'
            keyPassword '{KS_PASS}'
        }}
    }}
    buildTypes {{
        release {{
            signingConfig signingConfigs.release
            minifyEnabled false
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
}}
""")

# AndroidManifest.xml
os.makedirs("app/app/src/main/", exist_ok=True)
open("app/app/src/main/AndroidManifest.xml","w").write(f"""<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="{PKG}">

    <uses-permission android:name="android.permission.CAMERA"/>
    <uses-feature android:name="android.hardware.camera" android:required="false"/>

    <application
        android:label="{NAME}"
        android:icon="@mipmap/ic_launcher"
        android:theme="@style/AppTheme"
        android:allowBackup="true">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|keyboardHidden">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
    </application>

</manifest>
""")

# file_paths.xml
os.makedirs("app/app/src/main/res/xml/", exist_ok=True)
open("app/app/src/main/res/xml/file_paths.xml","w").write("""<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-files-path name="documents" path="Documents/"/>
    <files-path name="files" path="."/>
</paths>
""")

# MainActivity.java
os.makedirs(f"app/app/src/main/java/{PKG_PATH}/", exist_ok=True)
open(f"app/app/src/main/java/{PKG_PATH}/MainActivity.java","w").write(f"""package {PKG};

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;

public class MainActivity extends Activity {{
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {{
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        webView.loadUrl("file:///android_asset/index.html");
    }}
}}
""")

print("Android project created successfully.")
