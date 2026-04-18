from fastapi import FastAPI
from routes import enroll, verify

app = FastAPI()

app.include_router(enroll.router, prefix="/enroll")
app.include_router(verify.router, prefix="/verify")