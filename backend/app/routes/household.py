import secrets
from datetime import date, datetime, timezone
from fastapi import APIRouter, Depends, HTTPException, status, Request
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import get_current_user, require_household_owner
from app.models.household import Household, HouseholdMember, HouseholdPause, MemberRole
from app.models.user import User
from app.schemas.household import HouseholdCreate, HouseholdUpdate, HouseholdResponse, InviteLinkResponse, PauseCreate, PauseResponse

router = APIRouter()


@router.post("", response_model=HouseholdResponse, status_code=status.HTTP_201_CREATED)
def create_household(
    data: HouseholdCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    household = Household(name=data.name)
    db.add(household)
    db.flush()

    member = HouseholdMember(
        household_id=household.id,
        user_id=current_user.id,
        role=MemberRole.OWNER,
    )
    db.add(member)
    db.commit()
    db.refresh(household)
    return household


@router.get("/{household_id}", response_model=HouseholdResponse)
def get_household(
    household_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    _require_member(db, household_id, current_user.id)
    household = db.get(Household, household_id)
    if not household:
        raise HTTPException(status_code=404, detail="Haushalt nicht gefunden")
    return household


@router.put("/{household_id}", response_model=HouseholdResponse)
def update_household(
    household_id: int,
    data: HouseholdUpdate,
    _: HouseholdMember = Depends(require_household_owner),
    db: Session = Depends(get_db),
):
    household = db.get(Household, household_id)
    household.name = data.name
    db.commit()
    db.refresh(household)
    return household


@router.post("/{household_id}/invite", response_model=InviteLinkResponse)
def create_invite_link(
    household_id: int,
    request: Request,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    _require_member(db, household_id, current_user.id)
    token = secrets.token_urlsafe(16)
    member = db.query(HouseholdMember).filter(
        HouseholdMember.household_id == household_id,
        HouseholdMember.user_id == current_user.id,
    ).first()
    member.invite_token = token
    db.commit()

    base_url = str(request.base_url).rstrip("/")
    return InviteLinkResponse(
        invite_url=f"{base_url}/household/join/{token}",
        token=token,
    )


@router.post("/join/{token}", response_model=HouseholdResponse)
def join_household(
    token: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    invite = db.query(HouseholdMember).filter(HouseholdMember.invite_token == token).first()
    if not invite:
        raise HTTPException(status_code=404, detail="Einladungslink ungültig oder abgelaufen")

    already_member = db.query(HouseholdMember).filter(
        HouseholdMember.household_id == invite.household_id,
        HouseholdMember.user_id == current_user.id,
    ).first()
    if already_member:
        return db.get(Household, invite.household_id)

    new_member = HouseholdMember(
        household_id=invite.household_id,
        user_id=current_user.id,
        role=MemberRole.MEMBER,
    )
    db.add(new_member)
    db.commit()

    household = db.get(Household, invite.household_id)
    return household


@router.delete("/{household_id}/members/{user_id}", status_code=status.HTTP_204_NO_CONTENT)
def remove_member(
    household_id: int,
    user_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    member = _require_member(db, household_id, current_user.id)
    if current_user.id != user_id and member.role != MemberRole.OWNER:
        raise HTTPException(status_code=403, detail="Nur der Owner kann andere Mitglieder entfernen")

    target = db.query(HouseholdMember).filter(
        HouseholdMember.household_id == household_id,
        HouseholdMember.user_id == user_id,
    ).first()
    if target:
        db.delete(target)
        db.commit()


@router.get("/{household_id}/pauses", response_model=list[PauseResponse])
def list_pauses(
    household_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    _require_member(db, household_id, current_user.id)
    return db.query(HouseholdPause).filter(
        HouseholdPause.household_id == household_id,
    ).order_by(HouseholdPause.start_date.desc()).all()


@router.get("/{household_id}/pauses/active", response_model=PauseResponse | None)
def get_active_pause(
    household_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    _require_member(db, household_id, current_user.id)
    today = datetime.now(timezone.utc).date()
    return db.query(HouseholdPause).filter(
        HouseholdPause.household_id == household_id,
        HouseholdPause.start_date <= today,
        HouseholdPause.end_date >= today,
    ).first()


@router.post("/{household_id}/pauses", response_model=PauseResponse, status_code=status.HTTP_201_CREATED)
def create_pause(
    household_id: int,
    data: PauseCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    _require_member(db, household_id, current_user.id)
    pause = HouseholdPause(
        household_id=household_id,
        start_date=data.start_date,
        end_date=data.end_date,
        reason=data.reason,
        created_by_user_id=current_user.id,
    )
    db.add(pause)
    db.commit()
    db.refresh(pause)
    return pause


@router.delete("/{household_id}/pauses/{pause_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_pause(
    household_id: int,
    pause_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    _require_member(db, household_id, current_user.id)
    pause = db.query(HouseholdPause).filter(
        HouseholdPause.id == pause_id,
        HouseholdPause.household_id == household_id,
    ).first()
    if not pause:
        raise HTTPException(status_code=404, detail="Pause nicht gefunden")
    db.delete(pause)
    db.commit()


def _require_member(db: Session, household_id: int, user_id: int) -> HouseholdMember:
    member = db.query(HouseholdMember).filter(
        HouseholdMember.household_id == household_id,
        HouseholdMember.user_id == user_id,
    ).first()
    if not member:
        raise HTTPException(status_code=403, detail="Kein Zugriff auf diesen Haushalt")
    return member
