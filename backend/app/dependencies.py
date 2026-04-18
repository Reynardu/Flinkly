from fastapi import Depends, HTTPException, status, Header
from sqlalchemy.orm import Session

from app.database import get_db
from app.models.user import User
from app.models.household import HouseholdMember, MemberRole
from app.services.auth_service import decode_access_token


def get_current_user(
    authorization: str = Header(..., description="Bearer <jwt-token>"),
    db: Session = Depends(get_db),
) -> User:
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Ungültiger Authorization Header")

    token = authorization.removeprefix("Bearer ")
    user_id = decode_access_token(token)

    user = db.get(User, user_id)
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User nicht gefunden")
    return user


def require_household_member(
    household_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> HouseholdMember:
    member = db.query(HouseholdMember).filter(
        HouseholdMember.household_id == household_id,
        HouseholdMember.user_id == current_user.id,
    ).first()
    if not member:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Kein Zugriff auf diesen Haushalt")
    return member


def require_household_owner(
    household_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> HouseholdMember:
    member = require_household_member(household_id, current_user, db)
    if member.role != MemberRole.OWNER:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Nur der Haushaltseigentümer darf das")
    return member
