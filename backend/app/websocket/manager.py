import json
from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Query
from collections import defaultdict

from app.services.auth_service import decode_access_token

router = APIRouter()


class WebSocketManager:
    def __init__(self):
        self._connections: dict[int, list[tuple[WebSocket, int]]] = defaultdict(list)

    async def connect(self, websocket: WebSocket, household_id: int, user_id: int):
        await websocket.accept()
        self._connections[household_id].append((websocket, user_id))

    def disconnect(self, websocket: WebSocket, household_id: int):
        self._connections[household_id] = [
            (ws, uid) for ws, uid in self._connections[household_id] if ws != websocket
        ]

    async def broadcast_to_household(self, household_id: int, event: dict):
        dead = []
        for ws, _ in self._connections.get(household_id, []):
            try:
                await ws.send_text(json.dumps(event))
            except Exception:
                dead.append(ws)
        for ws in dead:
            self.disconnect(ws, household_id)

    async def send_to_user(self, household_id: int, target_user_id: int, event: dict):
        for ws, user_id in self._connections.get(household_id, []):
            if user_id == target_user_id:
                try:
                    await ws.send_text(json.dumps(event))
                except Exception:
                    self.disconnect(ws, household_id)


ws_manager = WebSocketManager()


@router.websocket("/ws/{household_id}")
async def websocket_endpoint(
    websocket: WebSocket,
    household_id: int,
    token: str = Query(..., description="JWT Token"),
):
    try:
        user_id = decode_access_token(token)
    except Exception:
        await websocket.close(code=4001)
        return

    from app.database import SessionLocal
    from app.models.user import User
    from app.models.household import HouseholdMember

    db = SessionLocal()
    try:
        user = db.get(User, user_id)
        if not user:
            await websocket.close(code=4002)
            return

        member = db.query(HouseholdMember).filter(
            HouseholdMember.household_id == household_id,
            HouseholdMember.user_id == user_id,
        ).first()
        if not member:
            await websocket.close(code=4003)
            return
    finally:
        db.close()

    await ws_manager.connect(websocket, household_id, user_id)
    await ws_manager.broadcast_to_household(household_id, {
        "type": "MEMBER_ONLINE",
        "user_id": user_id,
    })

    try:
        while True:
            await websocket.receive_text()
    except WebSocketDisconnect:
        ws_manager.disconnect(websocket, household_id)
        await ws_manager.broadcast_to_household(household_id, {
            "type": "MEMBER_OFFLINE",
            "user_id": user_id,
        })
