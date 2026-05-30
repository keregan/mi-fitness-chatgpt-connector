from pydantic import BaseModel


class WorkoutData(BaseModel):
    type: str
    duration_minutes: int
    intensity: str


class FitnessData(BaseModel):
    date: str
    steps: int
    sleep_hours: float
    sleep_quality: str
    average_heart_rate: int
    workout: WorkoutData
    notes: str | None = None