from datetime import datetime
from pydantic import BaseModel, field_validator
from app.models.task import FrequencyType
from app.schemas.user import UserResponse


class TaskCreate(BaseModel):
    title: str
    description: str | None = None
    difficulty: int = 2
    frequency_type: FrequencyType = FrequencyType.WEEKLY
    frequency_value: str | None = None
    due_date: datetime | None = None
    auto_repeat: bool = True
    assigned_to_user_id: int | None = None
    is_suggestion: bool = False

    @field_validator("difficulty")
    @classmethod
    def validate_difficulty(cls, v: int) -> int:
        if not 1 <= v <= 5:
            raise ValueError("Schwierigkeit muss zwischen 1 und 5 liegen")
        return v


class TaskUpdate(BaseModel):
    title: str | None = None
    description: str | None = None
    difficulty: int | None = None
    frequency_type: FrequencyType | None = None
    frequency_value: str | None = None
    due_date: datetime | None = None
    auto_repeat: bool | None = None
    assigned_to_user_id: int | None = None
    photo_url: str | None = None


class CompletionCreate(BaseModel):
    photo_url: str | None = None
    note: str | None = None


class CompletionResponse(BaseModel):
    id: int
    task_id: int
    user: UserResponse
    completed_at: datetime
    points_earned: int
    photo_url: str | None
    note: str | None

    model_config = {"from_attributes": True}


class TaskResponse(BaseModel):
    id: int
    room_id: int
    title: str
    description: str | None
    difficulty: int
    frequency_type: FrequencyType
    frequency_value: str | None
    due_date: datetime | None
    auto_repeat: bool
    points: int
    photo_url: str | None
    is_suggestion: bool
    assigned_to_user_id: int | None
    next_due_at: datetime | None
    created_at: datetime
    completions: list[CompletionResponse] = []
    completion_count: int = 0

    model_config = {"from_attributes": True}


class TaskSuggestion(BaseModel):
    title: str
    description: str
    difficulty: int
    frequency_type: FrequencyType
    points: int
    reason: str
