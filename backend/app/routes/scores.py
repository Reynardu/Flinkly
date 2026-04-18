from datetime import datetime, timedelta, timezone
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import get_current_user
from app.models.household import HouseholdMember, HouseholdPause
from app.models.task import TaskCompletion
from app.models.user import User
from app.schemas.achievement import ScoreboardResponse, ScoreEntry, UserScoreSummary
from app.services.points_service import get_user_level

router = APIRouter()


@router.get("/{household_id}/{period}", response_model=ScoreboardResponse)
def get_scoreboard(
    household_id: int,
    period: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    _require_member(db, household_id, current_user.id)

    now = datetime.now(timezone.utc)
    match period:
        case "daily":
            since = now.replace(hour=0, minute=0, second=0, microsecond=0)
        case "weekly":
            since = now - timedelta(days=now.weekday())
            since = since.replace(hour=0, minute=0, second=0, microsecond=0)
        case "monthly":
            since = now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
        case _:
            raise HTTPException(status_code=400, detail="period muss daily, weekly oder monthly sein")

    members = db.query(HouseholdMember).filter(HouseholdMember.household_id == household_id).all()

    scores = []
    total_points_all = 0

    for member in members:
        user = member.user
        points = db.query(func.sum(TaskCompletion.points_earned)).filter(
            TaskCompletion.user_id == user.id,
            TaskCompletion.completed_at >= since,
        ).scalar() or 0
        tasks_done = db.query(func.count(TaskCompletion.id)).filter(
            TaskCompletion.user_id == user.id,
            TaskCompletion.completed_at >= since,
        ).scalar() or 0

        scores.append((user, points, tasks_done))
        total_points_all += points

    scores.sort(key=lambda x: x[1], reverse=True)

    entries = [
        ScoreEntry(
            user=UserScoreSummary.model_validate(user),
            points=points,
            tasks_completed=tasks_done,
            rank=rank + 1,
        )
        for rank, (user, points, tasks_done) in enumerate(scores)
    ]

    fairness = {}
    for user, points, _ in scores:
        fairness[user.id] = round((points / total_points_all * 100) if total_points_all > 0 else 0, 1)

    return ScoreboardResponse(period=period, entries=entries, fairness_percent=fairness)


@router.get("/{household_id}/level/{user_id}")
def get_user_level_info(
    household_id: int,
    user_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    _require_member(db, household_id, current_user.id)
    user = db.get(User, user_id)
    if not user:
        raise HTTPException(status_code=404, detail="User nicht gefunden")
    return get_user_level(user.total_points)


@router.get("/{household_id}/daily-progress")
def get_daily_progress(
    household_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    _require_member(db, household_id, current_user.id)
    now = datetime.now(timezone.utc)
    today_start = now.replace(hour=0, minute=0, second=0, microsecond=0)
    today_date = now.date()
    today_points = db.query(func.sum(TaskCompletion.points_earned)).filter(
        TaskCompletion.user_id == current_user.id,
        TaskCompletion.completed_at >= today_start,
    ).scalar() or 0

    active_pause = db.query(HouseholdPause).filter(
        HouseholdPause.household_id == household_id,
        HouseholdPause.start_date <= today_date,
        HouseholdPause.end_date >= today_date,
    ).first()

    return {
        "today_points": today_points,
        "daily_goal": current_user.daily_point_goal,
        "percent": min(round(today_points / current_user.daily_point_goal * 100), 100) if current_user.daily_point_goal > 0 else 0,
        "goal_reached": today_points >= current_user.daily_point_goal,
        "streak": current_user.current_streak,
        "is_paused": active_pause is not None,
        "pause_reason": active_pause.reason if active_pause else None,
        "pause_end_date": str(active_pause.end_date) if active_pause else None,
    }


def _require_member(db: Session, household_id: int, user_id: int):
    member = db.query(HouseholdMember).filter(
        HouseholdMember.household_id == household_id,
        HouseholdMember.user_id == user_id,
    ).first()
    if not member:
        raise HTTPException(status_code=403, detail="Kein Zugriff auf diesen Haushalt")
