"""initial schema

Revision ID: 000
Revises:
Create Date: 2026-04-18
"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects.postgresql import ENUM as PgEnum

revision = "000"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "users",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("user_secret", sa.String(64), unique=True, nullable=False, index=True),
        sa.Column("display_name", sa.String(100), nullable=False),
        sa.Column("daily_point_goal", sa.Integer(), server_default="50"),
        sa.Column("total_points", sa.Integer(), server_default="0"),
        sa.Column("current_streak", sa.Integer(), server_default="0"),
        sa.Column("longest_streak", sa.Integer(), server_default="0"),
        sa.Column("last_active_date", sa.DateTime(timezone=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
    )

    op.create_table(
        "households",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("name", sa.String(100), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
    )

    op.execute("CREATE TYPE memberrole AS ENUM ('OWNER', 'MEMBER')")
    op.create_table(
        "household_members",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("household_id", sa.Integer(), sa.ForeignKey("households.id", ondelete="CASCADE"), nullable=False),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("role", PgEnum("OWNER", "MEMBER", name="memberrole", create_type=False), nullable=False, server_default="MEMBER"),
        sa.Column("invite_token", sa.String(64), unique=True, index=True, nullable=True),
        sa.Column("joined_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
    )

    op.create_table(
        "rooms",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("household_id", sa.Integer(), sa.ForeignKey("households.id", ondelete="CASCADE"), nullable=False),
        sa.Column("name", sa.String(100), nullable=False),
        sa.Column("icon", sa.String(50), server_default="home"),
        sa.Column("color", sa.String(7), server_default="#4CAF50"),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
    )

    op.execute("CREATE TYPE frequencytype AS ENUM ('DAILY', 'WEEKLY', 'MONTHLY', 'CUSTOM', 'ONCE')")
    op.create_table(
        "tasks",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("room_id", sa.Integer(), sa.ForeignKey("rooms.id", ondelete="CASCADE"), nullable=False),
        sa.Column("title", sa.String(200), nullable=False),
        sa.Column("description", sa.String(1000), nullable=True),
        sa.Column("difficulty", sa.Integer(), server_default="2"),
        sa.Column("frequency_type", PgEnum("DAILY", "WEEKLY", "MONTHLY", "CUSTOM", "ONCE", name="frequencytype", create_type=False), server_default="WEEKLY"),
        sa.Column("frequency_value", sa.String(100), nullable=True),
        sa.Column("due_date", sa.DateTime(timezone=True), nullable=True),
        sa.Column("auto_repeat", sa.Boolean(), server_default="true"),
        sa.Column("points", sa.Integer(), nullable=False),
        sa.Column("photo_url", sa.String(500), nullable=True),
        sa.Column("is_suggestion", sa.Boolean(), server_default="false"),
        sa.Column("assigned_to_user_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="SET NULL"), nullable=True),
        sa.Column("claimed_by_user_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="SET NULL"), nullable=True),
        sa.Column("claimed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("next_due_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
    )

    op.create_table(
        "task_completions",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("task_id", sa.Integer(), sa.ForeignKey("tasks.id", ondelete="CASCADE"), nullable=False),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("completed_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
        sa.Column("points_earned", sa.Integer(), nullable=False),
        sa.Column("photo_url", sa.String(500), nullable=True),
        sa.Column("note", sa.String(500), nullable=True),
    )

    op.execute(
        "CREATE TYPE achievementtype AS ENUM ("
        "'FIRST_TASK', 'STREAK_7', 'STREAK_30', 'SCORE_100', 'SCORE_1000',"
        "'FIVE_IN_ONE_DAY', 'MONTHLY_WINNER', 'TEAMPLAYER', 'LEVEL_UP')"
    )
    op.create_table(
        "achievements",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("type", PgEnum(
            "FIRST_TASK", "STREAK_7", "STREAK_30", "SCORE_100", "SCORE_1000",
            "FIVE_IN_ONE_DAY", "MONTHLY_WINNER", "TEAMPLAYER", "LEVEL_UP",
            name="achievementtype", create_type=False,
        ), nullable=False),
        sa.Column("title", sa.String(100), nullable=False),
        sa.Column("description", sa.String(300), nullable=False),
        sa.Column("icon", sa.String(10), nullable=False),
        sa.Column("earned_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
    )


def downgrade() -> None:
    op.drop_table("achievements")
    op.execute("DROP TYPE IF EXISTS achievementtype")
    op.drop_table("task_completions")
    op.drop_table("tasks")
    op.execute("DROP TYPE IF EXISTS frequencytype")
    op.drop_table("rooms")
    op.drop_table("household_members")
    op.execute("DROP TYPE IF EXISTS memberrole")
    op.drop_table("households")
    op.drop_table("users")
