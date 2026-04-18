# 1. Use an official, lightweight Python image
FROM python:3.10-slim

# 2. Install system-level dependencies required by OpenCV and InsightFace
# (Without these, cv2 will crash on a Linux server)
RUN apt-get update && apt-get install -y \
    libgl1 \
    libglib2.0-0 \
    g++ \
    && rm -rf /var/lib/apt/lists/*

# 3. Set the working directory inside the container
WORKDIR /app

# 4. Copy your requirements file first
COPY requirements.txt .

# 5. Install the Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# 6. Copy the rest of your application code into the container
COPY . .

# 7. Expose the port FastAPI runs on
EXPOSE 8000

# 8. The command to run your app (Using 1 worker for simplicity!)
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "1"]