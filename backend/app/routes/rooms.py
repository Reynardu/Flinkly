from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import get_current_user
from app.models.household import HouseholdMember
from app.models.room import Room
from app.models.user import User
from app.schemas.room import RoomCreate, RoomUpdate, RoomResponse

router = APIRouter()

ROOM_SUGGESTIONS = [
    {"name": "Küche", "icon": "kitchen", "color": "#FF5722"},
    {"name": "Bad", "icon": "bathroom", "color": "#2196F3"},
    {"name": "Wohnzimmer", "icon": "living", "color": "#4CAF50"},
    {"name": "Schlafzimmer", "icon": "bed", "color": "#9C27B0"},
    {"name": "Flur", "icon": "door", "color": "#FF9800"},
    {"name": "Keller", "icon": "storage", "color": "#795548"},
    {"name": "Garten", "icon": "yard", "color": "#8BC34A"},
]


@router.get("/suggestions")
def get_room_suggestions():
    return ROOM_SUGGESTIONS


@router.get("/household/{household_id}", response_model=list[RoomResponse])
def list_rooms(
    household_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    _require_member(db, household_id, current_user.id)
    rooms = db.query(Room).filter(Room.household_id == household_id).all()
    result = []
    for room in rooms:
        r = RoomResponse.model_validate(room)
        r.task_count = len(room.tasks)
        r.open_task_count = sum(1 for t in room.tasks if not t.is_suggestion)
        result.append(r)
    return result


@router.post("/household/{household_id}", response_model=RoomResponse, status_code=status.HTTP_201_CREATED)
def create_room(
    household_id: int,
    data: RoomCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    _require_member(db, household_id, current_user.id)
    room = Room(household_id=household_id, **data.model_dump())
    db.add(room)
    db.commit()
    db.refresh(room)
    return room


@router.put("/{room_id}", response_model=RoomResponse)
def update_room(
    room_id: int,
    data: RoomUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    room = _get_room_with_access(db, room_id, current_user.id)
    for field, value in data.model_dump(exclude_none=True).items():
        setattr(room, field, value)
    db.commit()
    db.refresh(room)
    return room


@router.delete("/{room_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_room(
    room_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    room = _get_room_with_access(db, room_id, current_user.id)
    db.delete(room)
    db.commit()


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
