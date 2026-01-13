#!/bin/bash

echo "Starting AI Speaking Test System..."
echo ""

echo "[1/3] Starting Whisper Server..."
cd whisper_server
source venv/bin/activate
python whisper_server.py &
WHISPER_PID=$!
cd ..
sleep 5

echo "[2/3] Starting Qwen Server..."
cd qwen_server
source venv/bin/activate
python qwen_server.py &
QWEN_PID=$!
cd ..
sleep 5

echo "[3/3] Starting Spring Boot Backend..."
mvn spring-boot:run &
BACKEND_PID=$!

echo ""
echo "All services are starting..."
echo "- Whisper Server: http://localhost:5000 (PID: $WHISPER_PID)"
echo "- Qwen Server: http://localhost:5001 (PID: $QWEN_PID)"
echo "- Backend API: http://localhost:8080 (PID: $BACKEND_PID)"
echo ""
echo "Press Ctrl+C to stop all services"

# Wait for all background processes
wait
