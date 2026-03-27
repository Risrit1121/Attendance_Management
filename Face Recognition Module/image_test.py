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


# ---------------- UTIL ----------------
def decode_image(img_b64):
    img_bytes = base64.b64decode(img_b64)
    arr = np.frombuffer(img_bytes, np.uint8)
    img = cv2.imdecode(arr, cv2.IMREAD_COLOR)

    if img is None:
        return None

    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    return img


def get_embedding(img):
    faces = face_app.get(img)

    if len(faces) == 0:
        return None

    return faces[0].embedding


def save_embedding(user_id, embedding):
    file_exists = os.path.isfile(CSV_FILE)

    with open(CSV_FILE, mode='a', newline='') as file:
        writer = csv.writer(file)

        if not file_exists:
            writer.writerow(["user_id", "embedding"])

        writer.writerow([user_id, embedding.tolist()])


def load_embeddings(user_id):
    if not os.path.exists(CSV_FILE):
        return []

    embeddings = []

    with open(CSV_FILE, mode='r') as file:
        reader = csv.DictReader(file)

        for row in reader:
            if row["user_id"] == user_id:
                emb = np.array(eval(row["embedding"]))
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

        emb = get_embedding(img)
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


# ---------------- VERIFY ----------------
@app.route("/verify", methods=["POST"])
def verify():
    data = request.json

    user_id = data["user_id"]
    frames = data["frames"]

    embeddings = []

    for frame in frames:
        img = decode_image(frame)
        if img is None:
            continue

        emb = get_embedding(img)
        if emb is not None:
            embeddings.append(emb)

    if len(embeddings) == 0:
        return jsonify({"status": "no_face"})

    final_embedding = np.mean(embeddings, axis=0)

    stored_embeddings = load_embeddings(user_id)

    if len(stored_embeddings) == 0:
        return jsonify({"status": "user_not_found"})

    similarities = [
        cosine_similarity(final_embedding, emb)
        for emb in stored_embeddings
    ]

    best_similarity = max(similarities)

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
    app.run(debug=True, port=5000)