import os
import numpy as np
from pymongo import MongoClient
from dotenv import load_dotenv

# Load environment variables from a .env file
load_dotenv()

# Fetch the MongoDB URI from the environment, fallback to localhost if not found
MONGO_URI = os.getenv("MONGO_URI", "mongodb://localhost:27017/")

# Mongo Connection
try:
    client = MongoClient(MONGO_URI)
    # Ping the server to verify connection
    client.admin.command('ping')
    print("Successfully connected to MongoDB!")
except Exception as e:
    print(f"Error connecting to MongoDB: {e}")

db = client["face_db"]
collection = db["users"]

# Create a unique index on user_id to guarantee instant lookups
collection.create_index("user_id", unique=True)

# ⚙️ config
MAX_EMBEDDINGS = 10

# 💾 SAVE (Enroll / Update)
def save_user_embedding(user_id, embeddings):
    """
    embeddings: numpy array (or list of numpy arrays)
    """
    # If a single embedding is passed as a numpy array, wrap it in a list
    if isinstance(embeddings, np.ndarray):
        new_embeddings = [embeddings.tolist()]
    else:
        # convert list of numpy arrays to lists
        new_embeddings = [emb.tolist() for emb in embeddings]

    user = collection.find_one({"user_id": user_id})

    if user:
        existing_embeddings = user.get("embeddings", [])
        # append new ones
        existing_embeddings.extend(new_embeddings)
        # ✂️ limit size (important)
        existing_embeddings = existing_embeddings[-MAX_EMBEDDINGS:]

        collection.update_one(
            {"user_id": user_id},
            {"$set": {"embeddings": existing_embeddings}}
        )
    else:
        collection.insert_one({
            "user_id": user_id,
            "embeddings": new_embeddings
        })

    return True

# 🔍 GET embeddings
def get_user_embeddings(user_id):
    user = collection.find_one({"user_id": user_id})

    if not user:
        return []

    embeddings = user.get("embeddings", [])
    # convert back to numpy arrays
    embeddings = [np.array(e) for e in embeddings]

    return embeddings