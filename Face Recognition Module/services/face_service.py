import numpy as np
import base64
import cv2

from face_module.embedding.mobilefacenet_embedder import MobileFaceNet
from face_module.verification.verify_user import Verifier
from face_module.database.db import get_user_embeddings, save_user_embedding


embedder = MobileFaceNet()
verifier = Verifier()


def decode_frame(frame_b64: str):
    img_bytes = base64.b64decode(frame_b64)
    arr = np.frombuffer(img_bytes, np.uint8)
    img = cv2.imdecode(arr, cv2.IMREAD_COLOR)

    if img is None:
        return None

    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    return img


# ---------------- VERIFY ----------------
def verify_face_service(user_id: str, frames: list):
    embeddings = []

    for frame in frames:
        img = decode_frame(frame)
        if img is None:
            continue

        emb = embedder.get_embedding(img)
        if emb is not None:
            embeddings.append(emb)

    if len(embeddings) == 0:
        return {"status": "no_face"}

    final_embedding = np.mean(embeddings, axis=0)

    stored_embeddings = get_user_embeddings(user_id)
    stored_embeddings = [np.array(e) for e in stored_embeddings]

    similarity = verifier.verify(final_embedding, stored_embeddings)

    if similarity > 0.50:
        return {"status": "verified", "similarity": float(similarity)}

    return {"status": "rejected", "similarity": float(similarity)}


# ---------------- ENROLL ----------------
def enroll_face_service(user_id: str, frames: list):
    embeddings = []

    for frame in frames:
        img = decode_frame(frame)
        if img is None:
            continue

        emb = embedder.get_embedding(img)
        if emb is not None:
            embeddings.append(emb)

    if len(embeddings) == 0:
        return {"status": "no_face"}

    final_embedding = np.mean(embeddings, axis=0)

    # normalize
    final_embedding = final_embedding / np.linalg.norm(final_embedding)

    save_user_embedding(user_id, final_embedding)

    return {
        "status": "enrolled",
        "embedding_dim": len(final_embedding)
    }