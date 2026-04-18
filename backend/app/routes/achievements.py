from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import get_current_user
from app.models.achievement import Achievement
from app.models.household import HouseholdMember
from app.models.user import User
from app.schemas.achievement import AchievementResponse

router = APIRouter()


@router.get("/{user_id}", response_model=list[AchievementResponse])
def get_achievements(
    user_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    user = db.get(User, user_id)
    if not user:
        raise HTTPException(status_code=404, detail="User nicht gefunden")
    return db.query(Achievement).filter(Achievement.user_id == user_id).order_by(Achievement.earned_at.desc()).all()
