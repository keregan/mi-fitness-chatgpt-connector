import os
from dotenv import load_dotenv
from openai import OpenAI
from app.models import FitnessData

load_dotenv()

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))


def analyze_fitness_data(data: FitnessData) -> str:
    prompt = f"""
Проанализируй фитнес-данные пользователя за день.

Дата: {data.date}
Шаги: {data.steps}
Сон: {data.sleep_hours} часов
Качество сна: {data.sleep_quality}
Средний пульс: {data.average_heart_rate}
Тренировка: {data.workout.type}
Длительность тренировки: {data.workout.duration_minutes} минут
Интенсивность: {data.workout.intensity}
Заметки: {data.notes}

Дай короткий и понятный анализ:
1. Как прошёл день
2. Что хорошо
3. На что обратить внимание
4. Что лучше сделать завтра

Важно: не ставь диагнозы и не давай медицинских назначений.
"""

    response = client.responses.create(
        model="gpt-5.5",
        input=prompt
    )

    return response.output_text