# Face Recognition Microservice

A lightweight face recognition microservice built with FastAPI for **face enrollment** and **verification** using deep learning embeddings.

This service is designed to be plugged into a larger backend (e.g., Node.js / main API server).

---

## 🚀 Overview

This microservice provides:

- Face Enrollment (store user embeddings)
- Face Verification (match live face against stored embeddings)
- Multi-frame processing for robustness
- Cosine similarity-based matching

---

## 🧠 How It Works

### Enrollment

1. Client sends multiple face frames
2. Embeddings are generated for each frame
3. All embeddings are averaged
4. Final embedding is normalized and stored

### Verification

1. Client sends multiple face frames
2. Embeddings are generated
3. Averaged into a single vector
4. Compared with stored embeddings using cosine similarity
5. Decision made based on threshold (default: **0.50**)

---

## 🏗️ Project Structure

```
face-service/
├── main.py                  # FastAPI entry point
├── routes/
│   ├── enroll.py
│   └── verify.py
├── services/
│   └── face_service.py      # Core pipeline logic
├── face_module/             # Your existing ML code (unchanged)
│   ├── embedding/
│   ├── verification/
│   └── database/
```

---

## ⚙️ Setup

### 1. Install dependencies

```bash
pip install -r requirements.txt
```

Or manually:

```bash
pip install fastapi uvicorn insightface onnxruntime numpy opencv-python
```

---

### 2. Run the service

```bash
uvicorn main:app --reload
```

Service runs at:

```
http://localhost:8000
```

---

## 📡 API Endpoints

---

### 🔹 1. Enroll Face

**POST** `/enroll-face/`

#### Request

```json
{
  "user_id": "user_123",
  "frames": ["base64_img1", "base64_img2", "..."]
}
```

#### Response

```json
{
  "status": "enrolled",
  "embedding_dim": 512
}
```

#### Possible Status:

- `enrolled`
- `no_face`

---

### 🔹 2. Verify Face

**POST** `/verify-face/`

#### Request

```json
{
  "user_id": "user_123",
  "frames": ["base64_img1", "base64_img2", "..."]
}
```

#### Response

```json
{
  "status": "verified",
  "similarity": 0.78
}
```

#### Possible Status:

- `verified`
- `rejected`
- `no_face`

---

## 🔁 Data Flow

```
Client
  ↓
FastAPI Endpoint
  ↓
Service Layer
  ↓
Decode base64 → Image (RGB)
  ↓
MobileFaceNet → Embeddings
  ↓
Average embeddings
  ↓
Database lookup
  ↓
Cosine similarity
  ↓
Response
```

---

## ⚠️ Important Notes

- Input images must contain **clear human faces**
- Frames should be:
  - 5–15 images per request
  - different angles preferred

- Images are expected as **base64 encoded strings**
- Internally converted to **RGB numpy arrays**

---

## 📊 Performance

| Component       | Time (approx) |
| --------------- | ------------- |
| Image decoding  | 2–8 ms        |
| Embedding model | 20–80 ms      |
| DB lookup       | <5 ms         |
| Total request   | ~100–300 ms   |

---

## 🧠 Model Details

- Model: InsightFace (MobileFaceNet)
- Output: 512-D embedding
- Metric: Cosine similarity

---

## 🔐 Security (Optional)

- Embeddings can be encrypted before storage
- Liveness detection can be integrated (not included here)

---

## 🔌 Integration

This service is meant to be used like:

```
Frontend / Mobile App
        ↓
Main Backend (Node / API)
        ↓
Face Recognition Microservice (this)
```

---

## ❌ What This Service Does NOT Handle

- User authentication
- Session management
- Liveness detection (currently disabled)
- Frontend interaction

---

## 📌 Summary

This is a **clean, production-ready face recognition microservice** that:

- Keeps ML logic isolated
- Provides simple APIs
- Is easy to scale and integrate

---
