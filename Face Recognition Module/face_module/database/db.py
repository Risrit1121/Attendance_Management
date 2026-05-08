import os
import certifi
import numpy as np
import redis
import pickle
from pymongo import MongoClient
from dotenv import load_dotenv

# Load env
load_dotenv()
MONGO_URI = os.getenv("MONGO_URI", "mongodb://localhost:27017/")
REDIS_URI = os.getenv("REDIS_URI", None)

MAX_EMBEDDINGS = 10
EXPECTED_DIM = 512  # MobileFaceNet/buffalo_sc embedding size

client = None
db = None
collection = None
redis_client = None

# ---------------- CONNECTION ----------------
# 1. Isolate MongoDB Connection
try:
    if "mongodb+srv" in MONGO_URI:
        client = MongoClient(MONGO_URI, tlsCAFile=certifi.where(), serverSelectionTimeoutMS=5000)
    else:
        client = MongoClient(MONGO_URI, serverSelectionTimeoutMS=5000)

    client.admin.command('ping')
    print("✅ Connected to MongoDB")

    db = client["face_db"]
    collection = db["users"]
    collection.create_index("user_id", unique=True)

except Exception as e:
    print("❌ MongoDB Connection Failed:", e)

# 2. Isolate Redis Connection (App won't crash if Redis is down)
try:
    if REDIS_URI:
        redis_client = redis.from_url(REDIS_URI)
        redis_client.ping()
        print("✅ Connected to Redis Cache")
except Exception as e:
    print("⚠️ Redis unavailable, running without cache:", e)
    redis_client = None


# ---------------- SAVE ----------------
def save_user_embedding(user_id, embeddings):
    if collection is None:
        print("DB not connected")
        return False

    # FIX: Prevent Numpy ValueError crash
    if embeddings is None or len(embeddings) == 0:
        return False

    new_embeddings = []

    # 🔥 Safe conversion and dimension validation
    for emb in embeddings:
        if emb is None:
            continue

        try:
            if isinstance(emb, np.ndarray):
                # FIX: Validate dimension before conversion
                if emb.ndim != 1 or emb.shape[0] != EXPECTED_DIM:
                    continue
                emb = emb.astype(np.float32).tolist()
            else:
                emb = list(emb)
                if len(emb) != EXPECTED_DIM:
                    continue

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

    # 🔥 Invalidate the Cache
    if redis_client:
        try:
            redis_client.delete(f"user_cache:{user_id}")
        except Exception:
            pass

    return True


# ---------------- GET ----------------
def get_user_embeddings(user_id):
    if collection is None:
        return []

    # 1. Try fetching from Redis Cache first
    if redis_client:
        try:
            cached_data = redis_client.get(f"user_cache:{user_id}")
            if cached_data:
                return pickle.loads(cached_data)
        except Exception as e:
            print(f"Redis fetch error: {e}")

    # 2. If not in cache (or cache failed), fetch from MongoDB
    user = collection.find_one({"user_id": user_id})

    if not user:
        return []

    embeddings = user.get("embeddings", [])
    clean_embeddings = []

    for e in embeddings:
        try:
            arr = np.array(e, dtype=np.float32)

            # FIX: Validate correct shape for verification
            if arr.ndim == 1 and arr.shape[0] == EXPECTED_DIM:
                clean_embeddings.append(arr)

        except Exception:
            continue

    # 3. Save the clean embeddings to Redis for next time
    if redis_client and clean_embeddings:
        try:
            redis_client.setex(
                f"user_cache:{user_id}", 
                86400, 
                pickle.dumps(clean_embeddings)
            )
        except Exception as e:
            print(f"Redis save error: {e}")

    return clean_embeddings