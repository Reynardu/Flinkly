from datetime import datetime
from pydantic import BaseModel


class RoomCreate(BaseModel):
    name: str
    icon: str = "home"
    color: str = "#4CAF50"


class RoomUpdate(BaseModel):
    name: str | None = None
    icon: str | None = None
    color: str | None = None


class RoomResponse(BaseModel):
    id: int
    household_id: int
    name: str
    icon: str
    color: str
    created_at: datetime
    task_count: int = 0
    open_task_count: int = 0

    model_config = {"from_attributes": True}
