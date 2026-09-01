<div align="center">

# 🎬 AnilibrixPlus

### *Современный, быстрый и функциональный Android-клиент для просмотра аниме*

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-13%2B%20(API%2033%2B)-3DDC84.svg?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/Media3-ExoPlayer-E53935.svg?style=for-the-badge&logo=google&logoColor=white)](https://developer.android.com/media/media3)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](LICENSE)

<p align="center">
  <a href="#-ключевые-возможности">Возможности</a> •
  <a href="#-мультиисточники-и-озвучки">Озвучки и стриминг</a> •
  <a href="#-встроенный-torrent-сервис">Торрент-движок</a> •
  <a href="#-архитектура-и-стек">Технологии</a> •
  <a href="#-установка-и-сборка">Сборка</a>
</p>

</div>

---

## 🌟 Ключевые возможности

### 🎨 Интерфейс и дизайн нового поколения
* **Material Design 3 (Material You)** — динамическая палитра, адаптирующаяся под тему и обои устройства, плавная анимация переходов и поддержка тёмной/светлой темы.
* **Кастомные микроанимации** — карусели релизов, масштабирование карточек при нажатии, тактильная отдача (Haptics) и скелетон-лоадеры.
* **Каталог и фильтрация** — гибкий поиск по жанрам, сезонам, годам, типам релизов и сортировка по популярности и рейтингам.
* **Расписание онгоингов** — интерактивное расписание выхода новых серий по дням недели.

---

### 🎙 Мультиисточники и выбор озвучки
* **AniLibria** — официальные высококачественные релизы с гибким выбором качества (480p, 720p, 1080p).
* **Kodik API** — интеграция с огромной базой сторонних студий озвучки (Studio Band, Dream Cast, AniDUB, Jam Club и др.) и русских субтитров.
* **Consumet API (Gogoanime)** — поддержка зарубежных стримов с английским дубляжом (*EN Dub*) и субтитрами (*EN Sub*).
* **Умный выбор предпочтительной озвучки**:
  * Глобальная настройка любимой студии в профиле.
  * Индивидуальный выбор и сохранение озвучки для каждого отдельного тайтла.
  * Мгновенное обновление списка серий и онлайн-потоков при смене переводчика.

---

### ⚡ Встроенный Torrent-сервис и умный парсер
* **Встроенный загрузчик торрентов** — больше не требуются сторонние приложения (µTorrent/Flud). Вся загрузка происходит прямо внутри приложения в фоновом режиме (`Foreground Service`) с уведомлением в шторке.
* **Умный парсер релизов (`TorrentNameParser`)**:
  * Автоматически форматирует «сырые» заголовки с **Nyaa.si** и **AniLibria**.
  * Распознаёт релиз-группы (*SubsPlease, Erai-raws, Judas, EMBER, AniLibria*), видеокодеки (*HEVC x265 10-bit, AV1, x264*), качество (*1080p, 720p, 4K*) и аудиодорожки (*Dual-Audio, Multi-Sub*).
* **Фильтрация и поиск по раздачам**:
  * Быстрый текстовый фильтр по ключевым словам.
  * Горизонтальная лента чипов выбора серий (*«Все серии»*, *«Пакеты / Сезон»*, *«Сер 1»*, *«Сер 2»*...).
  * Выборочная загрузка конкретных файлов и воспроизведение офлайн.

---

### 🍿 Продвинутый видеоплеер (Media3 / ExoPlayer)
* **Пропуск заставок (AniSkip)** — автоматический или полуавтоматический пропуск опенингов и эндингов с настраиваемыми интервалами.
* **Поддержка субтитров** — поддержка встроенных дорожек и возможность загрузки внешних файлов `.srt` / `.vtt`.
* **Жестовое управление** — свайпы для регулировки яркости (слева), громкости (справа) и быстрой перемотки двойным тапом.
* **Режим «Картинка в картинке» (PiP)** и фоновое воспроизведение через `PlaybackService` с интеграцией в экран блокировки и гарнитуру.
* **Управление воспроизведением**: изменение скорости (от `0.25x` до `3.0x`), эквалайзер и переключение аудиодорожек.

---

### 🔄 Синхронизация и библиотека
* **Shikimori** — полная авторизация по OAuth 2.0, синхронизация списков просмотра, рейтингов и персональных отметок.
* **MyAnimeList (Jikan API)** — подробная информация о персонажах, сейю, кадрах из серий и рейтингах.
* **Локальная история и кэш** — сохранение прогресса с точностью до секунды и бесшовное продолжение просмотра.

---

## 🛠 Архитектура и стек технологий

Проект разработан в соответствии с принципами **Clean Architecture** и однонаправленным потоком данных (**MVI / MVVM**):

```
app/
├── src/main/java/com/anilibrix/plus/
│   ├── app/                      # Application класс, Hilt модули Dependency Injection
│   ├── core/                     # Базовые модули и системные сервисы
│   │   ├── database/             # Room Database v6 (Entity, DAO, Миграции)
│   │   ├── datastore/            # DataStore Preferences (Настройки, токены)
│   │   ├── download/             # Менеджер Media3 загрузок
│   │   ├── network/              # OkHttp перехватчики (Auth, Logging)
│   │   ├── notifications/        # Каналы уведомлений Android
│   │   ├── playback/             # MediaSessionService фонового плеера
│   │   ├── torrent/              # Torrent движок, BEncode, метаданные, парсер имен
│   │   └── sync/                 # Фоновая синхронизация (WorkManager)
│   ├── data/                     # Слой данных (Data Layer)
│   │   ├── remote/api/           # Retrofit интерфейсы (AniLibria, Kodik, Consumet, Shikimori, Jikan)
│   │   ├── remote/dto/           # Сериализуемые DTO модели (Kotlinx.Serialization)
│   │   └── repository/           # Реализации репозиториев
│   ├── domain/                   # Доменный слой (Domain Layer)
│   │   ├── model/                # Чистые доменные сущности
│   │   └── repository/           # Интерфейсы доступа к данным
│   └── ui/                       # Слой представления (Presentation Layer - Jetpack Compose)
│       ├── components/           # Переиспользуемые дизайн-компоненты
│       ├── detail/               # Экран деталей тайтла (Эпизоды, торренты, озвучки)
│       ├── downloads/            # Менеджер загрузок и торрент-задач
│       ├── player/               # Полноэкранный плеер с жестами и контролами
│       ├── profile/              # Профиль, настройки API и синхронизация
│       ├── theme/                # Цветовые схемы, типографика, формы, отступы
│       └── navigation/           # Навигационный граф и переходы
```

### Основные библиотеки:
| Категория | Библиотека | Назначение |
|---|---|---|
| **UI & UX** | Jetpack Compose / Material 3 | Декларативный UI с поддержкой Material You |
| **DI** | Dagger Hilt 2.51+ | Внедрение зависимостей |
| **Асинхронность** | Coroutines & StateFlow | Реактивное управление состоянием |
| **Сеть** | Retrofit 2 / OkHttp 3 | REST API клиенты |
| **Сериализация** | Kotlinx Serialization | Высокопроизводительный JSON парсинг |
| **База данных** | Room 2.6+ | Локальная БД для истории, кэша и торрентов |
| **Настройки** | Jetpack DataStore | Хранение пользовательских настроек |
| **Мультимедиа** | AndroidX Media3 (ExoPlayer) | Воспроизведение видео, HLS потоков и кэширование |
| **Изображения** | Landscapist Glide | Кэширование и crossfade-загрузка постеров |
| **Фоновые задачи** | AndroidX WorkManager | Фоновая синхронизация списков |

---

## 🚀 Установка и сборка

### Системные требования:
* **Android OS:** Android 13.0 (API 33) или новее.
* **JDK:** OpenJDK 21 (с поддержкой `jlink`).
* **Android Studio:** Ladybug / Koala или свежее.

### Клонирование и локальная сборка:

1. **Клонируйте репозиторий:**
   ```bash
   git clone https://github.com/lonxzsy/anilibrix-plus-mobile.git
   cd anilibrix-plus-mobile
   ```

2. **Сборка Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   *Готовый файл будет расположен по адресу:*  
   `app/build/outputs/apk/debug/app-debug.apk`

3. **Сборка Release APK:**
   ```bash
   ./gradlew assembleRelease
   ```

4. **Запуск Unit-тестов:**
   ```bash
   ./gradlew test
   ```

---

## 📡 Поддерживаемые API

* 🌸 **[AniLibria API v3](https://anilibria.tv)** — официальный каталог, эпизоды и HLS потоки.
* 🎬 **[Kodik API](https://kodik.biz)** — мультиязычные студии озвучки и субтитры.
* 🌍 **[Consumet API](https://consumet.org)** — международные источники аниме (Gogoanime).
* 📜 **[Shikimori API](https://shikimori.one)** — синхронизация пользовательских списков и OAuth.
* 🌐 **[Jikan / MyAnimeList](https://jikan.moe)** — метаданные, постеры, кадры и персонал.
* ⏱ **[AniSkip API](https://anime-skip.com)** — таймкоды опенингов и эндингов.
* 🧲 **[Nyaa.si](https://nyaa.si)** — каталог торрент-раздач в высоком качестве.

---

## 🤝 Вклад в разработку (Contributing)

Мы приветствуем любые улучшения, предложения и исправления!

1. Сделайте **Fork** проекта.
2. Создайте свою ветку фичи: `git checkout -b feature/my-new-feature`
3. Зафиксируйте изменения: `git commit -m 'Add awesome feature'`
4. Отправьте ветку в свой репозиторий: `git push origin feature/my-new-feature`
5. Создайте **Pull Request**.

---

## 📄 Лицензия

Проект распространяется под лицензией **MIT**. Подробности в файле [LICENSE](LICENSE).

---

<div align="center">
  <sub>Разработано с ❤️ для аниме-сообщества. Приложение является неофициальным клиентом.</sub>
</div>
