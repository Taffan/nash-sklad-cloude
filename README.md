# Наш Склад — сборка APK через GitHub Actions

## Шаг 1 — Создать репозиторий на GitHub

1. Зайдите на https://github.com → **New repository**
2. Название: `nash-sklad`
3. Видимость: **Private** (или Public — не важно)
4. Нажмите **Create repository**

## Шаг 2 — Загрузить файлы

Загрузите все файлы из ZIP в репозиторий:
- Нажмите **uploading an existing file**
- Перетащите все файлы из ZIP (включая папку `.github/`)
- **Commit changes**

> ⚠️ Важно: папка `.github/workflows/` должна быть в корне репозитория

## Шаг 3 — Запустить сборку

1. Перейдите во вкладку **Actions**
2. Слева выберите **Build APK**
3. Нажмите **Run workflow** → **Run workflow**
4. Подождите ~5 минут

## Шаг 4 — Скачать APK

1. В Actions → нажмите на завершённый workflow
2. Внизу страницы найдите **Artifacts**
3. Скачайте **NashSklad-APK**

---

## Про keystore (подпись APK)

При первой сборке создаётся новый keystore автоматически.

### Чтобы сохранить keystore для будущих сборок:

1. В логах Actions найдите блок:
   ```
   === KEYSTORE BASE64 (сохраните в GitHub Secrets как KEYSTORE_BASE64) ===
   ```
2. Скопируйте длинную строку base64
3. Перейдите в **Settings → Secrets → Actions → New repository secret**
4. Имя: `KEYSTORE_BASE64`, значение: вставьте строку
5. Добавьте ещё секрет `KEYSTORE_PASSWORD` со значением `nashsklad123` (или своим паролем)

### Чтобы убрать строку браузера (TWA верификация):

1. В логах найдите блок:
   ```
   === SHA-256 fingerprint (нужен для assetlinks.json) ===
   SHA256: AA:BB:CC:...
   ```
2. Скопируйте SHA-256 без пробелов
3. Откройте файл `assetlinks.json` и замените значение `sha256_cert_fingerprints`
4. То же самое в `.well-known/assetlinks.json`
5. Загрузите обновлённый ZIP на Netlify
6. Проверьте: https://serene-pavlova-a08285.netlify.app/.well-known/assetlinks.json
7. Пересоберите APK

---

## Netlify адрес
https://serene-pavlova-a08285.netlify.app

## Package ID
`app.netlify.serene_pavlova_a08285.twa`
