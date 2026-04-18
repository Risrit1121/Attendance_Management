import numpy as np
import base64
import cv2
import concurrent.futures

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
    if not frames:
        return {
            "status": "invalid_payload", 
            "message": "Expected at least 1 frame"
        }

    # Decode all frames
    valid_imgs = []
    for f in frames:
        img = decode_frame(f)
        if img is not None:
            valid_imgs.append(img)

    if not valid_imgs:
        return {"status": "invalid_frames"}

    embeddings = []
    
    # Process all frames in parallel. 
    # This acts as a failsafe; you can pass 1, 3, or 5 frames.
    with concurrent.futures.ThreadPoolExecutor(max_workers=len(valid_imgs)) as executor:
        # We only need the embedding now, not the keypoints
        results = executor.map(embedder.get_embedding, valid_imgs)
        
        for emb in results:
            if emb is not None:
                embeddings.append(emb)

    if not embeddings:
        return {"status": "no_face_detected_in_some_frames"}

    # Database Identity Verification
    stored_embeddings = get_user_embeddings(user_id)
    if not stored_embeddings:
        return {"status": "user_not_found"}

    # Compare all valid frames against the DB and find the best match
    highest_similarity = 0.0
    for candidate_emb in embeddings:
        similarity = verifier.verify(candidate_emb, stored_embeddings)
        if similarity > highest_similarity:
            highest_similarity = similarity
    print(highest_similarity)
    if highest_similarity > 0.50:
        return {"status": "verified", "similarity": float(highest_similarity)}

    return {"status": "rejected", "similarity": float(highest_similarity)}


# ---------------- ENROLL ----------------
def enroll_face_service(user_id: str, frames: list):
    # Decode all frames upfront
    imgs = [decode_frame(frame) for frame in frames]
    valid_imgs = [img for img in imgs if img is not None]

    if not valid_imgs:
        return {"status": "no_face"}

    embeddings = []
    
    # Process the entire batch of enrollment frames in parallel
    with concurrent.futures.ThreadPoolExecutor() as executor:
        results = executor.map(embedder.get_embedding, valid_imgs)
        
        for emb in results:
            if emb is not None:
                embeddings.append(emb)

    if len(embeddings) == 0:
        return {"status": "no_face"}

    final_embedding = np.mean(embeddings, axis=0)

    # Normalize
    final_embedding = final_embedding / np.linalg.norm(final_embedding)
    normalized_embeddings = [
    emb / np.linalg.norm(emb) 
    for emb in embeddings 
    if np.linalg.norm(emb) != 0
]

    # save_user_embedding(user_id, final_embedding)
    save_user_embedding(user_id, normalized_embeddings)

    return {
        "status": "enrolled",
        "embedding_dim": len(final_embedding)
    }