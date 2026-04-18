from datetime import datetime, date
from pydantic import BaseModel, model_validator
from app.models.household import MemberRole
from app.schemas.user import UserResponse


class HouseholdCreate(BaseModel):
    name: str


class HouseholdUpdate(BaseModel):
    name: str


class MemberResponse(BaseModel):
    id: int
    user: UserResponse
    role: MemberRole
    joined_at: datetime

    model_config = {"from_attributes": True}


class HouseholdResponse(BaseModel):
    id: int
    name: str
    created_at: datetime
    members: list[MemberResponse] = []

    model_config = {"from_attributes": True}


class InviteLinkResponse(BaseModel):
    invite_url: str
    token: str


class PauseCreate(BaseModel):
    start_date: date
    end_date: date
    reason: str | None = None

    @model_validator(mode="after")
    def end_after_start(self) -> "PauseCreate":
        if self.end_date < self.start_date:
            raise ValueError("end_date muss nach start_date liegen")
        return self


class PauseResponse(BaseModel):
    id: int
    household_id: int
    start_date: date
    end_date: date
    reason: str | None
    created_by_user_id: int
    created_at: datetime

    model_config = {"from_attributes": True}
