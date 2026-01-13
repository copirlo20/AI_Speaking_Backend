@echo off
echo Starting AI Speaking Test System...
echo.

echo [1/3] Starting Whisper Server...
start "Whisper Server" cmd /k "cd whisper_server && venv\Scripts\activate && python whisper_server.py"
timeout /t 5 /nobreak

echo [2/3] Starting Qwen Server...
start "Qwen Server" cmd /k "cd qwen_server && venv\Scripts\activate && python qwen_server.py"
timeout /t 5 /nobreak

echo [3/3] Starting Spring Boot Backend...
start "Spring Boot Backend" cmd /k "mvn spring-boot:run"

echo.
echo All services are starting...
echo - Whisper Server: http://localhost:5000
echo - Qwen Server: http://localhost:5001
echo - Backend API: http://localhost:8080
echo.
pause
