from datetime import datetime
from pydantic import BaseModel


class UserBase(BaseModel):
    display_name: str
    daily_point_goal: int = 50


class UserUpdate(BaseModel):
    display_name: str | None = None
    daily_point_goal: int | None = None


class UserResponse(UserBase):
    id: int
    total_points: int
    current_streak: int
    longest_streak: int
    created_at: datetime

    model_config = {"from_attributes": True}
