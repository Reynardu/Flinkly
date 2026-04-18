from datetime import datetime
from pydantic import BaseModel
from app.models.achievement import AchievementType


class AchievementResponse(BaseModel):
    id: int
    type: AchievementType
    title: str
    description: str
    icon: str
    earned_at: datetime

    model_config = {"from_attributes": True}


class ScoreEntry(BaseModel):
    user: "UserScoreSummary"
    points: int
    tasks_completed: int
    rank: int


class UserScoreSummary(BaseModel):
    id: int
    display_name: str
    avatar_url: str | None
    current_streak: int

    model_config = {"from_attributes": True}


ScoreEntry.model_rebuild()


class ScoreboardResponse(BaseModel):
    period: str  # "daily" | "weekly" | "monthly"
    entries: list[ScoreEntry]
    fairness_percent: dict[int, float]  # user_id -> prozent der Haushaltslast
