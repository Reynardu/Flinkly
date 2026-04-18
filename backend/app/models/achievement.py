from datetime import datetime
from sqlalchemy import String, Integer, DateTime, ForeignKey, func, Enum
from sqlalchemy.orm import Mapped, mapped_column, relationship
import enum

from app.database import Base


class AchievementType(str, enum.Enum):
    FIRST_TASK = "FIRST_TASK"
    STREAK_7 = "STREAK_7"
    STREAK_30 = "STREAK_30"
    SCORE_100 = "SCORE_100"
    SCORE_1000 = "SCORE_1000"
    FIVE_IN_ONE_DAY = "FIVE_IN_ONE_DAY"
    MONTHLY_WINNER = "MONTHLY_WINNER"
    TEAMPLAYER = "TEAMPLAYER"
    LEVEL_UP = "LEVEL_UP"


class Achievement(Base):
    __tablename__ = "achievements"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    type: Mapped[AchievementType] = mapped_column(Enum(AchievementType), nullable=False)
    title: Mapped[str] = mapped_column(String(100), nullable=False)
    description: Mapped[str] = mapped_column(String(300), nullable=False)
    icon: Mapped[str] = mapped_column(String(10), nullable=False)
    earned_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())

    user: Mapped["User"] = relationship(back_populates="achievements")
