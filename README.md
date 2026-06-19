# Mi Fitness - ChatGPT Connector

**Mi Fitness - ChatGPT Connector** — это проект для связи фитнес-данных с Android-устройства и ChatGPT через собственный API.

Проект получает данные из **Health Connect**, отправляет их из Android-приложения на **FastAPI backend**, сохраняет последние синхронизированные данные на сервере и предоставляет их через API, который можно подключить к **ChatGPT Action**.

Главная цель проекта — сделать так, чтобы пользователь мог спрашивать ChatGPT о своей активности, сне, калориях, тренировках и восстановлении на основе реальных данных с устройства.

---

## Описание проекта

Проект состоит из трёх основных частей:

1. **Android-приложение**  
   Читает данные из Health Connect и отправляет их на backend.

2. **Backend на FastAPI**  
   Принимает данные от Android-приложения, сохраняет их и отдаёт по API.

3. **ChatGPT Action**  
   Позволяет ChatGPT получать актуальные фитнес-данные и анализировать их в диалоге.

---

## Архитектура

```text
Mi Fitness / фитнес-браслет
        ↓
Health Connect
        ↓
Android-приложение
        ↓
POST /sync/today
        ↓
FastAPI backend
        ↓
GET /today
        ↓
ChatGPT Action
        ↓
Анализ данных в ChatGPT
```

---

## Как работает проект

1. Mi Fitness или другое фитнес-приложение передаёт поддерживаемые данные в Health Connect.
2. Android-приложение запрашивает разрешения Health Connect.
3. Пользователь нажимает кнопку чтения данных в приложении.
4. Приложение получает данные за текущий день.
5. Данные преобразуются в JSON.
6. Android-приложение отправляет JSON на backend.
7. Backend сохраняет последнюю синхронизацию.
8. ChatGPT Action получает данные через API.
9. ChatGPT анализирует активность, сон, калории, тренировки и восстановление.

---

## Реализованный функционал

### Android-приложение

Android-приложение написано на **Kotlin** с использованием **Jetpack Compose**.

На текущем этапе приложение умеет:

- подключаться к Health Connect;
- запрашивать разрешения на чтение данных;
- читать шаги за день;
- читать данные сна;
- отображать стадии сна:
  - быстрый сон;
  - крепкий сон;
  - поверхностный сон;
- читать пульс, если данные доступны;
- читать дистанцию, если данные доступны;
- читать активные калории;
- читать общие калории;
- читать тренировки, если данные доступны;
- формировать JSON с данными;
- копировать JSON в буфер обмена;
- отправлять JSON на backend;
- брать адрес API и токен из `local.properties`.

---

### Backend

Backend написан на **FastAPI**.

Он предоставляет endpoints:

```text
GET /
POST /sync/today
GET /today
GET /fitness/today
```

Назначение endpoints:

```text
GET /
```

Проверка, что API работает.

```text
POST /sync/today
```

Приём фитнес-данных от Android-приложения.

```text
GET /today
```

Получение последних синхронизированных фитнес-данных.

```text
GET /fitness/today
```

Альтернативный путь для получения последних данных.

---

## Безопасность

Проект работает с личными данными здоровья, поэтому API защищён токеном.

Для доступа используется HTTP-заголовок:

```text
x-fitness-token
```

Токен хранится:

- на сервере в `.env`;
- на Android-устройстве в `local.properties`;
- в настройках ChatGPT Action.

Токен не должен попадать в GitHub.

---

## Структура проекта

```text
mi-fitness-chatgpt-connector/
│
├── android-app/
│   ├── app/
│   │   ├── src/
│   │   └── build.gradle.kts
│   ├── local.properties
│   └── local.properties.example
│
├── backend/
│   ├── main.py
│   ├── Dockerfile
│   ├── requirements.txt
│   └── .env.example
│
├── docs/
│   └── openapi.yaml
│
├── README.md
└── .gitignore
```

---

## Настройка Android-приложения

### 1. Открыть проект

В Android Studio нужно открыть папку:

```text
android-app/
```

---

### 2. Настроить `local.properties`

Файл находится здесь:

```text
android-app/local.properties
```

Пример содержимого:

```properties
sdk.dir=C\:\\Users\\YourUser\\AppData\\Local\\Android\\Sdk

FITNESS_API_URL=https://your-domain.example/fitness/sync/today
FITNESS_API_TOKEN=your_secret_token_here
```

Важно: настоящий `local.properties` нельзя загружать в GitHub.

---

### 3. Разрешения Health Connect

В приложении используются разрешения:

```xml
<uses-permission android:name="android.permission.health.READ_STEPS" />
<uses-permission android:name="android.permission.health.READ_SLEEP" />
<uses-permission android:name="android.permission.health.READ_HEART_RATE" />
<uses-permission android:name="android.permission.health.READ_EXERCISE" />
<uses-permission android:name="android.permission.health.READ_DISTANCE" />
<uses-permission android:name="android.permission.health.READ_ACTIVE_CALORIES_BURNED" />
<uses-permission android:name="android.permission.health.READ_TOTAL_CALORIES_BURNED" />
```

Также требуется доступ к интернету:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

---

### 4. Запуск приложения

В Android Studio:

```text
Run → выбрать устройство → запустить приложение
```

В приложении:

1. Нажать **Прочитать данные за сегодня**.
2. Выдать разрешения Health Connect.
3. Нажать **Отправить на сервер**.

---

## Настройка backend локально

### 1. Создать виртуальное окружение

```bash
cd backend
python -m venv venv
```

Активация на Windows:

```bash
venv\Scripts\activate
```

Активация на Linux/macOS:

```bash
source venv/bin/activate
```

---

### 2. Установить зависимости

```bash
pip install -r requirements.txt
```

---

### 3. Указать API-токен

Windows PowerShell:

```powershell
$env:FITNESS_API_TOKEN="your_secret_token_here"
```

Linux/macOS:

```bash
export FITNESS_API_TOKEN="your_secret_token_here"
```

---

### 4. Запустить backend

```bash
python -m uvicorn main:app --reload
```

Локальный адрес:

```text
http://127.0.0.1:8000
```

Swagger-документация:

```text
http://127.0.0.1:8000/docs
```

---

## Docker deployment

Backend можно запускать через Docker.

### Dockerfile

```dockerfile
FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

COPY main.py .

RUN mkdir -p data

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
```

---

### Пример `docker-compose.yml`

```yaml
services:
  fitness-backend:
    build:
      context: ./backend
    container_name: fitness-backend
    restart: unless-stopped
    env_file:
      - .env
    volumes:
      - fitness_data:/app/data
    networks:
      - fitness_network
      - caddy_network

networks:
  fitness_network:
    name: fitness_network

  caddy_network:
    external: true
    name: your_caddy_network_name

volumes:
  fitness_data:
```

---

### Пример `.env`

```env
FITNESS_API_TOKEN=your_secret_token_here
```

Файл `.env` нельзя загружать в GitHub.

---

## Пример настройки Caddy

Если backend должен быть доступен по пути `/fitness`, можно использовать такую конфигурацию:

```caddy
your-domain.example {
    handle_path /fitness/* {
        reverse_proxy fitness-backend:8000
    }

    handle {
        reverse_proxy another-service:8000
    }
}
```

После такой настройки будут доступны:

```text
https://your-domain.example/fitness/today
https://your-domain.example/fitness/sync/today
https://your-domain.example/fitness/docs
```

---

## Настройка ChatGPT Action

Проект можно подключить к пользовательскому GPT через **Actions**.

### Authentication

В настройках Action нужно выбрать:

```text
Authentication Type: API Key
Auth Type: Custom
Header name: x-fitness-token
Value: your_secret_token_here
```

Токен вставляется только в настройки Action.  
В OpenAPI-схему токен вставлять не нужно.

---

### OpenAPI Schema

Пример схемы находится в файле:

```text
docs/openapi.yaml
```

Минимальная схема:

```yaml
openapi: 3.1.0

info:
  title: Mi Fitness ChatGPT Connector API
  description: API for reading fitness data synced from Android Health Connect.
  version: 0.1.0

servers:
  - url: https://your-domain.example/fitness

paths:
  /today:
    get:
      operationId: getTodayFitnessData
      summary: Get today's fitness data
      description: Returns the latest synced fitness data from Health Connect.
      security:
        - fitnessTokenAuth: []
      responses:
        "200":
          description: Latest synced fitness data
          content:
            application/json:
              schema:
                type: object
                additionalProperties: true

components:
  securitySchemes:
    fitnessTokenAuth:
      type: apiKey
      in: header
      name: x-fitness-token

  schemas: {}
```

---

## Пример ответа API

```json
{
  "status": "success",
  "data": {
    "date": "2026-06-02",
    "source": "Health Connect",
    "activity": {
      "steps": 6774,
      "distanceMeters": null
    },
    "sleep": {
      "totalMinutes": 362,
      "total": "6 ч 2 мин",
      "remMinutes": 43,
      "rem": "43 мин",
      "deepMinutes": 80,
      "deep": "1 ч 20 мин",
      "lightMinutes": 239,
      "light": "3 ч 59 мин"
    },
    "heartRate": {
      "average": null,
      "min": null,
      "max": null
    },
    "calories": {
      "activeKcal": 47,
      "totalKcal": 722
    },
    "workouts": []
  }
}
```

---

## Конфиденциальность

Проект работает с персональными фитнес-данными. Поэтому важно:

- не загружать реальные данные здоровья в GitHub;
- не коммитить `.env`;
- не коммитить `local.properties`;
- не хранить токены в коде;
- не хранить личный домен в публичных файлах;
- использовать HTTPS;
- защищать endpoints токеном;
- менять токен, если он случайно был раскрыт.

---

## Рекомендуемый `.gitignore`

```gitignore
# Android
android-app/local.properties
local.properties
.gradle/
*/build/
.idea/
*.iml

# Backend
backend/venv/
backend/__pycache__/
backend/*.pyc
backend/data/
backend/fitness_storage.json
backend/.env

# OS
.DS_Store
Thumbs.db
```

---

## Статус проекта

Текущий статус: **рабочий MVP**.

Уже реализовано:

- Android-приложение;
- подключение Health Connect;
- чтение фитнес-данных;
- формирование JSON;
- отправка данных на backend;
- FastAPI backend;
- Docker deployment;
- Caddy reverse proxy;
- защита через токен;
- ChatGPT Action;
- анализ данных через ChatGPT.

Планируется реализоать:
- полностью автоматизированную передачу данных с приложение Mi-fitness в ChatGpt
- оптимизировать еженедельный анализ сна, пульса и бега
- поставитьвать состояние и какчество сна под тренировки бега и физические тренировки
- передавать информацию если произошли какие либо крупыне изменение в ухудшения здоровье

---

## Используемые технологии

- Kotlin
- Jetpack Compose
- Android Health Connect
- FastAPI
- Pydantic
- Uvicorn
- Docker
- Docker Compose
- Caddy
- OpenAPI
- ChatGPT Actions

---

## Репозиторий

```text
https://github.com/keregan/mi-fitness-chatgpt-connector
```
