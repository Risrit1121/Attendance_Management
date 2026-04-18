import os
import certifi
import numpy as np
from pymongo import MongoClient
from dotenv import load_dotenv

# Load env
load_dotenv()
MONGO_URI = os.getenv("MONGO_URI", "mongodb://localhost:27017/")

MAX_EMBEDDINGS = 10

client = None
db = None
collection = None

# ---------------- CONNECTION ----------------
try:
    if "mongodb+srv" in MONGO_URI:
        client = MongoClient(MONGO_URI, tlsCAFile=certifi.where())
    else:
        client = MongoClient(MONGO_URI)

    client.admin.command('ping')
    print("✅ Connected to MongoDB")

    db = client["face_db"]
    collection = db["users"]

    collection.create_index("user_id", unique=True)

except Exception as e:
    print("❌ MongoDB Connection Failed:", e)


# ---------------- SAVE ----------------
def save_user_embedding(user_id, embeddings):
    if collection is None:
        print("DB not connected")
        return False

    if not embeddings:
        return False

    new_embeddings = []

    # 🔥 Safe conversion
    for emb in embeddings:
        if emb is None:
            continue

        try:
            if isinstance(emb, np.ndarray):
                emb = emb.astype(np.float32).tolist()
            else:
                emb = list(emb)

            # Ensure 1D vector
            if isinstance(emb, list) and len(emb) > 0:
                new_embeddings.append(emb)

        except Exception:
            continue

    if not new_embeddings:
        return False

    # 🔥 Upsert (atomic + safe)
    collection.update_one(
        {"user_id": user_id},
        {
            "$push": {
                "embeddings": {
                    "$each": new_embeddings,
                    "$slice": -MAX_EMBEDDINGS  # keep last N only
                }
            }
        },
        upsert=True
    )

    return True


# ---------------- GET ----------------
def get_user_embeddings(user_id):
    if collection is None:
        return []

    user = collection.find_one({"user_id": user_id})

    if not user:
        return []

    embeddings = user.get("embeddings", [])

    clean_embeddings = []

    for e in embeddings:
        try:
            arr = np.array(e, dtype=np.float32)

            # ensure correct shape
            if arr.ndim == 1:
                clean_embeddings.append(arr)

        except Exception:
            continue

    return clean_embeddings