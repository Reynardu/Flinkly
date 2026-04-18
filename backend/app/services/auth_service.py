import secrets
from datetime import datetime, timedelta, timezone

import jwt
from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.config import settings
from app.models.user import User

JWT_ALGORITHM = "HS256"
JWT_EXPIRE_DAYS = 365


def verify_household_password(password: str):
    if password != settings.household_password:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Falsches Haushalt-Passwort",
        )


def create_access_token(user_id: int) -> str:
    payload = {
        "sub": str(user_id),
        "exp": datetime.now(timezone.utc) + timedelta(days=JWT_EXPIRE_DAYS),
    }
    return jwt.encode(payload, settings.secret_key, algorithm=JWT_ALGORITHM)


def decode_access_token(token: str) -> int:
    try:
        payload = jwt.decode(token, settings.secret_key, algorithms=[JWT_ALGORITHM])
        return int(payload["sub"])
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token abgelaufen")
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Ungültiger Token")


def register_user(db: Session, display_name: str, password: str) -> tuple[User, str]:
    verify_household_password(password)
    user = User(
        user_secret=secrets.token_hex(32),
        display_name=display_name,
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    token = create_access_token(user.id)
    return user, token


def login_user(db: Session, user_secret: str) -> tuple[User, str]:
    user = db.query(User).filter(User.user_secret == user_secret).first()
    if not user:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Unbekannter Nutzer")
    token = create_access_token(user.id)
    return user, token
