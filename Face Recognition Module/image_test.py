import ast
import base64
import numpy as np
import cv2
import csv
import os
from flask import Flask, request, jsonify

# InsightFace
from insightface.app import FaceAnalysis

app = Flask(__name__)

# ---------------- INIT MODEL ----------------
face_app = FaceAnalysis(name='buffalo_sc')
face_app.prepare(ctx_id=-1)  # CPU

CSV_FILE = "embeddings.csv"

# ---------------- LIVENESS ----------------
class HeadPose:

    def detect_turn(self, nose, left_eye, right_eye):

        left_dist = np.linalg.norm(nose - left_eye)
        right_dist = np.linalg.norm(nose - right_eye)

        if left_dist > right_dist * 1.4:
            return "left"

        if right_dist > left_dist * 1.4:
            return "right"

        return "center"


head_pose = HeadPose()

def evaluate_challenge(expected_challenge, kps):
    left_eye = kps[0]
    right_eye = kps[1]
    nose = kps[2]

    detected_pose = head_pose.detect_turn(nose, left_eye, right_eye)

    if expected_challenge == "turn_left" and detected_pose == "left":
        return True

    if expected_challenge == "turn_right" and detected_pose == "right":
        return True

    return False


@app.route("/")
def home():
    return {"message": "Server is running"}


# ---------------- UTIL ----------------
def decode_image(img_b64):
    img_bytes = base64.b64decode(img_b64)
    arr = np.frombuffer(img_bytes, np.uint8)
    img = cv2.imdecode(arr, cv2.IMREAD_COLOR)

    if img is None:
        return None

    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    return img


def get_embedding_and_kps(img):
    faces = face_app.get(img)

    if len(faces) == 0:
        return None, None

    face = faces[0]
    return face.embedding, face.kps


def save_embedding(user_id, embedding):
    rows = []
    user_found = False

    # Read existing data
    if os.path.exists(CSV_FILE):
        with open(CSV_FILE, mode='r') as file:
            reader = csv.DictReader(file)
            for row in reader:
                if row["user_id"] == user_id:
                    # Replace existing embedding
                    rows.append({
                        "user_id": user_id,
                        "embedding": embedding.tolist()
                    })
                    user_found = True
                else:
                    rows.append(row)

    # If user not found → add new
    if not user_found:
        rows.append({
            "user_id": user_id,
            "embedding": embedding.tolist()
        })

    # Write everything back (overwrite file)
    with open(CSV_FILE, mode='w', newline='') as file:
        writer = csv.DictWriter(file, fieldnames=["user_id", "embedding"])
        writer.writeheader()
        writer.writerows(rows)


def load_embeddings(user_id):
    if not os.path.exists(CSV_FILE):
        return []

    embeddings = []

    with open(CSV_FILE, mode='r') as file:
        reader = csv.DictReader(file)

        for row in reader:
            if row["user_id"] == user_id:
                emb = np.array(ast.literal_eval(row["embedding"]))
                embeddings.append(emb)

    return embeddings


def cosine_similarity(a, b):
    return np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))


# ---------------- ENROLL ----------------
@app.route("/enroll", methods=["POST"])
def enroll():
    data = request.json

    user_id = data["user_id"]
    frames = data["frames"]

    embeddings = []

    for frame in frames:
        img = decode_image(frame)
        if img is None:
            continue

        emb, _ = get_embedding_and_kps(img)
        if emb is not None:
            embeddings.append(emb)

    if len(embeddings) == 0:
        return jsonify({"status": "no_face"})

    final_embedding = np.mean(embeddings, axis=0)
    final_embedding = final_embedding / np.linalg.norm(final_embedding)

    save_embedding(user_id, final_embedding)

    return jsonify({
        "status": "enrolled",
        "embedding_dim": len(final_embedding)
    })


@app.route("/verify", methods=["POST"])
def verify():
    data = request.json

    user_id = data["user_id"]
    print(user_id)
    frames = data["frames"]
    challenges = data.get("challenges", [])

    imgs = [decode_image(f) for f in frames]

    if any(img is None for img in imgs):
        return jsonify({"status": "invalid_frames"})

    results = []
    for img in imgs:
        emb, kps = get_embedding_and_kps(img)
        results.append((emb, kps))

    if all(r[0] is None for r in results):
        return jsonify({"status": "no_face"})

    # ---------------- TRY LIVENESS ----------------
    try:
        if len(frames) == 3 and len(challenges) == 2:
            emb1, kps1 = results[0]
            emb2, kps2 = results[1]
            emb3, kps3 = results[2]

            if emb1 is None or emb2 is None or emb3 is None:
                raise Exception("Face missing in some frames")

            if not evaluate_challenge(challenges[1], kps3):
                return jsonify({
                    "status": "liveness_failed",
                    "message": f"Failed: {challenges[1]}"
                })

            final_embedding = emb1  # same as your main pipeline

        else:
            raise Exception("Invalid liveness input")

    except Exception:
        # ---------------- FALLBACK (OLD FLOW) ----------------
        embeddings = [r[0] for r in results if r[0] is not None]

        if len(embeddings) == 0:
            return jsonify({"status": "no_face"})

        final_embedding = np.mean(embeddings, axis=0)

    # ---------------- VERIFICATION ----------------
    stored_embeddings = load_embeddings(user_id)

    if len(stored_embeddings) == 0:
        return jsonify({"status": "user_not_found"})

    similarities = [
        cosine_similarity(final_embedding, emb)
        for emb in stored_embeddings
    ]

    best_similarity = max(similarities)
    print(best_similarity)

    if best_similarity > 0.50:
        return jsonify({
            "status": "verified",
            "similarity": float(best_similarity)
        })

    return jsonify({
        "status": "rejected",
        "similarity": float(best_similarity)
    })

# ---------------- RUN ----------------
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)