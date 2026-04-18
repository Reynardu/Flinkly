from datetime import datetime, date
from sqlalchemy import String, Integer, DateTime, Date, ForeignKey, func, Enum
from sqlalchemy.orm import Mapped, mapped_column, relationship
import enum

from app.database import Base


class MemberRole(str, enum.Enum):
    OWNER = "OWNER"
    MEMBER = "MEMBER"


class Household(Base):
    __tablename__ = "households"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    name: Mapped[str] = mapped_column(String(100), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())

    members: Mapped[list["HouseholdMember"]] = relationship(back_populates="household", cascade="all, delete-orphan")
    rooms: Mapped[list["Room"]] = relationship(back_populates="household", cascade="all, delete-orphan")
    pauses: Mapped[list["HouseholdPause"]] = relationship(back_populates="household", cascade="all, delete-orphan")


class HouseholdPause(Base):
    __tablename__ = "household_pauses"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    household_id: Mapped[int] = mapped_column(Integer, ForeignKey("households.id", ondelete="CASCADE"), nullable=False)
    start_date: Mapped[date] = mapped_column(Date, nullable=False)
    end_date: Mapped[date] = mapped_column(Date, nullable=False)
    reason: Mapped[str | None] = mapped_column(String(200))
    created_by_user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())

    household: Mapped["Household"] = relationship(back_populates="pauses")


class HouseholdMember(Base):
    __tablename__ = "household_members"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    household_id: Mapped[int] = mapped_column(Integer, ForeignKey("households.id", ondelete="CASCADE"), nullable=False)
    user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    role: Mapped[MemberRole] = mapped_column(Enum(MemberRole), default=MemberRole.MEMBER, nullable=False)
    invite_token: Mapped[str | None] = mapped_column(String(64), unique=True, index=True)
    joined_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())

    household: Mapped["Household"] = relationship(back_populates="members")
    user: Mapped["User"] = relationship(back_populates="household_memberships")
