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
] + [f"app/app/src/main/res/mipmap-{x}/" for x in ["mdpi","hdpi","xhdpi","xxhdpi","xxxhdpi"]]

for d in dirs:
    os.makedirs(d, exist_ok=True)

# Копируем index.html
shutil.copy("index.html", "app/app/src/main/assets/index.html")

# build.gradle и settings.gradle
with open("app/settings.gradle","w") as f:
    f.write("include ':app'\n")

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

with open("app/gradle.properties","w") as f:
    f.write(
        "android.useAndroidX=true\n"
        "android.enableJetifier=true\n"
        "org.gradle.jvmargs=-Xmx2048m\n"
        "android.suppressKotlinVersionCompatibilityCheck=true\n"
    )

# Здесь можно добавить MainActivity.java, манифест, styles.xml, strings.xml
# и т.д., как мы делали в предыдущих вариантах

print("WebView project structure created.")
