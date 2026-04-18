from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routes import auth, household, rooms, tasks, scores, achievements
from app.websocket.manager import router as ws_router

app = FastAPI(
    title="Flinkly API",
    description="Backend für Flinkly – die spielerische Haushalts-App",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router, prefix="/auth", tags=["Auth"])
app.include_router(household.router, prefix="/household", tags=["Household"])
app.include_router(rooms.router, prefix="/rooms", tags=["Rooms"])
app.include_router(tasks.router, prefix="/tasks", tags=["Tasks"])
app.include_router(scores.router, prefix="/scores", tags=["Scores"])
app.include_router(achievements.router, prefix="/achievements", tags=["Achievements"])
app.include_router(ws_router, tags=["WebSocket"])


@app.get("/health")
def health_check():
    return {"status": "ok"}
