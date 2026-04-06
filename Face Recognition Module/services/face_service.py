import numpy as np
import base64
import cv2

from face_module.embedding.mobilefacenet_embedder import MobileFaceNet
from face_module.verification.verify_user import Verifier
from face_module.database.db import get_user_embeddings, save_user_embedding
from face_module.liveness.head_pose import HeadPose # Import liveness module

embedder = MobileFaceNet()
verifier = Verifier()
head_pose = HeadPose()

def decode_frame(frame_b64: str):
    img_bytes = base64.b64decode(frame_b64)
    arr = np.frombuffer(img_bytes, np.uint8)
    img = cv2.imdecode(arr, cv2.IMREAD_COLOR)

    if img is None:
        return None

    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    return img

def evaluate_challenge(expected_challenge: str, kps):
    """
    Evaluates if the keypoints match the expected challenge from the frontend.
    InsightFace keypoints order: [0]: left_eye, [1]: right_eye, [2]: nose.
    """
    left_eye = kps[0]
    right_eye = kps[1]
    nose = kps[2]
    
    # Detect the actual turn using your existing HeadPose class
    detected_pose = head_pose.detect_turn(nose, left_eye, right_eye)
    
    # Map frontend challenge strings to your HeadPose return strings ("left", "right")
    if expected_challenge == "turn_left" and detected_pose == "left":
        return True
    if expected_challenge == "turn_right" and detected_pose == "right":
        return True
        
    return False

# ---------------- VERIFY ----------------
def verify_face_service(user_id: str, frames: list, challenges: list):
    # 1. Validate Payload
    if len(frames) != 3 or len(challenges) != 2:
        return {
            "status": "invalid_payload", 
            "message": "Expected exactly 3 frames and 2 challenges"
        }

    # 2. Decode Frames
    img1 = decode_frame(frames[0]) # Base frame
    img2 = decode_frame(frames[1]) # Challenge 1 frame
    img3 = decode_frame(frames[2]) # Challenge 2 frame

    if img1 is None or img2 is None or img3 is None:
        return {"status": "invalid_frames"}

    # 3. Get Embeddings and Keypoints
    emb1, kps1 = embedder.get_embedding_and_kps(img1)
    emb2, kps2 = embedder.get_embedding_and_kps(img2)
    emb3, kps3 = embedder.get_embedding_and_kps(img3)

    if emb1 is None or emb2 is None or emb3 is None:
        return {"status": "no_face_detected_in_some_frames"}

    # # 4. Anti-Spoofing (Intra-Face Verification)
    # # Ensure the user didn't show a static photo for Frame 1, and someone else for Frame 2/3
    # if verifier.cosine_similarity(emb1, emb2) < 0.60 or verifier.cosine_similarity(emb1, emb3) < 0.60:
    #      return {"status": "liveness_failed", "message": "Face switched during challenges"}

    # # 5. Evaluate Liveness Challenges
    # if not evaluate_challenge(challenges[0], kps2):
    #     return {"status": "liveness_failed", "message": f"Failed to perform: {challenges[0]}"}
        
    if not evaluate_challenge(challenges[1], kps3):
        return {"status": "liveness_failed", "message": f"Failed to perform: {challenges[1]}"}

    # 6. Database Identity Verification (using Frame 1)
    stored_embeddings = get_user_embeddings(user_id)
    if not stored_embeddings:
        return {"status": "user_not_found"}

    stored_embeddings = [np.array(e) for e in stored_embeddings]
    similarity = verifier.verify(emb1, stored_embeddings)

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