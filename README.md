# Hệ Thống Thi Speaking Tự Động với AI

Một hệ thống thi speaking tự động sử dụng AI cục bộ (Whisper + Qwen) để chấm điểm và cung cấp phản hồi chi tiết.

## 🎯 Tổng Quan

Hệ thống bao gồm các thành phần sau:

- **Backend (Spring Boot)**: REST API server với JWT authentication, DTO pattern, pagination & filtering
- **Whisper Server (Python)**: Chuyển đổi giọng nói thành văn bản (OpenAI Whisper)
- **Qwen Server (Python)**: Chấm điểm và tạo phản hồi dựa trên LLM (Alibaba Qwen)
- **Database (MySQL)**: Lưu trữ dữ liệu với soft delete pattern

## 🏗️ Kiến Trúc Hệ Thống

```
┌──────────────────────────────┐
│   Frontend (React/Vue)       │
│   - Giao diện đăng nhập      │
│   - Giao diện làm bài thi    │
│   - Dashboard quản trị       │
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
       │          │                └─ 9 bảng với indexes
       ▼          ▼
┌──────────┐  ┌──────────────┐
│ Whisper  │  │    Qwen      │
│  (5000)  │  │   (5001)     │
│  STT AI  │  │  Chấm điểm   │
└──────────┘  └──────────────┘
```

## 🔄 Luồng Xử Lý

1. **Xác thực**: User đăng nhập → Tạo JWT token → Xác thực token
2. **Tạo kỳ thi**: Admin tạo câu hỏi → Tạo đề thi (thủ công hoặc ngẫu nhiên)
3. **Làm bài thi**: Học sinh bắt đầu → Ghi âm câu trả lời → Upload file
4. **Xử lý AI**:
   - Backend → Whisper Server → Văn bản được chuyển đổi
   - Backend → Qwen Server (với text + câu hỏi + câu trả lời mẫu) → Điểm + Phản hồi
5. **Kết quả**: Lưu vào database → Hiển thị cho học sinh → Xuất báo cáo

## 📊 Cấu Trúc Database

### Bảng (9 bảng)

- **users**: Quản lý người dùng (Admin/Teacher) với mã hóa mật khẩu
- **questions**: Ngân hàng câu hỏi với level (EASY/HARD), indexes
- **sample_answers**: Câu trả lời mẫu cho mỗi câu hỏi với thang điểm
- **exams**: Định nghĩa kỳ thi với trạng thái (ACTIVE/INACTIVE/DRAFT)
- **exam_questions**: Quan hệ nhiều-nhiều giữa exams và questions
- **test_sessions**: Phiên thi của học sinh với tổng điểm và theo dõi hoàn thành
- **test_answers**: Câu trả lời riêng lẻ với audio URL, transcription, điểm, phản hồi
- **ai_processing_logs**: Logs kiểm tra xử lý AI (Whisper + Qwen)
- **base_entity**: Các trường chung (createdAt, updatedAt, deletedAt, createdBy)

### Tính Năng Chính

- ✅ Soft delete pattern (trường deletedAt)
- ✅ Audit fields (createdAt, updatedAt, createdBy)
- ✅ Indexes để tối ưu hiệu suất (level, status, dates)
- ✅ Foreign key constraints với cascading phù hợp

## 🚀 Cài Đặt

### Yêu Cầu Hệ Thống

- **Java**: 21 (LTS)
- **Maven**: 3.6+ (hoặc dùng Maven wrapper)
- **MySQL**: 8.0+
- **Python**: 3.9+ (cho AI servers)
- **RAM**: Tối thiểu 8GB (khuyến nghị 16GB cho AI models)
- **Disk**: ~5GB (models + dependencies)

### 🗄️ Cài Đặt Database

```bash
# Kết nối MySQL
mysql -u root -p

# Tạo database
CREATE DATABASE ai_speaking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# Import schema
USE ai_speaking;
SOURCE database/schema.sql;

# Kiểm tra
SHOW TABLES;
```

### ⚙️ Cài Đặt Backend (Spring Boot)

```bash
# Clone repository
cd backend

# Cấu hình database
# Sửa file src/main/resources/application.properties:
#   spring.datasource.username=root
#   spring.datasource.password=your_password
#   spring.datasource.url=jdbc:mysql://localhost:3306/ai_speaking

# Build project
mvn clean install

# Chạy ứng dụng
mvn spring-boot:run

# Hoặc chạy file JAR
# java -jar target/ai-speaking-backend-1.0.0.jar
```

✅ Backend sẽ chạy tại: `http://localhost:8080`

### 🎤 Cài Đặt Whisper Server

```bash
cd whisper_server

# Cài đặt dependencies
pip install -r requirements.txt

# Chạy server
python whisper_server.py
```

✅ Whisper Server: `http://localhost:5000`

**Lưu ý**: Lần chạy đầu tiên sẽ tải Whisper model (~150MB - 3GB tùy kích thước model)

### 🧠 Cài Đặt Qwen Server

```bash
cd qwen_server

# Cài đặt dependencies
pip install -r requirements.txt

# Chạy server
python qwen_server.py
```

✅ Qwen Server: `http://localhost:5001`

**Lưu ý**: Lần chạy đầu tiên sẽ tải Qwen model (~500MB - 1.5GB tùy kích thước model)

## 📡 Tài Liệu API

Chi tiết đầy đủ xem file: [API_DOCUMENTATION.md](API_DOCUMENTATION.md)

### Tóm Tắt Các Nhóm API

1. **Authentication** (3 APIs): Đăng nhập, đăng ký, kiểm tra username
2. **User Management** (9 APIs): CRUD người dùng - Chỉ ADMIN
3. **Questions** (7 APIs): CRUD câu hỏi - TEACHER + ADMIN
4. **Exams** (9 APIs): CRUD kỳ thi, tạo đề ngẫu nhiên - TEACHER + ADMIN
5. **Test Sessions** (7 APIs): Làm bài thi (PUBLIC), xem kết quả
6. **Statistics** (8 APIs): Thống kê dashboard, báo cáo - TEACHER + ADMIN
7. **Reports** (3 APIs): Xuất CSV, báo cáo chi tiết - TEACHER + ADMIN
8. **Admin Operations** (7 APIs): Xóa hàng loạt, quản lý hệ thống - ADMIN

### Ví Dụ API Cơ Bản

#### 1. Đăng Nhập

```bash
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}

# Response:
{
  "token": "eyJhbGc...",
  "id": 1,
  "username": "admin",
  "fullName": "Administrator",
  "role": "ADMIN",
  "isActive": true
}
```

#### 2. Tạo Câu Hỏi

```bash
POST http://localhost:8080/questions
Authorization: Bearer <token>
Content-Type: application/json

{
  "content": "Describe your hometown",
  "level": "EASY"
}
```

#### 3. Học Sinh Làm Bài (Không cần đăng nhập)

```bash
# Bắt đầu bài thi
POST http://localhost:8080/test-sessions
Content-Type: application/json

{
  "examId": 1,
  "studentName": "Nguyen Van A",
  "studentOrganization": "University ABC"
}

# Nộp câu trả lời
POST http://localhost:8080/test-sessions/1/submit-answer?questionId=1
Content-Type: multipart/form-data

audio=@recording.wav

# Hoàn thành bài thi
POST http://localhost:8080/test-sessions/1/complete
```

## ✨ Tính Năng

### Tính Năng Admin/Giáo Viên

- ✅ **Quản lý người dùng**: Tạo, cập nhật, vô hiệu hóa (chỉ Admin)
- ✅ **Ngân hàng câu hỏi**: CRUD với phân loại level
- ✅ **Câu trả lời mẫu**: Nhiều câu trả lời mẫu cho mỗi câu hỏi với thang điểm
- ✅ **Tạo đề thi**: Chọn thủ công hoặc tạo ngẫu nhiên
- ✅ **Tìm kiếm nâng cao**: Tìm kiếm đa tiêu chí cho Questions, Exams, Test Sessions
- ✅ **Thao tác hàng loạt**: Xóa nhiều câu hỏi, cập nhật trạng thái nhiều kỳ thi
- ✅ **Dashboard thống kê**: Phân tích và số liệu hiệu suất thời gian thực
- ✅ **Xuất CSV**: Xuất kết quả thi và báo cáo
- ✅ **Audit Logs**: Theo dõi xử lý AI (Whisper + Qwen)

### Tính Năng Học Sinh

- ✅ **Không cần đăng ký**: Nhập tên và tổ chức để bắt đầu
- ✅ **Ghi âm**: Ghi âm câu trả lời trực tiếp
- ✅ **Phản hồi thời gian thực**: Chấm điểm ngay sau khi nộp
- ✅ **Phản hồi chi tiết**: Giải thích và gợi ý do AI tạo
- ✅ **Theo dõi tiến độ**: Xem câu đã trả lời/chưa trả lời
- ✅ **Báo cáo cuối**: Điểm tổng với chi tiết từng phần

### Tính Năng Kỹ Thuật

- ✅ **JWT Authentication**: Xác thực an toàn dựa trên token
- ✅ **DTO Pattern**: Tách biệt rõ ràng giữa entities và API contracts
- ✅ **Soft Delete**: Bảo toàn tính toàn vẹn dữ liệu với deletedAt pattern
- ✅ **Pagination**: Tải dữ liệu hiệu quả với Spring Data Pageable
- ✅ **Validation**: Jakarta Bean Validation cho request data
- ✅ **Error Handling**: Phản hồi lỗi nhất quán
- ✅ **CORS Support**: CORS có thể cấu hình cho frontend
- ✅ **File Upload**: Xử lý file audio an toàn
- ✅ **Async Processing**: Xử lý AI không chặn (non-blocking)
- ✅ **Transaction Management**: Tuân thủ ACID với @Transactional

## ⚙️ Cấu Hình

### Application Properties

File: `src/main/resources/application.properties`

```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/ai_speaking
spring.datasource.username=root
spring.datasource.password=your_password

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# File Upload
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
file.upload.dir=./uploads/audio

# AI Services
ai.whisper.url=http://localhost:5000
ai.qwen.url=http://localhost:5001
ai.request.timeout=60000

# JWT Security
jwt.secret=YourSuperSecretKeyForJWTTokenGenerationChangeThisInProduction
jwt.expiration=86400000

# CORS
cors.allowed.origins=http://localhost:3000,http://localhost:3001,http://localhost:4200
```

### Tùy Chọn Model Whisper

Sửa file `whisper_server.py`:

```python
# Kích thước model: tiny, base, small, medium, large
# Lớn hơn = chính xác hơn nhưng chậm hơn
model = whisper.load_model("base")  # Đổi sang "small" hoặc "medium"

# Sử dụng GPU nếu có
device = "cuda" if torch.cuda.is_available() else "cpu"
```

**So sánh Model:**

| Model | Kích thước | Tốc độ | Độ chính xác |
|-------|-----------|---------|--------------|
| tiny  | ~40MB | Rất nhanh | Tốt |
| base  | ~150MB | Nhanh | Tốt hơn |
| small | ~500MB | Trung bình | Rất tốt |
| medium| ~1.5GB | Chậm | Xuất sắc |
| large | ~3GB | Rất chậm | Tốt nhất |

### Tùy Chọn Model Qwen

Sửa file `qwen_server.py`:

```python
# Tùy chọn model:
# - Qwen/Qwen2.5-0.5B-Instruct (nhanh nhất, ~500MB)
# - Qwen/Qwen2.5-1.5B-Instruct (cân bằng, ~1.5GB)
# - Qwen/Qwen2.5-3B-Instruct (chất lượng tốt nhất, ~3GB)

model_name = "Qwen/Qwen2.5-0.5B-Instruct"

# Tham số generation
max_new_tokens = 512  # Tăng để feedback dài hơn
temperature = 0.7     # 0.1-1.0, cao hơn = sáng tạo hơn
```

### Tùy Chỉnh Prompt Chấm Điểm

Hệ thống sử dụng prompt chi tiết trong `qwen_server.py` với:

- **Tiêu chí chấm điểm rõ ràng**:
  - Nội dung (40%)
  - Ngữ pháp (30%)
  - Từ vựng (20%)
  - Phát âm & Độ trôi chảy (10%)
- **So sánh với câu trả lời mẫu**: AI sẽ so sánh câu trả lời với các mẫu được cung cấp
- **Phản hồi bằng tiếng Việt**: Feedback chi tiết bằng tiếng Việt

## 📚 Cấu Trúc Project

```
backend/
├── src/main/java/com/aispeaking/
│   ├── config/           # Các class cấu hình
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
│   ├── dto/              # Data Transfer Objects (15+)
│   │   ├── Request DTOs (User, Question, Exam, Session)
│   │   └── Response DTOs (với phương thức from() factory)
│   ├── entity/           # JPA Entities (9)
│   │   ├── BaseEntity.java
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
├── API_DOCUMENTATION.md  # Tài liệu API đầy đủ
├── start-all.bat         # Script khởi động tất cả
└── pom.xml               # Maven dependencies
```

## 🔐 Lưu Ý Bảo Mật

- 🔒 Mật khẩu được mã hóa bằng BCrypt
- 🎫 JWT tokens hết hạn sau 24 giờ (có thể cấu hình)
- 🚫 Soft delete ngăn mất dữ liệu
- ✅ CORS chỉ cho phép các origins được cấu hình
- 🔑 Các thao tác admin yêu cầu role ADMIN
- 📝 Audit logs theo dõi tất cả xử lý AI

## 🚀 Mẹo Hiệu Suất

1. **Database**: Thêm indexes cho các trường thường truy vấn
2. **AI Models**: Sử dụng kích thước model phù hợp với phần cứng
3. **Caching**: Cân nhắc Redis cho session/token caching
4. **File Storage**: Sử dụng cloud storage (S3/Azure) cho production
5. **Load Balancing**: Sử dụng nhiều AI server instances
6. **Monitoring**: Thêm application performance monitoring (APM)

## 📖 Tài Liệu Bổ Sung

- **Chi tiết API**: Xem [API_DOCUMENTATION.md](API_DOCUMENTATION.md)
- **Database Schema**: Kiểm tra `database/schema.sql`
- **Phân quyền**: ADMIN (toàn quyền), TEACHER (quản lý câu hỏi, kỳ thi, thống kê)

## 👥 Tác Giả & Credits

- **Backend Framework**: Spring Boot 3.5.9 + Spring Security + Spring Data JPA
- **AI Models**:
  - OpenAI Whisper (Speech-to-Text)
  - Alibaba Qwen 2.5 (Language Model cho chấm điểm)
- **Database**: MySQL 8.0
- **Authentication**: JWT (JSON Web Tokens)
- **Build Tool**: Maven
- **Java Version**: 21 (LTS)

## 📄 License

MIT License - Tự do sử dụng project này cho mục đích học tập và phát triển.

---

**Được xây dựng với ❤️ sử dụng Spring Boot, Whisper AI, và Qwen AI**


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
- `GET /questions/search` - Advanced search (level, createdBy, dates)
- `GET /questions/{id}` - Get question by ID
- `POST /questions` - Create new question
- `PUT /questions/{id}` - Update question
- `DELETE /questions/{id}` - Soft delete question

**Filter Parameters:**

- `level`: EASY, HARD
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

## 💡 Usage Examples

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
  "studentName": "John Doe",
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
GET /questions/search?level=MEDIUM&createdAfter=2026-01-01&page=0&size=20
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
- ✅ **Question Bank**: CRUD operations with level classification
- ✅ **Sample Answers**: Multiple sample answers per question with scoring rubric
- ✅ **Exam Creation**: Manual selection or random generation
- ✅ **Advanced Filtering**: Multi-criteria search for Questions, Exams, Test Sessions
- ✅ **Bulk Operations**: Delete multiple questions, update multiple exam statuses
- ✅ **Statistics Dashboard**: Real-time analytics and performance metrics
- ✅ **CSV Export**: Export test results and exam reports
- ✅ **Audit Logs**: Track AI processing (Whisper + Qwen)

### Student Features

- ✅ **No Registration Required**: Enter name and organization to start
- ✅ **Audio Recording**: Record answers directly in browser
- ✅ **Real-time Feedback**: Instant scoring after submit
- ✅ **Detailed Feedback**: AI-generated explanations and suggestions
- ✅ **Progress Tracking**: See answered/pending questions
- ✅ **Final Report**: Overall score with detailed breakdown

### Technical Features

- ✅ **JWT Authentication**: Secure token-based authentication
- ✅ **DTO Pattern**: Clean separation between entities and API contracts
- ✅ **Soft Delete**: Preserve data integrity with deletedAt pattern
- ✅ **Pagination**: Efficient data loading with Spring Data Pageable
- ✅ **Validation**: Jakarta Bean Validation for request data
- ✅ **Error Handling**: Consistent error responses
- ✅ **CORS Support**: Configurable CORS for frontend integration
- ✅ **File Upload**: Secure audio file handling
- ✅ **Async Processing**: Non-blocking AI operations
- ✅ **Transaction Management**: ACID compliance with @Transactional

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
