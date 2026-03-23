from pydantic import BaseModel

class EnrollRequest(BaseModel):
    user_id: str
    image_base64: str

class VerifyRequest(BaseModel):
    user_id: str
    image_base64: str


class APIResponse(BaseModel):
    success: bool
    message: str
    data: dict | None = None