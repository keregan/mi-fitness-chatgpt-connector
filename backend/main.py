from fastapi import FastAPI
from pydantic import BaseModel
from typing import Optional, List, Any
import json
from pathlib import Path


app = FastAPI(
    title="Mi Fitness ChatGPT Connector API",
    description="Backend for receiving Health Connect data and providing it to ChatGPT Action.",
    version="0.1.0"
)

STORAGE_FILE = Path("fitness_storage.json")


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


@app.get("/")
def root():
    return {
        "message": "Mi Fitness ChatGPT Connector API is running"
    }


@app.post("/sync/today")
def sync_today(data: FitnessData):
    with STORAGE_FILE.open("w", encoding="utf-8") as file:
        json.dump(data.model_dump(), file, ensure_ascii=False, indent=2)

    return {
        "status": "success",
        "message": "Fitness data saved",
        "date": data.date
    }


@app.get("/fitness/today")
def get_today_fitness_data():
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