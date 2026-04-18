from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException, status, BackgroundTasks
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import get_current_user
from app.models.household import HouseholdMember
from app.models.room import Room
from app.models.task import Task, TaskCompletion
from app.models.user import User
from app.schemas.task import TaskCreate, TaskUpdate, TaskResponse, CompletionCreate, CompletionResponse, TaskSuggestion
from app.services.achievement_service import check_and_award_achievements, update_streak
from app.services.notification_service import notify_household_members
from app.services.points_service import calculate_points, calculate_next_due
from app.websocket.manager import ws_manager

router = APIRouter()

TASK_TEMPLATES = {
    "Küche": [
        ("Geschirrspüler ausräumen", 1, "DAILY"),
        ("Herd reinigen", 3, "WEEKLY"),
        ("Kühlschrank prüfen", 2, "WEEKLY"),
        ("Arbeitsplatte wischen", 1, "DAILY"),
        ("Kühlschrank ausräumen & reinigen", 4, "MONTHLY"),
    ],
    "Bad": [
        ("WC putzen", 2, "WEEKLY"),
        ("Spiegel wischen", 1, "WEEKLY"),
        ("Handtücher wechseln", 2, "WEEKLY"),
        ("Badewanne/Dusche reinigen", 3, "WEEKLY"),
        ("Boden wischen", 2, "WEEKLY"),
    ],
    "Wohnzimmer": [
        ("Staubsaugen", 2, "WEEKLY"),
        ("Stauben", 2, "WEEKLY"),
        ("Fenster putzen", 4, "MONTHLY"),
        ("Kissen/Decken ordnen", 1, "DAILY"),
    ],
    "Schlafzimmer": [
        ("Betten machen", 1, "DAILY"),
        ("Bettzeug wechseln", 3, "MONTHLY"),
        ("Staubsaugen", 2, "WEEKLY"),
    ],
    "Allgemein": [
        ("Müll rausbringen", 1, "WEEKLY"),
        ("Einkaufen", 2, "WEEKLY"),
        ("Blumen gießen", 1, "DAILY"),
        ("Wäsche waschen", 2, "WEEKLY"),
        ("Wäsche aufhängen/trocknen", 2, "WEEKLY"),
    ],
}


@router.get("/room/{room_id}", response_model=list[TaskResponse])
def list_tasks(
    room_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    room = _get_room_with_access(db, room_id, current_user.id)
    tasks = db.query(Task).filter(Task.room_id == room_id).all()
    result = []
    for task in tasks:
        t = TaskResponse.model_validate(task)
        t.completion_count = len(task.completions)
        result.append(t)
    return result


@router.post("/room/{room_id}", response_model=TaskResponse, status_code=status.HTTP_201_CREATED)
def create_task(
    room_id: int,
    data: TaskCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    room = _get_room_with_access(db, room_id, current_user.id)
    points = calculate_points(data.difficulty, data.frequency_type)
    task = Task(
        room_id=room_id,
        points=points,
        **data.model_dump(),
    )
    db.add(task)
    db.commit()
    db.refresh(task)
    return task


@router.put("/{task_id}", response_model=TaskResponse)
def update_task(
    task_id: int,
    data: TaskUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    task = _get_task_with_access(db, task_id, current_user.id)
    for field, value in data.model_dump(exclude_none=True).items():
        setattr(task, field, value)
    if data.difficulty or data.frequency_type:
        task.points = calculate_points(task.difficulty, task.frequency_type)
    db.commit()
    db.refresh(task)
    return task


@router.delete("/{task_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_task(
    task_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    task = _get_task_with_access(db, task_id, current_user.id)
    db.delete(task)
    db.commit()


@router.post("/{task_id}/complete", response_model=CompletionResponse)
async def complete_task(
    task_id: int,
    data: CompletionCreate,
    background_tasks: BackgroundTasks,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    task = _get_task_with_access(db, task_id, current_user.id)

    # Optimistic Locking: prüfe ob Task in den letzten 30 Sekunden von jemand anderem beansprucht wurde
    now = datetime.now(timezone.utc)
    if (
        task.claimed_by_user_id
        and task.claimed_by_user_id != current_user.id
        and task.claimed_at
        and (now - task.claimed_at).seconds < 30
    ):
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"Aufgabe wird gerade von einem anderen Mitglied erledigt",
        )

    task.claimed_by_user_id = current_user.id
    task.claimed_at = now
    db.flush()

    completion = TaskCompletion(
        task_id=task_id,
        user_id=current_user.id,
        points_earned=task.points,
        photo_url=data.photo_url,
        note=data.note,
    )
    db.add(completion)

    current_user.total_points += task.points
    room = db.get(Room, task.room_id)
    update_streak(db, current_user, household_id=room.household_id)
    task.next_due_at = calculate_next_due(task)
    task.claimed_by_user_id = None
    task.claimed_at = None

    db.commit()
    db.refresh(completion)

    new_achievements = check_and_award_achievements(db, current_user)

    members = db.query(HouseholdMember).filter(
        HouseholdMember.household_id == room.household_id
    ).all()

    event = {
        "type": "TASK_COMPLETED",
        "task_id": task_id,
        "task_title": task.title,
        "user_id": current_user.id,
        "user_name": current_user.display_name,
        "points": task.points,
        "achievements": [a.title for a in new_achievements],
    }
    await ws_manager.broadcast_to_household(room.household_id, event)

    background_tasks.add_task(
        notify_household_members,
        members=members,
        title="Aufgabe erledigt! ✓",
        body=f"{current_user.display_name} hat '{task.title}' erledigt (+{task.points} Punkte)",
        exclude_user_id=current_user.id,
        data={"task_id": str(task_id), "type": "TASK_COMPLETED"},
    )

    return completion


@router.get("/suggestions/{household_id}", response_model=list[TaskSuggestion])
def get_suggestions(
    household_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    _require_member(db, household_id, current_user.id)
    rooms = db.query(Room).filter(Room.household_id == household_id).all()
    suggestions = []

    for room in rooms:
        active_task_count = len([t for t in room.tasks if not t.is_suggestion])
        templates = TASK_TEMPLATES.get(room.name, [])

        if active_task_count < 3 and templates:
            existing_titles = {t.title for t in room.tasks}
            for title, difficulty, freq in templates[:3]:
                if title not in existing_titles and len(suggestions) < 3:
                    from app.models.task import FrequencyType
                    freq_type = FrequencyType(freq)
                    suggestions.append(TaskSuggestion(
                        title=title,
                        description=f"Vorgeschlagene Aufgabe für {room.name}",
                        difficulty=difficulty,
                        frequency_type=freq_type,
                        points=calculate_points(difficulty, freq_type),
                        reason=f"{room.name} hat weniger als 3 aktive Aufgaben",
                    ))

    return suggestions[:3]


def _require_member(db: Session, household_id: int, user_id: int):
    member = db.query(HouseholdMember).filter(
        HouseholdMember.household_id == household_id,
        HouseholdMember.user_id == user_id,
    ).first()
    if not member:
        raise HTTPException(status_code=403, detail="Kein Zugriff auf diesen Haushalt")
    return member


def _get_room_with_access(db: Session, room_id: int, user_id: int) -> Room:
    room = db.get(Room, room_id)
    if not room:
        raise HTTPException(status_code=404, detail="Raum nicht gefunden")
    _require_member(db, room.household_id, user_id)
    return room


def _get_task_with_access(db: Session, task_id: int, user_id: int) -> Task:
    task = db.get(Task, task_id)
    if not task:
        raise HTTPException(status_code=404, detail="Aufgabe nicht gefunden")
    _get_room_with_access(db, task.room_id, user_id)
    return task
