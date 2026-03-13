# Наш Склад — сборка APK через GitHub

Никакой Netlify не нужен. Всё через GitHub Pages + GitHub Actions.

## Шаг 1 — Создать репозиторий
1. github.com → New repository → название `nash-sklad` → Create

## Шаг 2 — Загрузить файлы
**Через GitHub Desktop** (рекомендую):
1. Скачайте desktop.github.com
2. File → Clone repository → nash-sklad
3. Скопируйте все файлы из ZIP в папку репозитория
4. Commit to main → Push origin

**Через браузер:**
- Add file → Upload files → перетащите все файлы → Commit
- Если .github/ не загрузилась: Add file → Create new file →
  путь `.github/workflows/build-apk.yml` → вставьте содержимое файла

## Шаг 3 — Включить GitHub Pages
Settings → Pages → Source: gh-pages → / (root) → Save
(gh-pages появится после первой сборки)

## Шаг 4 — Запустить сборку
Actions → Build APK → Run workflow → ждите ~7 минут

## Шаг 5 — Скачать APK
Нажмите на завершённый workflow → Artifacts → NashSklad-APK

## Сохранить keystore (важно!)
В логах первой сборки найдите блок "СОХРАНИТЕ В SECRETS КАК KEYSTORE_BASE64"
→ Settings → Secrets → New secret → KEYSTORE_BASE64
