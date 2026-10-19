# Роуз Фит 🌸

Милый фитнес-трекер челленджа для Android — вдохновлён 75 Hard, но в розовой стилистике со стикерами. Полностью на русском языке.

![Kotlin](https://img.shields.io/badge/Kotlin-1.9-FF6FA5) ![minSdk 26](https://img.shields.io/badge/minSdk-26-FFC2D9) ![Release APK](https://img.shields.io/badge/APK-ready-7ED9A8)

## Возможности

- **Главный экран** — большой пульсирующий розовый круг с номером текущего дня, искорками ✨ и статистикой (выполнено дней, последний вес, шаги).
- **Календарь** — сетка на N квадратиков: ✅ выполненные дни, ⚖️ дни взвешивания каждые 5 дней, 🔒 будущие дни, 💖 сегодня.
- **Экран дня**:
  - переключатель «День выполнен»;
  - ввод шагов 👟;
  - приёмы пищи 🍽: фото (камера или галерея), название, калории — сумма ккал считается автоматически;
  - каждые 5 дней — вес ⚖️ и фото прогресса 📸.
- **Онбординг** — выбор длины челленджа (30/60/75/90/100 или своё число) и даты старта.
- Данные хранятся локально в SharedPreferences, фото — во внутреннем хранилище приложения.

## Установка

Скачай `RoseFit.apk` из [Releases](https://github.com/McCerber7/RoseFit/releases) (или из корня репозитория), перекинь на телефон и установи. Потребуется разрешить установку приложений из неизвестных источников.

**Требования:** Android 8.0 (API 26) и выше.

## Сборка из исходников

```bash
# нужны JDK 17, Android SDK (platform-tools, platforms;android-34, build-tools;34.0.0)
# и путь к SDK в local.properties:  sdk.dir=<путь>
gradle assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

Для подписи release-сборки нужен keystore (`keystore/pinkfit.jks`); в репозитории он отсутствует — создай свой через `keytool -genkeypair`.

## Стек

Kotlin · Android View (XML) · Material 3 · Canvas-графика · SharedPreferences · ExifInterface
