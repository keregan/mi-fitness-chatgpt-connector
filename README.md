# Mi Fitness → ChatGPT Connector

**Mi Fitness → ChatGPT Connector** is a portfolio project that connects fitness data from an Android device to ChatGPT through a custom API.

The project reads health data from **Health Connect**, sends it from an Android application to a **FastAPI backend**, stores the latest synced data on the server, and exposes it through an API that can be connected to a **ChatGPT Action**.

The main goal of the project is to make it possible to ask ChatGPT questions about daily activity, sleep, calories, workouts, and recovery based on real data from the user’s device.

---

## Project Overview

The current implementation includes:

- Android application written in **Kotlin** and **Jetpack Compose**
- Integration with **Health Connect**
- Reading daily health metrics from the device
- JSON export of health data
- Sending health data from Android to a backend server
- FastAPI backend for receiving and serving synced data
- Docker-based deployment
- Caddy reverse proxy support
- Token-based API protection
- ChatGPT Action integration

---

## Architecture

```text
Mi Fitness / wearable device
        ↓
Health Connect
        ↓
Android application
        ↓
POST /sync/today
        ↓
FastAPI backend
        ↓
GET /fitness/today or /today
        ↓
ChatGPT Action
        ↓
Fitness data analysis in ChatGPT
```

---

## Data Flow

1. Mi Fitness writes supported health data into Health Connect.
2. The Android application requests Health Connect permissions.
3. The user reads health data inside the app.
4. The app formats the data into JSON.
5. The app sends the JSON to the backend.
6. The backend stores the latest synced data.
7. ChatGPT Action requests the latest data from the backend.
8. ChatGPT analyzes the data and gives a human-readable summary.

---

## Current Features

### Android App

The Android application can currently read and display:

- Steps
- Sleep duration
- Sleep stages:
  - REM / fast sleep
  - Deep sleep
  - Light sleep
- Heart rate data, if available
- Distance, if available
- Active calories
- Total calories
- Workouts, if available

The app also supports:

- Compact health data screen
- JSON generation
- Copying JSON to clipboard
- Sending JSON to the backend server
- API URL and token configuration through `local.properties`

---

### Backend

The backend is built with **FastAPI** and provides:

- `GET /` — basic health check
- `POST /sync/today` — receive today’s health data from Android
- `GET /today` — return the latest synced health data
- `GET /fitness/today` — alternative route for the latest synced data

The backend stores the latest synced data in a local JSON file inside the container volume.

---

### Security

The backend uses a custom API token header:

```text
x-fitness-token
```

Both sync and read endpoints are protected with this token.

Sensitive values are not stored in the repository. The project uses local configuration files and environment variables for secrets.

---

## Project Structure

```text
mi-fitness-chatgpt-connector/
│
├── android-app/
│   ├── app/
│   │   ├── src/
│   │   └── build.gradle.kts
│   ├── local.properties
│   └── ...
│
├── backend/
│   ├── main.py
│   ├── Dockerfile
│   └── requirements.txt
│
├── docs/
│   └── openapi.yaml
│
├── README.md
└── .gitignore
```

---

## Android Setup

### 1. Open the Android project

Open the following folder in Android Studio:

```text
android-app/
```

### 2. Configure local properties

Create or update:

```text
android-app/local.properties
```

Example:

```properties
sdk.dir=C\:\\Users\\YourUser\\AppData\\Local\\Android\\Sdk

FITNESS_API_URL=https://your-domain.example/fitness/sync/today
FITNESS_API_TOKEN=your_secret_token_here
```

Do not commit this file to GitHub.

### 3. Required Health Connect permissions

The Android app uses the following Health Connect permissions:

```xml
<uses-permission android:name="android.permission.health.READ_STEPS" />
<uses-permission android:name="android.permission.health.READ_SLEEP" />
<uses-permission android:name="android.permission.health.READ_HEART_RATE" />
<uses-permission android:name="android.permission.health.READ_EXERCISE" />
<uses-permission android:name="android.permission.health.READ_DISTANCE" />
<uses-permission android:name="android.permission.health.READ_ACTIVE_CALORIES_BURNED" />
<uses-permission android:name="android.permission.health.READ_TOTAL_CALORIES_BURNED" />
```

The app also requires internet access:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### 4. Run the app

In Android Studio:

```text
Run → Select device → Start application
```

Inside the app:

1. Tap **Read today’s data**
2. Allow Health Connect permissions
3. Tap **Send to server**

---

## Backend Setup

### 1. Create virtual environment

```bash
cd backend
python -m venv venv
```

Activate it on Windows:

```bash
venv\Scripts\activate
```

Activate it on Linux/macOS:

```bash
source venv/bin/activate
```

### 2. Install dependencies

```bash
pip install -r requirements.txt
```

### 3. Set API token

On Windows PowerShell:

```powershell
$env:FITNESS_API_TOKEN="your_secret_token_here"
```

On Linux/macOS:

```bash
export FITNESS_API_TOKEN="your_secret_token_here"
```

### 4. Run backend locally

```bash
python -m uvicorn main:app --reload
```

Backend will be available at:

```text
http://127.0.0.1:8000
```

Swagger documentation:

```text
http://127.0.0.1:8000/docs
```

---

## Docker Deployment

The backend can be deployed with Docker.

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

### docker-compose.yml example

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

### .env example

```env
FITNESS_API_TOKEN=your_secret_token_here
```

Do not commit `.env` to GitHub.

---

## Caddy Reverse Proxy Example

If the backend is deployed behind Caddy and should be available under `/fitness`, an example configuration is:

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

With this setup:

```text
https://your-domain.example/fitness/today
https://your-domain.example/fitness/sync/today
https://your-domain.example/fitness/docs
```

will be routed to the fitness backend.

---

## ChatGPT Action Setup

The project can be connected to a custom GPT through **Actions**.

### Authentication

Use API Key authentication:

```text
Authentication Type: API Key
Auth Type: Custom
Header name: x-fitness-token
Value: your_secret_token_here
```

### OpenAPI Schema

Example schema:

```yaml
openapi: 3.1.0

info:
  title: Mi Fitness ChatGPT Connector API
  description: API for reading fitness, sleep, heart rate and activity data synced from Android Health Connect.
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

## Example API Response

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

## Privacy Notes

This project works with personal health-related data.

Recommended precautions:

- Do not commit real health data to GitHub
- Do not commit `.env`
- Do not commit `local.properties`
- Do not hardcode personal domains or tokens in tracked files
- Use HTTPS for deployed APIs
- Protect all sensitive endpoints with a token
- Rotate the token if it was accidentally exposed

---

## Git Ignore Recommendations

Recommended entries:

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

## Project Status

Current status: **working MVP**

Implemented:

- Android Health Connect integration
- Health data reading
- JSON export
- Backend sync endpoint
- Backend read endpoint
- Docker deployment
- Caddy route
- Token protection
- ChatGPT Action integration

---

## Future Improvements

Planned improvements:

- Store history by date instead of only latest synced data
- Add weekly and monthly summaries
- Add charts for activity and sleep
- Add better workout type mapping
- Add background sync from Android
- Add manual date selection
- Add support for multiple users
- Improve backend persistence with a database
- Add automated tests
- Add CI/CD pipeline
- Improve GPT Action schema with stricter response models

---

## Technologies Used

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

## Repository

```text
https://github.com/keregan/mi-fitness-chatgpt-connector
```
