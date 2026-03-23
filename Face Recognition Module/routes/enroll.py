from fastapi import APIRouter
from services.face_service import enroll_face_service

router = APIRouter()

@router.post("/")
def enroll(data: dict):
    return enroll_face_service(
        user_id=data["user_id"],
        frames=data["frames"]
    )