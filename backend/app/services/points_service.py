from datetime import datetime, timedelta, timezone
from app.models.task import Task, FrequencyType

DIFFICULTY_POINTS = {1: 5, 2: 10, 3: 20, 4: 35, 5: 50}
FREQUENCY_MULTIPLIER = {
    FrequencyType.DAILY: 1.0,
    FrequencyType.WEEKLY: 1.2,
    FrequencyType.MONTHLY: 1.5,
    FrequencyType.CUSTOM: 1.2,
    FrequencyType.ONCE: 1.0,
}


def calculate_points(difficulty: int, frequency_type: FrequencyType) -> int:
    base = DIFFICULTY_POINTS.get(difficulty, 10)
    multiplier = FREQUENCY_MULTIPLIER.get(frequency_type, 1.0)
    return round(base * multiplier)


_WEEKDAY_MAP = {"MON": 0, "TUE": 1, "WED": 2, "THU": 3, "FRI": 4, "SAT": 5, "SUN": 6}


def _next_weekday_occurrence(now: datetime, frequency_value: str) -> datetime:
    target_days = sorted(
        _WEEKDAY_MAP[d.strip()]
        for d in frequency_value.split(",")
        if d.strip() in _WEEKDAY_MAP
    )
    if not target_days:
        return now + timedelta(weeks=1)
    current = now.weekday()
    for delta in range(1, 8):
        if (current + delta) % 7 in target_days:
            return now + timedelta(days=delta)
    return now + timedelta(weeks=1)


def calculate_next_due(task: Task) -> datetime | None:
    if not task.auto_repeat:
        return None

    now = datetime.now(timezone.utc)
    match task.frequency_type:
        case FrequencyType.DAILY:
            return now + timedelta(days=1)
        case FrequencyType.WEEKLY:
            if task.frequency_value:
                return _next_weekday_occurrence(now, task.frequency_value)
            return now + timedelta(weeks=1)
        case FrequencyType.MONTHLY:
            return now + timedelta(days=30)
        case FrequencyType.CUSTOM:
            return now + timedelta(days=7)
        case FrequencyType.ONCE:
            return None


def get_user_level(total_points: int) -> dict:
    levels = [
        (0, "Haushaltsneuling", "🌱"),
        (500, "Putz-Lehrling", "🧹"),
        (1500, "Ordnungs-Profi", "⭐"),
        (3500, "Haushaltsgott", "🏆"),
        (7500, "Legenden-Status", "👑"),
    ]
    current_level = levels[0]
    next_level = levels[1] if len(levels) > 1 else None

    for i, (threshold, title, icon) in enumerate(levels):
        if total_points >= threshold:
            current_level = (threshold, title, icon)
            next_level = levels[i + 1] if i + 1 < len(levels) else None

    result = {
        "title": current_level[1],
        "icon": current_level[2],
        "points": total_points,
    }
    if next_level:
        result["next_level_title"] = next_level[1]
        result["points_needed"] = next_level[0] - total_points
    return result
