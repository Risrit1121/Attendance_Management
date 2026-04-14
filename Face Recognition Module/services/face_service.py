import numpy as np
import base64
import cv2
import concurrent.futures

from face_module.embedding.mobilefacenet_embedder import MobileFaceNet
from face_module.verification.verify_user import Verifier
from face_module.database.db import get_user_embeddings, save_user_embedding
from face_module.liveness.head_pose import HeadPose

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
    
    detected_pose = head_pose.detect_turn(nose, left_eye, right_eye)
    
    if expected_challenge == "turn_left" and detected_pose == "left":
        return True
    if expected_challenge == "turn_right" and detected_pose == "right":
        return True
        
    return False

# ---------------- VERIFY ----------------
def verify_face_service(user_id: str, frames: list, challenges: list):
    if len(frames) != 3 or len(challenges) != 2:
        return {
            "status": "invalid_payload", 
            "message": "Expected exactly 3 frames and 2 challenges"
        }

    # Decode all frames
    img1 = decode_frame(frames[0]) 
    img2 = decode_frame(frames[1]) 
    img3 = decode_frame(frames[2]) 

    if img1 is None or img2 is None or img3 is None:
        return {"status": "invalid_frames"}

    # Process all 3 frames perfectly in parallel using thread pooling
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as executor:
        future1 = executor.submit(embedder.get_embedding_and_kps, img1)
        future2 = executor.submit(embedder.get_embedding_and_kps, img2)
        future3 = executor.submit(embedder.get_embedding_and_kps, img3)

        emb1, kps1 = future1.result()
        emb2, kps2 = future2.result()
        emb3, kps3 = future3.result()

    if emb1 is None or emb2 is None or emb3 is None:
        return {"status": "no_face_detected_in_some_frames"}

    # Evaluate Liveness Challenges
    if not evaluate_challenge(challenges[1], kps3):
        return {"status": "liveness_failed", "message": f"Failed to perform: {challenges[1]}"}

    # Database Identity Verification
    stored_embeddings = get_user_embeddings(user_id)
    if not stored_embeddings:
        return {"status": "user_not_found"}

    similarity = verifier.verify(emb1, stored_embeddings)

    if similarity > 0.50:
        return {"status": "verified", "similarity": float(similarity)}

    return {"status": "rejected", "similarity": float(similarity)}


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

    save_user_embedding(user_id, final_embedding)

    return {
        "status": "enrolled",
        "embedding_dim": len(final_embedding)
    }