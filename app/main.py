from fastapi import FastAPI
from app.models import FitnessData
from app.analyzer import analyze_fitness_data

app = FastAPI(title="FitnessGPT Sync")


@app.get("/")
def root():
    return {
        "message": "FitnessGPT Sync API is running"
    }


@app.post("/analyze")
def analyze(data: FitnessData):
    result = analyze_fitness_data(data)
    return {
        "analysis": result
    }