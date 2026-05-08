# Face Recognition Microservice

A high-performance, containerized face recognition microservice built using FastAPI, InsightFace (buffalo_sc), MongoDB, and Redis. The system supports robust enrollment and real-time verification with optimized embedding storage, caching, and parallel inference.

---

# 🚀 Features

- Redis-based embedding caching for fast verification
- Local Dockerized MongoDB for persistent storage
- InsightFace buffalo_sc embeddings (512-dimensional)
- Parallel frame processing using ThreadPoolExecutor
- Embedding dimension validation and sanitization
- Atomic MongoDB embedding updates
- FastAPI-based REST API
- Redis failure isolation and resilient DB handling
- Optimized for high-concurrency verification workloads

---

# ⚙️ Quick Start (Docker)

The recommended deployment method is Docker Compose.

## 1. Start the Services

```bash
docker-compose up -d --build
```

This starts:

- FastAPI application
- MongoDB
- Redis cache

The API will run on:

```text
http://localhost:9000
```

---

## 2. Verify Containers

Check logs:

```bash
docker logs face_recognition_microservice
```

Expected logs:

```text
✅ Connected to MongoDB
✅ Connected to Redis Cache
```

---

# 💻 Local Development Setup

## 1. Install Dependencies

Python 3.10 recommended.

```bash
pip install -r requirements.txt
```

---

## 2. Configure Environment Variables

Create a `.env` file:

```env
MONGO_URI="mongodb://localhost:27017/"
REDIS_URI="redis://localhost:6379/0"
```

---

## 3. Run the Server

```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --workers 4
```

---

# 📡 API Endpoints

## 🔹 Enroll

**POST** `/enroll`

### Request

```json
{
  "user_id": "student_123",
  "frames": ["base64_string_1", "base64_string_2"]
}
```

### Response

```json
{
  "status": "enrolled",
  "embedding_dim": 512
}
```

---

## 🔹 Verify

**POST** `/verify`

### Request

```json
{
  "user_id": "student_123",
  "frames": ["base64_string_1", "base64_string_2", "base64_string_3"]
}
```

### Response

```json
{
  "status": "verified",
  "similarity": 0.82
}
```

---

# 📂 Project Structure

```text
.
├── main.py
├── routes/
├── services/
├── face_module/
├── face_module/database/
├── requirements.txt
├── docker-compose.yml
└── .env
```

---

# 🧠 Core Components

| Component        | Description                          |
| ---------------- | ------------------------------------ |
| `main.py`        | FastAPI application entry point      |
| `routes/`        | API endpoint definitions             |
| `services/`      | Verification and enrollment pipeline |
| `face_module/`   | InsightFace inference and utilities  |
| `database/db.py` | MongoDB + Redis embedding layer      |

---

# ⚡ Performance Optimizations

- Redis caching reduces repeated MongoDB reads
- Parallel frame inference improves throughput
- Atomic embedding updates prevent race conditions
- Automatic cache invalidation after enrollment
- Strict 512-dimensional embedding validation
- Keeps only latest embeddings per user

---

# 🛠️ Tech Stack

- FastAPI
- InsightFace
- MongoDB
- Redis
- Docker
- NumPy
- OpenCV

---

# 👨‍💻 Author

**Challa Srikrishna Reddy**  
ES22BTECH11006

GitHub: https://github.com/NaniReddyCh
