from datetime import datetime, timezone
from sqlalchemy.orm import Session

from app.models.achievement import Achievement, AchievementType
from app.models.task import TaskCompletion
from app.models.user import User

ACHIEVEMENT_DEFINITIONS = {
    AchievementType.FIRST_TASK: ("Erster Schritt", "Erste Aufgabe erledigt!", "🧹"),
    AchievementType.STREAK_7: ("Wochenheld", "7 Tage in Folge Tagesziel erreicht", "🔥"),
    AchievementType.STREAK_30: ("Monatsmeister", "30 Tage Streak!", "🌟"),
    AchievementType.SCORE_100: ("Centurion", "100 Punkte gesammelt", "💯"),
    AchievementType.SCORE_1000: ("Tausender", "1000 Punkte gesammelt", "🏅"),
    AchievementType.FIVE_IN_ONE_DAY: ("Schnellputzer", "5 Aufgaben an einem Tag", "⚡"),
    AchievementType.MONTHLY_WINNER: ("Monatssieger", "Meiste Punkte diesen Monat", "🏆"),
    AchievementType.TEAMPLAYER: ("Teamplayer", "Gleiche Aufgabe wie Partner 3x in einer Woche", "🤝"),
    AchievementType.LEVEL_UP: ("Aufgestiegen", "Neues Level erreicht", "⭐"),
}


def check_and_award_achievements(db: Session, user: User) -> list[Achievement]:
    awarded = []
    existing = {a.type for a in user.achievements}

    def award(achievement_type: AchievementType):
        if achievement_type in existing:
            return
        title, description, icon = ACHIEVEMENT_DEFINITIONS[achievement_type]
        achievement = Achievement(
            user_id=user.id,
            type=achievement_type,
            title=title,
            description=description,
            icon=icon,
        )
        db.add(achievement)
        awarded.append(achievement)
        existing.add(achievement_type)

    total_completions = db.query(TaskCompletion).filter(TaskCompletion.user_id == user.id).count()
    if total_completions >= 1:
        award(AchievementType.FIRST_TASK)

    if user.current_streak >= 7:
        award(AchievementType.STREAK_7)
    if user.current_streak >= 30:
        award(AchievementType.STREAK_30)

    if user.total_points >= 100:
        award(AchievementType.SCORE_100)
    if user.total_points >= 1000:
        award(AchievementType.SCORE_1000)

    today = datetime.now(timezone.utc).date()
    today_completions = db.query(TaskCompletion).filter(
        TaskCompletion.user_id == user.id,
        TaskCompletion.completed_at >= datetime.combine(today, datetime.min.time()),
    ).count()
    if today_completions >= 5:
        award(AchievementType.FIVE_IN_ONE_DAY)

    db.commit()
    return awarded


def update_streak(db: Session, user: User, household_id: int | None = None):
    from datetime import timedelta
    from app.models.household import HouseholdPause

    today = datetime.now(timezone.utc).date()

    if user.last_active_date:
        last_date = user.last_active_date.date()
        if last_date == today:
            return

        # Streak nicht brechen wenn der Vortag in einer Haushaltspause lag
        gap_covered_by_pause = False
        if household_id and last_date < today - timedelta(days=1):
            pause = db.query(HouseholdPause).filter(
                HouseholdPause.household_id == household_id,
                HouseholdPause.start_date <= today,
                HouseholdPause.end_date >= last_date + timedelta(days=1),
            ).first()
            gap_covered_by_pause = pause is not None

        if last_date == today - timedelta(days=1) or gap_covered_by_pause:
            user.current_streak += 1
        else:
            user.current_streak = 1
    else:
        user.current_streak = 1

    if user.current_streak > user.longest_streak:
        user.longest_streak = user.current_streak

    user.last_active_date = datetime.now(timezone.utc)
    db.commit()
