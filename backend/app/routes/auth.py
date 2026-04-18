from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import get_current_user
from app.models.user import User
from app.schemas.user import UserResponse, UserUpdate
from app.services.auth_service import register_user, login_user

router = APIRouter()


class RegisterRequest(BaseModel):
    display_name: str
    password: str


class LoginRequest(BaseModel):
    user_secret: str


class AuthResponse(BaseModel):
    token: str
    user_secret: str
    user: UserResponse


@router.post("/register", response_model=AuthResponse)
def register(data: RegisterRequest, db: Session = Depends(get_db)):
    user, token = register_user(db, data.display_name, data.password)
    return AuthResponse(token=token, user_secret=user.user_secret, user=UserResponse.model_validate(user))


@router.post("/login", response_model=AuthResponse)
def login(data: LoginRequest, db: Session = Depends(get_db)):
    user, token = login_user(db, data.user_secret)
    return AuthResponse(token=token, user_secret=user.user_secret, user=UserResponse.model_validate(user))


@router.get("/me", response_model=UserResponse)
def get_me(current_user: User = Depends(get_current_user)):
    return current_user


@router.put("/me", response_model=UserResponse)
def update_me(
    data: UserUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    for field, value in data.model_dump(exclude_none=True).items():
        setattr(current_user, field, value)
    db.commit()
    db.refresh(current_user)
    return current_user
