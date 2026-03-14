import os, shutil

PKG      = "ru.nashsklad.app"
NAME     = "Наш Склад"
KS_PASS  = os.environ.get("KS_PASS", "nashsklad123")
PKG_PATH = PKG.replace('.', '/')

# Создаём нужные папки
dirs = [
    f"app/app/src/main/java/{PKG_PATH}/",
    "app/app/src/main/assets/",
    "app/app/src/main/res/values/",
    "app/app/src/main/res/xml/",
] + [f"app/app/src/main/res/mipmap-{x}/" for x in ["mdpi","hdpi","xhdpi","xxhdpi","xxxhdpi"]]

for d in dirs:
    os.makedirs(d, exist_ok=True)

# Копируем index.html в assets
shutil.copy("index.html", "app/app/src/main/assets/index.html")

# settings.gradle
with open("app/settings.gradle","w") as f:
    f.write("include ':app'\n")

# build.gradle (root)
with open("app/build.gradle","w") as f:
    f.write(
        "buildscript {\n"
        "    repositories { google(); mavenCentral() }\n"
        "    dependencies { classpath 'com.android.tools.build:gradle:8.2.2' }\n"
        "}\n"
        "allprojects {\n"
        "    repositories { google(); mavenCentral() }\n"
        "    configurations.all {\n"
        "        resolutionStrategy {\n"
        "            force 'org.jetbrains.kotlin:kotlin-stdlib:1.8.22'\n"
        "            force 'org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.22'\n"
        "            force 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.22'\n"
        "        }\n"
        "    }\n"
        "}\n"
    )

# gradle.properties
with open("app/gradle.properties","w") as f:
    f.write(
        "android.useAndroidX=true\n"
        "android.enableJetifier=true\n"
        "org.gradle.jvmargs=-Xmx2048m\n"
        "android.suppressKotlinVersionCompatibilityCheck=true\n"
    )

# app/build.gradle
app_build = f"""
plugins {{ id 'com.android.application' }}

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
"""

with open("app/app/build.gradle","w") as f:
    f.write(app_build)

# AndroidManifest.xml
manifest = f"""<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

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

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="{PKG}.provider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths"/>
        </provider>

    </application>
</manifest>
"""

with open("app/app/src/main/AndroidManifest.xml","w") as f:
    f.write(manifest)

# file_paths.xml
file_paths = """<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-files-path name="documents" path="Documents/"/>
    <files-path name="files" path="."/>
</paths>
"""
with open("app/app/src/main/res/xml/file_paths.xml","w") as f:
    f.write(file_paths)

# strings.xml
strings = f"""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">{NAME}</string>
</resources>
"""
with open("app/app/src/main/res/values/strings.xml","w") as f:
    f.write(strings)

# styles.xml
styles = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="AppTheme" parent="Theme.AppCompat.Light.NoActionBar">
        <item name="colorPrimary">#CC1F1F</item>
        <item name="colorPrimaryDark">#AA1010</item>
        <item name="colorAccent">#CC1F1F</item>
    </style>
</resources>
"""
with open("app/app/src/main/res/values/styles.xml","w") as f:
    f.write(styles)

# MainActivity.java
main_activity = f"""package {PKG};

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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
        webView.loadUrl("file:///android_asset/index.html");
    }}
}}
"""
java_path = f"app/app/src/main/java/{PKG_PATH}/MainActivity.java"
with open(java_path,"w") as f:
    f.write(main_activity)

print("✅ WebView Android project created successfully!")
