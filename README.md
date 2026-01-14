# AI Speaking Test System

Hệ thống kiểm tra speaking tự động sử dụng Local AI (Whisper + Qwen) chấm điểm và đưa ra nhận xét chi tiết.

## 🎯 Tổng quan

Hệ thống bao gồm các thành phần:
- **Backend (Spring Boot 3.2)**: REST API server với JWT authentication, DTO pattern, pagination & filtering
- **Whisper Server (Python)**: Speech-to-Text conversion (OpenAI Whisper)
- **Qwen Server (Python)**: LLM-based scoring và feedback generation (Alibaba Qwen)
- **Database (MySQL 8.0)**: Persistent storage với soft delete pattern

## 🏗️ Kiến trúc hệ thống

```
┌──────────────────────────────┐
│   Frontend (React/Vue)       │
│   - Login/Register UI        │
│   - Test Taking Interface    │
│   - Admin Dashboard          │
└──────────┬───────────────────┘
           │ HTTP/REST + JWT
           ▼
┌────────────────────────────────┐
│   Spring Boot Backend (8080)   │
│   ├─ Security (JWT)            │
│   ├─ Controllers (REST APIs)   │
│   ├─ Services (Business Logic) │
│   ├─ DTOs (Data Transfer)      │
│   └─ Repositories (JPA)        │
└──────┬──────────┬──────────────┘
       │          │
       │          ├─────────────► MySQL Database (3306)
       │          │                └─ 8 Tables with indexes
       ▼          ▼
┌──────────┐  ┌──────────────┐
│ Whisper  │  │    Qwen      │
│  (5000)  │  │   (5001)     │
│  STT AI  │  │  Scoring AI  │
└──────────┘  └──────────────┘
```

## 🔄 Luồng xử lý

1. **Authentication**: User login → JWT token generation → Token validation
2. **Test Creation**: Admin creates questions → Generates exams (manual or random)
3. **Test Taking**: Student starts session → Records audio answers → Uploads files
4. **AI Processing**: 
   - Backend → Whisper Server → Transcribed text
   - Backend → Qwen Server (with text + question + sample answers) → Score + Feedback
5. **Result**: Store in database → Display to student → Export reports

## 📊 Cấu trúc Database

### Tables (8)
- **users**: User management (Admin/Teacher roles) với password encryption
- **questions**: Question bank với level (EASY/MEDIUM/HARD), category, indexes
- **sample_answers**: Sample answers cho mỗi question với scoring rubric
- **exams**: Exam definitions với duration, status (ACTIVE/INACTIVE/DRAFT)
- **exam_questions**: Many-to-many relationship giữa exams và questions
- **test_sessions**: Student test sessions với total score và completion tracking
- **test_answers**: Individual answers với audio URL, transcription, score, feedback
- **ai_processing_logs**: AI processing audit logs (Whisper + Qwen)

### Key Features
- ✅ Soft delete pattern (deletedAt field)
- ✅ Audit fields (createdAt, updatedAt, createdBy)
- ✅ Indexes for performance (level, category, status, dates)
- ✅ Foreign key constraints với proper cascading

## 🚀 Cài đặt

### Yêu cầu hệ thống
- **Java**: 17 hoặc mới hơn
- **Maven**: 3.6+ (hoặc sử dụng Maven wrapper)
- **MySQL**: 8.0+
- **Python**: 3.9+ (cho AI servers)
- **RAM**: Tối thiểu 8GB (16GB recommended cho AI models)
- **Disk**: ~5GB (models + dependencies)

### 🗄️ Setup Database

```bash
# Kết nối MySQL
mysql -u root -p

# Tạo database
CREATE DATABASE ai_speaking_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# Import schema
USE ai_speaking_db;
SOURCE database/schema.sql;

# Verify
SHOW TABLES;
```

### ⚙️ Setup Backend (Spring Boot)

```bash
# Clone repository
cd backend

# Cấu hình database
# Edit src/main/resources/application.properties:
#   spring.datasource.username=root
#   spring.datasource.password=your_password
#   spring.datasource.url=jdbc:mysql://localhost:3306/ai_speaking_db

# Build project
mvn clean install

# Run application
mvn spring-boot:run

# Hoặc chạy JAR file
# java -jar target/ai-speaking-0.0.1-SNAPSHOT.jar
```

✅ Backend sẽ chạy tại: `http://localhost:8080`

**Default Admin Account:**
- Username: `admin`
- Password: `admin123`

### 🎤 Setup Whisper Server

```bash
cd whisper_server

# Create virtual environment
python -m venv venv

# Activate
# Windows:
venv\Scripts\activate
# Linux/Mac:
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Run server
python whisper_server.py
```

✅ Whisper Server: `http://localhost:5000`

**Note**: First run sẽ download Whisper model (~150MB - 3GB tùy model size)

### 🧠 Setup Qwen Server

```bash
cd qwen_server

# Create virtual environment
python -m venv venv

# Activate
# Windows:
venv\Scripts\activate
# Linux/Mac:
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Run server
python qwen_server.py
```

✅ Qwen Server: `http://localhost:5001`

**Note**: First run sẽ download Qwen model (~500MB - 1.5GB tùy model size)

### 🚀 Quick Start (All Services)

**Windows:**
```bash
# Chạy tất cả services cùng lúc
start-all.bat
```

**Manual:**
```bash
# Terminal 1: Backend
cd backend && mvn spring-boot:run

# Terminal 2: Whisper
cd whisper_server && python whisper_server.py

# Terminal 3: Qwen
cd qwen_server && python qwen_server.py
```

## 📡 API Documentation

### 🔐 Authentication
- `POST /auth/login` - User login (returns JWT token)
- `POST /auth/register` - Register new teacher account
- `GET /auth/check-username/{username}` - Check username availability

### 👥 User Management
- `GET /users` - Get all users (paginated)
- `GET /users/{id}` - Get user by ID
- `GET /users/username/{username}` - Get user by username
- `POST /users` - Create new user (admin only)
- `PUT /users/{id}` - Update user
- `PUT /users/{id}/change-password` - Change password
- `PUT /users/{id}/toggle-status` - Toggle active status
- `DELETE /users/{id}` - Soft delete user

### ❓ Questions Management
- `GET /questions` - Get all questions (paginated, supports filtering)
- `GET /questions/search` - Advanced search (level, category, createdBy, dates)
- `GET /questions/{id}` - Get question by ID
- `POST /questions` - Create new question
- `PUT /questions/{id}` - Update question
- `DELETE /questions/{id}` - Soft delete question

**Filter Parameters:**
- `level`: EASY, MEDIUM, HARD
- `category`: String
- `createdBy`: User ID
- `createdAfter`, `createdBefore`: Date range

### 📝 Exams Management
- `GET /exams` - Get all exams (paginated, supports filtering)
- `GET /exams/search` - Advanced search (status, name, dates)
- `GET /exams/{id}` - Get exam by ID
- `GET /exams/{id}/questions` - Get all questions in exam
- `POST /exams` - Create new exam
- `POST /exams/{id}/questions` - Add questions to exam (manual)
- `POST /exams/generate-random` - Generate random exam
- `PUT /exams/{id}` - Update exam
- `DELETE /exams/{id}` - Soft delete exam

**Filter Parameters:**
- `status`: ACTIVE, INACTIVE, DRAFT
- `name`: String search
- `createdAfter`, `createdBefore`: Date range

### 🎓 Test Sessions
- `GET /test-sessions` - Get all test sessions (paginated)
- `GET /test-sessions/search` - Advanced search (examId, student name, status, scores)
- `GET /test-sessions/{id}` - Get session details
- `GET /test-sessions/{id}/answers` - Get all answers for session
- `POST /test-sessions` - Start new test session
- `POST /test-sessions/{id}/answers` - Submit answer (with audio file)
- `POST /test-sessions/{id}/complete` - Complete test session

**Search Parameters:**
- `examId`: Long
- `studentName`: String
- `status`: IN_PROGRESS, COMPLETED, CANCELLED
- `minScore`, `maxScore`: Decimal (0-10)
- `startedAfter`, `startedBefore`: Date range

### 📊 Statistics & Analytics
- `GET /statistics/dashboard` - Dashboard overview
- `GET /statistics/questions/by-level` - Question count by level
- `GET /statistics/exams/by-status` - Exam count by status
- `GET /statistics/test-sessions/by-status` - Session count by status
- `GET /statistics/test-sessions/{id}` - Detailed session statistics
- `GET /statistics/exams/{id}` - Detailed exam statistics
- `GET /statistics/test-sessions/recent?limit=10` - Recent test sessions
- `GET /statistics/by-date-range?startDate=...&endDate=...` - Stats by date range

### 🔧 Admin Operations
- `DELETE /admin/questions/bulk-delete` - Delete multiple questions
  ```json
  {"questionIds": [1, 2, 3]}
  ```
- `PUT /admin/exams/bulk-update-status` - Update status for multiple exams
  ```json
  {"examIds": [1, 2], "status": "ACTIVE"}
  ```
- `GET /admin/test-sessions` - View all test sessions with filters
- `PUT /admin/test-sessions/{id}/cancel` - Cancel a test session
- `DELETE /admin/test-sessions/{id}` - Delete test session
- `GET /admin/health` - System health check
- `GET /admin/config` - System configuration

### 📄 Reports & Export
- `GET /reports/test-session/{id}/export-csv` - Export session as CSV
- `GET /reports/test-session/{id}/detailed` - Detailed JSON report
- `GET /reports/exam/{examId}/export-csv` - Export all sessions for exam as CSV

## 💡 Ví dụ sử dụng

### 1. Authentication Flow

```bash
# Register new teacher account
POST /auth/register
Content-Type: application/json

{
  "username": "teacher01",
  "password": "SecurePass123",
  "fullName": "Nguyen Van A"
}

# Login
POST /auth/login
Content-Type: application/json

{
  "username": "teacher01",
  "password": "SecurePass123"
}

# Response:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "id": 2,
  "username": "teacher01",
  "fullName": "Nguyen Van A",
  "role": "TEACHER",
  "isActive": true,
  "message": "Login successful"
}

# Use token in subsequent requests:
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 2. Create Question with Sample Answers

```bash
POST /questions
Authorization: Bearer <token>
Content-Type: application/json

{
  "content": "Describe your favorite place to visit",
  "level": "MEDIUM",
  "category": "Travel",
  "sampleAnswers": [
    {
      "content": "My favorite place is the beach. I love the sound of waves and the fresh sea breeze.",
      "score": 8.5,
      "explanation": "Good vocabulary, clear structure"
    }
  ]
}
```

### 3. Generate Random Exam

```bash
# Step 1: Create exam definition
POST /exams
Authorization: Bearer <token>

{
  "name": "Speaking Test - Level 2",
  "totalQuestions": 10,
  "durationMinutes": 20,
  "passingScore": 6.0,
  "status": "ACTIVE"
}

# Step 2: Generate random questions
POST /exams/generate-random
Authorization: Bearer <token>

{
  "examId": 1,
  "level": "MEDIUM",
  "count": 10
}
```

### 4. Student Takes Test

```bash
# Start test session (no authentication required for students)
POST /test-sessions
Content-Type: application/json

{
  "examId": 1,
  "studentName": "Nguyen Van A",
  "studentOrganization": "ABC Company"
}

# Response: {"id": 1, "examId": 1, "status": "IN_PROGRESS", ...}

# Submit audio answer
POST /test-sessions/1/answers?questionId=1
Content-Type: multipart/form-data

audio=@recording.wav

# AI Processing happens automatically:
# 1. Whisper transcribes audio → text
# 2. Qwen scores answer → feedback
# Response: {"score": 7.5, "feedback": "Good attempt...", ...}

# Complete test
POST /test-sessions/1/complete

# Response: {"totalScore": 7.8, "status": "COMPLETED", ...}
```

### 5. Advanced Search & Filtering

```bash
# Search questions by multiple criteria
GET /questions/search?level=MEDIUM&category=Travel&createdAfter=2026-01-01&page=0&size=20
Authorization: Bearer <token>

# Search test sessions with score range
GET /test-sessions/search?minScore=7.0&maxScore=9.0&status=COMPLETED&page=0&size=10
Authorization: Bearer <token>

# Get recent test sessions
GET /statistics/test-sessions/recent?limit=5
Authorization: Bearer <token>
```

## ✨ Features

### Admin/Teacher Features
- ✅ **User Management**: Create, update, deactivate users (Admin only)
- ✅ **Question Bank**: CRUD operations với level, category classification
- ✅ **Sample Answers**: Multiple sample answers per question với scoring rubric
- ✅ **Exam Creation**: Manual selection hoặc random generation
- ✅ **Advanced Filtering**: Multi-criteria search cho Questions, Exams, Test Sessions
- ✅ **Bulk Operations**: Delete multiple questions, update multiple exam statuses
- ✅ **Statistics Dashboard**: Real-time analytics và performance metrics
- ✅ **CSV Export**: Export test results và exam reports
- ✅ **Audit Logs**: Track AI processing (Whisper + Qwen)

### Student Features
- ✅ **No Registration Required**: Enter name và organization to start
- ✅ **Audio Recording**: Record answers directly in browser
- ✅ **Real-time Feedback**: Instant scoring sau khi submit
- ✅ **Detailed Feedback**: AI-generated explanations và suggestions
- ✅ **Progress Tracking**: See answered/pending questions
- ✅ **Final Report**: Overall score với detailed breakdown

### Technical Features
- ✅ **JWT Authentication**: Secure token-based authentication
- ✅ **DTO Pattern**: Clean separation giữa entities và API contracts
- ✅ **Soft Delete**: Preserve data integrity với deletedAt pattern
- ✅ **Pagination**: Efficient data loading với Spring Data Pageable
- ✅ **Validation**: Jakarta Bean Validation cho request data
- ✅ **Error Handling**: Consistent error responses
- ✅ **CORS Support**: Configurable CORS cho frontend integration
- ✅ **File Upload**: Secure audio file handling
- ✅ **Async Processing**: Non-blocking AI operations
- ✅ **Transaction Management**: ACID compliance với @Transactional

## ⚙️ Configuration

### Application Properties

```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/ai_speaking_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# File Upload
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
file.upload.dir=uploads/audio

# AI Services
ai.whisper.url=http://localhost:5000
ai.qwen.url=http://localhost:5001
ai.request.timeout=60000

# JWT Security
jwt.secret=your-secret-key-change-in-production
jwt.expiration=86400000

# CORS
cors.allowed.origins=http://localhost:3000,http://localhost:5173
```

### Whisper Model Options

Edit `whisper_server.py`:
```python
# Model sizes: tiny, base, small, medium, large
# Larger = more accurate but slower
model = whisper.load_model("base")  # Change to "small" or "medium"

# Use GPU if available
device = "cuda" if torch.cuda.is_available() else "cpu"
```

**Model Comparison:**
| Model | Size | Speed | Accuracy |
|-------|------|-------|----------|
| tiny  | ~40MB | Very Fast | Good |
| base  | ~150MB | Fast | Better |
| small | ~500MB | Medium | Very Good |
| medium| ~1.5GB | Slow | Excellent |
| large | ~3GB | Very Slow | Best |

### Qwen Model Options

Edit `qwen_server.py`:
```python
# Model options:
# - Qwen/Qwen2.5-0.5B-Instruct (fastest, ~500MB)
# - Qwen/Qwen2.5-1.5B-Instruct (balanced, ~1.5GB)
# - Qwen/Qwen2.5-3B-Instruct (best quality, ~3GB)

model_name = "Qwen/Qwen2.5-0.5B-Instruct"

# Generation parameters
max_new_tokens = 256  # Increase for longer feedback
temperature = 0.7     # 0.1-1.0, higher = more creative
```

### AI Scoring Prompt Customization

Edit `AIProcessingService.java`:
```java
private String buildScoringPrompt(Question question, List<SampleAnswer> sampleAnswers) {
    // Customize scoring criteria:
    // - Grammar weight
    // - Vocabulary richness
    // - Fluency
    // - Content relevance
    // - Pronunciation (if needed)
}
```

## 🐛 Troubleshooting

### Backend Issues

**Database Connection Failed**
```bash
# Check MySQL is running
mysql -u root -p

# Create database if not exists
CREATE DATABASE ai_speaking_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# Verify credentials in application.properties
spring.datasource.username=root
spring.datasource.password=your_password
```

**Port 8080 Already in Use**
```bash
# Windows: Find and kill process
netstat -ano | findstr :8080
taskkill /PID <process_id> /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9

# Or change port in application.properties
server.port=8081
```

**JWT Token Expired**
```bash
# Login again to get new token
POST /auth/login

# Increase expiration time (application.properties)
jwt.expiration=86400000  # 24 hours in milliseconds
```

### Whisper Server Issues

**FFmpeg Not Found**
```bash
# Windows: Download from https://ffmpeg.org/
# Add to PATH or install via pip
pip install ffmpeg-python

# Ubuntu/Debian
sudo apt-get install ffmpeg

# Mac
brew install ffmpeg
```

**CUDA Out of Memory**
```python
# Use CPU instead (whisper_server.py)
device = "cpu"

# Or use smaller model
model = whisper.load_model("tiny")
```

**Slow Transcription**
- Use smaller model (tiny/base instead of medium/large)
- Enable GPU acceleration if available
- Reduce audio quality before upload

### Qwen Server Issues

**Model Download Failed**
```bash
# Manual download from Hugging Face
# Set HF_HOME environment variable
export HF_HOME=/path/to/models

# Use mirror if in restricted region
export HF_ENDPOINT=https://hf-mirror.com
```

**Out of Memory**
```python
# Use smaller model (qwen_server.py)
model_name = "Qwen/Qwen2.5-0.5B-Instruct"

# Reduce max_new_tokens
max_new_tokens = 128

# Use CPU if GPU memory insufficient
device = "cpu"
```

**Slow Scoring**
- Use 0.5B model instead of 1.5B/3B
- Reduce `max_new_tokens` to 128-256
- Use GPU if available (requires CUDA setup)

### File Upload Issues

**File Too Large**
```properties
# Increase limits (application.properties)
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB
```

**Upload Directory Not Found**
```bash
# Create directories
mkdir -p uploads/audio

# Or change path in application.properties
file.upload.dir=/absolute/path/to/uploads
```

**Permission Denied**
```bash
# Grant write permissions
# Windows
icacls uploads /grant Users:F

# Linux/Mac
chmod -R 755 uploads
```

### Common Errors

**"User not found"**
- Use default admin account (admin/admin123)
- Register new account via `/auth/register`

**"Exam not found"**
- Create exam first before generating questions
- Check exam ID in response after creation

**"Invalid JWT token"**
- Token might be expired, login again
- Check Authorization header format: `Bearer <token>`
- Verify jwt.secret matches in application.properties

## 📚 Project Structure

```
backend/
├── src/main/java/com/aispeaking/
│   ├── config/           # Configuration classes
│   │   ├── AppConfig.java
│   │   ├── CorsConfig.java
│   │   └── SecurityConfig.java
│   ├── controller/       # REST Controllers (8)
│   │   ├── AuthController.java
│   │   ├── UserController.java
│   │   ├── QuestionController.java
│   │   ├── ExamController.java
│   │   ├── TestSessionController.java
│   │   ├── StatisticsController.java
│   │   ├── AdminController.java
│   │   └── ReportController.java
│   ├── dto/              # Data Transfer Objects (19)
│   │   ├── Request DTOs (User, Question, Exam, Session)
│   │   └── Response DTOs (with from() factory methods)
│   ├── entity/           # JPA Entities (8)
│   │   ├── User.java
│   │   ├── Question.java
│   │   ├── SampleAnswer.java
│   │   ├── Exam.java
│   │   ├── ExamQuestion.java
│   │   ├── TestSession.java
│   │   ├── TestAnswer.java
│   │   ├── AIProcessingLog.java
│   │   └── enums/        # Enums (6)
│   ├── repository/       # Spring Data JPA Repositories (8)
│   ├── security/         # Security & JWT
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── JwtTokenProvider.java
│   │   ├── CustomUserDetailsService.java
│   │   └── UserPrincipal.java
│   └── service/          # Business Logic Services (6)
│       ├── UserService.java
│       ├── QuestionService.java
│       ├── ExamService.java
│       ├── TestSessionService.java
│       ├── AIProcessingService.java
│       └── StatisticsService.java
├── src/main/resources/
│   └── application.properties
├── database/
│   └── schema.sql        # Database schema
├── whisper_server/       # Speech-to-Text AI
│   ├── whisper_server.py
│   └── requirements.txt
├── qwen_server/          # Scoring AI
│   ├── qwen_server.py
│   └── requirements.txt
└── pom.xml               # Maven dependencies
```

## 🔐 Security Notes

- 🔒 Passwords are encrypted using BCrypt
- 🎫 JWT tokens expire after 24 hours (configurable)
- 🚫 Soft delete prevents data loss
- ✅ CORS configured for allowed origins only
- 🔑 Admin operations require ADMIN role
- 📝 Audit logs track all AI processing

## 🚀 Performance Tips

1. **Database**: Add indexes for frequently queried fields
2. **AI Models**: Use appropriate model sizes for your hardware
3. **Caching**: Consider Redis for session/token caching
4. **File Storage**: Use cloud storage (S3/Azure) for production
5. **Load Balancing**: Use multiple AI server instances
6. **Monitoring**: Add application performance monitoring (APM)

## 📖 Additional Documentation

- **API Details**: See full API documentation in codebase
- **Database Schema**: Check `database/schema.sql`
- **Postman Collection**: Import for API testing
- **Architecture**: See architecture diagrams in docs

## 👥 Authors & Credits

- **Backend Framework**: Spring Boot 3.2 + Spring Security + Spring Data JPA
- **AI Models**: 
  - OpenAI Whisper (Speech-to-Text)
  - Alibaba Qwen 2.5 (Language Model for Scoring)
- **Database**: MySQL 8.0
- **Authentication**: JWT (JSON Web Tokens)
- **Build Tool**: Maven
- **Java Version**: 17 (LTS)

## 📄 License

MIT License - Feel free to use this project for learning and development purposes.

---

**Built with ❤️ using Spring Boot, Whisper AI, and Qwen AI**
