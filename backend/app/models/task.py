from datetime import datetime
from sqlalchemy import String, Integer, DateTime, ForeignKey, func, Enum, Boolean, Float
from sqlalchemy.orm import Mapped, mapped_column, relationship
import enum

from app.database import Base


class FrequencyType(str, enum.Enum):
    DAILY = "DAILY"
    WEEKLY = "WEEKLY"
    MONTHLY = "MONTHLY"
    CUSTOM = "CUSTOM"
    ONCE = "ONCE"


class Task(Base):
    __tablename__ = "tasks"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    room_id: Mapped[int] = mapped_column(Integer, ForeignKey("rooms.id", ondelete="CASCADE"), nullable=False)
    title: Mapped[str] = mapped_column(String(200), nullable=False)
    description: Mapped[str | None] = mapped_column(String(1000))
    difficulty: Mapped[int] = mapped_column(Integer, default=2)  # 1-5
    frequency_type: Mapped[FrequencyType] = mapped_column(Enum(FrequencyType), default=FrequencyType.WEEKLY)
    frequency_value: Mapped[str | None] = mapped_column(String(100))  # z.B. "MON,FRI" oder "1" (Tag im Monat)
    due_date: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    auto_repeat: Mapped[bool] = mapped_column(Boolean, default=True)
    points: Mapped[int] = mapped_column(Integer, nullable=False)
    photo_url: Mapped[str | None] = mapped_column(String(500))
    is_suggestion: Mapped[bool] = mapped_column(Boolean, default=False)
    assigned_to_user_id: Mapped[int | None] = mapped_column(Integer, ForeignKey("users.id", ondelete="SET NULL"))
    # Optimistic locking: verhindert Doppel-Erledigung
    claimed_by_user_id: Mapped[int | None] = mapped_column(Integer, ForeignKey("users.id", ondelete="SET NULL"))
    claimed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    next_due_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())

    room: Mapped["Room"] = relationship(back_populates="tasks")
    completions: Mapped[list["TaskCompletion"]] = relationship(back_populates="task", cascade="all, delete-orphan")


class TaskCompletion(Base):
    __tablename__ = "task_completions"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    task_id: Mapped[int] = mapped_column(Integer, ForeignKey("tasks.id", ondelete="CASCADE"), nullable=False)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    completed_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    points_earned: Mapped[int] = mapped_column(Integer, nullable=False)
    photo_url: Mapped[str | None] = mapped_column(String(500))
    note: Mapped[str | None] = mapped_column(String(500))

    task: Mapped["Task"] = relationship(back_populates="completions")
    user: Mapped["User"] = relationship(back_populates="completions")
