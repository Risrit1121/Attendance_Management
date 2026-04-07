from fastapi import APIRouter
from services.face_service import verify_face_service

router = APIRouter()

@router.post("/")
def verify(data: dict):
    return verify_face_service(
        user_id=data.get("user_id"),
        frames=data.get("frames", []),
        challenges=data.get("challenges", []) # Get the challenges from frontend
    )