# Face Recognition Microservice

A high-performance, containerized Face Recognition API built with FastAPI, InsightFace (MobileFaceNet), and MongoDB Atlas. Designed for robust face enrollment and real-time verification.

## 🚀 Key Features

- **Parallel Inference:** Processes multiple face frames simultaneously using `ThreadPoolExecutor` for fast response times.
- **Cloud Database:** Integrated with MongoDB Atlas for persistent, instant identity lookups via indexed `user_id`s.
- **Docker-Ready:** Fully containerized for seamless deployment without dependency or C++ compilation conflicts.
- **Robust Matching:** Averages embeddings across multiple frames and verifies identities using Cosine Similarity.

---

## ⚙️ Quick Start (Docker Deployment)

The easiest and safest way to deploy this microservice in production is via Docker Compose.

### 1. Configure Environment

Create a `.env` file in the root directory and add your MongoDB Atlas connection string:

```env
MONGO_URI="mongodb+srv://<username>:<password>@<cluster>.mongodb.net/?retryWrites=true&w=majority"
```

### 2. Build and Run

Execute the following command on your deployment server:

```bash
docker-compose up -d --build
```

The service will be available at `http://localhost:8000`, running 4 parallel worker processes.

---

## 💻 Local Development Setup

If you need to run the server locally (without Docker):

1. **Install dependencies** (Python 3.10 recommended):
   ```bash
   pip install -r requirements.txt
   ```
2. **Setup environment:** Ensure your `.env` file exists with your `MONGO_URI`.
3. **Start the server:**
   ```bash
   uvicorn main:app --host 0.0.0.0 --port 8000 --workers 4
   ```

---

## 📡 API Endpoints

### 🔹 1. Enroll Face

**POST** `/enroll-face/`

**Request Payload:**

```json
{
  "user_id": "student_123",
  "frames": ["base64_string_1", "base64_string_2", "..."]
}
```

**Response:**

```json
{
  "status": "enrolled",
  "embedding_dim": 512
}
```

---

### 🔹 2. Verify Face

**POST** `/verify-face/`

**Request Payload:**

```json
{
  "user_id": "student_123",
  "frames": ["base64_string_1", "base64_string_2", "base64_string_3"],
  "challenges": ["blink", "turn_left"]
}
```

**Response:**

```json
{
  "status": "verified",
  "similarity": 0.82
}
```

---

## 📂 Project Architecture

- `main.py`: FastAPI application entry point
- `routes/`: Endpoint definitions (`/enroll-face`, `/verify-face`)
- `services/face_service.py`: Core pipeline managing parallel image decoding, liveness checks, and neural network inference
- `face_module/`: Machine learning engine (InsightFace, head pose calculations, MongoDB interactions)

---

## Author

- **Srikrishna Reddy** - [ES22BTECH11006](https://github.com/NaniReddyCh)
