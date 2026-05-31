from fastapi import FastAPI, Header, HTTPException
from pydantic import BaseModel
from typing import Optional, List, Any
import json
import os
from pathlib import Path


app = FastAPI(
    title="Mi Fitness ChatGPT Connector API",
    description="Backend for receiving Health Connect data and providing it to ChatGPT Action.",
    version="0.1.0"
)

API_TOKEN = os.getenv("FITNESS_API_TOKEN")

DATA_DIR = Path("data")
DATA_DIR.mkdir(exist_ok=True)

STORAGE_FILE = DATA_DIR / "fitness_storage.json"


class ActivityData(BaseModel):
    steps: int
    distanceMeters: Optional[int] = None


class SleepData(BaseModel):
    totalMinutes: Optional[int] = None
    total: Optional[str] = None
    remMinutes: Optional[int] = None
    rem: Optional[str] = None
    deepMinutes: Optional[int] = None
    deep: Optional[str] = None
    lightMinutes: Optional[int] = None
    light: Optional[str] = None


class HeartRateData(BaseModel):
    average: Optional[int] = None
    min: Optional[int] = None
    max: Optional[int] = None


class CaloriesData(BaseModel):
    activeKcal: Optional[int] = None
    totalKcal: Optional[int] = None


class FitnessData(BaseModel):
    date: str
    source: str
    activity: ActivityData
    sleep: SleepData
    heartRate: HeartRateData
    calories: CaloriesData
    workouts: List[Any] = []


def verify_token(x_fitness_token: Optional[str]):
    if not API_TOKEN:
        raise HTTPException(
            status_code=500,
            detail="FITNESS_API_TOKEN is not configured on server"
        )

    if x_fitness_token != API_TOKEN:
        raise HTTPException(
            status_code=401,
            detail="Invalid or missing token"
        )


@app.get("/")
def root():
    return {
        "message": "Mi Fitness ChatGPT Connector API is running"
    }


@app.post("/sync/today")
def sync_today(
    data: FitnessData,
    x_fitness_token: Optional[str] = Header(default=None)
):
    verify_token(x_fitness_token)

    with STORAGE_FILE.open("w", encoding="utf-8") as file:
        json.dump(data.model_dump(), file, ensure_ascii=False, indent=2)

    return {
        "status": "success",
        "message": "Fitness data saved",
        "date": data.date
    }


@app.get("/fitness/today")
def get_today_fitness_data(
    x_fitness_token: Optional[str] = Header(default=None)
):
    verify_token(x_fitness_token)

    if not STORAGE_FILE.exists():
        return {
            "status": "empty",
            "message": "No fitness data synced yet"
        }

    with STORAGE_FILE.open("r", encoding="utf-8") as file:
        data = json.load(file)

    return {
        "status": "success",
        "data": data
    }