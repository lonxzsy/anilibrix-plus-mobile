# AnilibrixPlus

Современный Android-клиент для просмотра аниме на базе Anilibria с Material Design 3.

## Описание

AnilibrixPlus — это полнофункциональное приложение для Android, предоставляющее удобный доступ к каталогу аниме Anilibria. Приложение построено с использованием современного стека технологий Android и следует принципам Clean Architecture.

## Особенности

- **Material Design 3** — современный и адаптивный интерфейс
- **Каталог аниме** — обширная библиотека с фильтрами и поиском
- **Видеоплеер** — встроенный плеер с поддержкой HLS и субтитров
- **Расписание** — следите за новыми релизами
- **Библиотека** — управляйте избранным, плейлистами и историей просмотра
- **Коллекции** — организуйте аниме по статусам (смотрю, буду смотреть, просмотрено)
- **Студии** — поиск и просмотр аниме из внешних источников
- **Офлайн-режим** — кэширование данных для работы без интернета
- **Синхронизация** — интеграция с MyAnimeList для получения дополнительной информации
- **Темная тема** — поддержка светлой и темной тем

## Технологии

### Архитектура и паттерны
- Clean Architecture (Domain, Data, Presentation)
- MVVM с Unidirectional Data Flow
- Repository Pattern
- Dependency Injection (Hilt)

### UI
- Jetpack Compose
- Material Design 3
- Navigation Compose
- Lottie Animations

### Сеть
- Retrofit 2
- OkHttp 3
- Kotlinx Serialization

### Локальное хранилище
- Room Database
- DataStore (Preferences)

### Медиа
- ExoPlayer (Media3)
- HLS Streaming
- Пользовательские субтитры

### Дополнительно
- Kotlin Coroutines & Flow
- Glide для загрузки изображений
- Unit тесты (JUnit, MockK, Turbine)

## Требования

- Android 13 (API 33) или выше
- Интернет-соединение

## Установка

### Из исходного кода

1. Клонируйте репозиторий:
```bash
git clone https://github.com/ваш-username/anilibrix-plus-mobile.git
cd anilibrix-plus-mobile
```

2. Откройте проект в Android Studio

3. Соберите и запустите приложение:
```bash
./gradlew assembleDebug
```

## Сборка

### Debug версия
```bash
./gradlew assembleDebug
```

### Release версия
```bash
./gradlew assembleRelease
```

## Структура проекта

```
app/
├── src/main/java/com/anilibrix/plus/
│   ├── app/                    # Application и DI
│   ├── core/                   # Базовые компоненты
│   │   ├── database/          # Room entities и DAO
│   │   ├── datastore/         # DataStore
│   │   ├── network/           # Interceptors
│   │   └── util/              # Утилиты
│   ├── data/                   # Data layer
│   │   ├── local/             # Локальные источники данных
│   │   ├── remote/            # API и DTO
│   │   └── repository/        # Реализации репозиториев
│   ├── domain/                 # Domain layer
│   │   ├── model/             # Доменные модели
│   │   ├── repository/        # Интерфейсы репозиториев
│   │   └── usecase/           # Use cases
│   └── ui/                     # Presentation layer
│       ├── components/        # Переиспользуемые компоненты
│       ├── navigation/        # Навигация
│       ├── theme/             # Тема приложения
│       └── [screens]/         # Экраны приложения
```

## Основные экраны

- **Главная** — популярное аниме, продолжить просмотр, рекомендации
- **Каталог** — поиск и фильтрация по жанрам, типам, годам
- **Расписание** — расписание выхода новых серий
- **Библиотека** — избранное, плейлисты, история просмотра
- **Профиль** — настройки, авторизация, информация о приложении
- **Детали тайтла** — полная информация, список серий, связанные тайтлы
- **Плеер** — воспроизведение с поддержкой жестов и субтитров
- **Студии** — поиск и просмотр из внешних источников

## API

Приложение использует следующие API:
- Anilibria API — основной источник аниме контента
- MyAnimeList (Jikan) — дополнительная информация и рейтинги
- GitHub API — проверка обновлений

## Разработка

### Код стайл
Проект следует официальным [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)

### Тестирование
```bash
# Unit тесты
./gradlew test

# Android Instrumented тесты
./gradlew connectedAndroidTest
```

## Вклад в проект

Буду рад вашему участию! Вот как вы можете помочь:

1. Fork репозитория
2. Создайте ветку для вашей фичи (`git checkout -b feature/amazing-feature`)
3. Commit изменения (`git commit -m 'Add amazing feature'`)
4. Push в ветку (`git push origin feature/amazing-feature`)
5. Откройте Pull Request

## Лицензия

Этот проект распространяется под лицензией MIT. См. файл `LICENSE` для подробностей.

## Контакты

Если у вас есть вопросы или предложения, создайте issue в репозитории.

## Благодарности

- [Anilibria](https://anilibria.tv) — за предоставление API и контента
- [Material Design](https://m3.material.io/) — за дизайн-систему
- Android community — за отличные библиотеки и инструменты

---

**Примечание:** Это неофициальное приложение, не связанное с командой Anilibria.
