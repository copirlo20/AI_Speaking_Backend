# AI Speaking Test System

Hệ thống kiểm tra speaking tự động sử dụng Local AI (Whisper + Qwen) chấm điểm.

## Tổng quan

Hệ thống bao gồm:
- **Backend (Spring Boot)**: API server quản lý câu hỏi, đề thi, bài thi
- **Whisper Server (Python)**: Chuyển đổi giọng nói thành văn bản (Speech-to-Text)
- **Qwen Server (Python)**: Chấm điểm và nhận xét bài thi (LLM Scoring)
- **Database (MySQL)**: Lưu trữ dữ liệu

## Kiến trúc hệ thống

```
┌──────────────┐
│   Frontend   │
│  (React/Vue) │
└──────┬───────┘
       │
       ▼
┌──────────────────────────┐
│   Spring Boot Backend    │
│   Port: 8080             │
│   - REST API             │
│   - File Upload          │
│   - Business Logic       │
└─────┬──────────┬─────────┘
      │          │
      ▼          ▼
┌─────────┐  ┌──────────┐
│ Whisper │  │  Qwen    │
│ Server  │  │  Server  │
│ Port    │  │  Port    │
│ 5000    │  │  5001    │
└─────────┘  └──────────┘
```

## Luồng xử lý

1. Thí sinh nói → Thu âm → Upload file audio
2. Backend gửi audio → **Whisper Server** → Nhận text
3. Backend gửi text + câu hỏi + đáp án mẫu → **Qwen Server** → Nhận điểm + nhận xét
4. Backend lưu kết quả → Hiển thị cho thí sinh

## Cấu trúc Database

### Tables
- **users**: Quản lý user (Admin/Teacher)
- **questions**: Ngân hàng câu hỏi (có level, category)
- **sample_answers**: Đáp án mẫu cho từng câu hỏi
- **exams**: Đề thi (có thể tạo thủ công hoặc random)
- **exam_questions**: Câu hỏi trong đề thi
- **test_sessions**: Bài thi của thí sinh
- **test_answers**: Câu trả lời của thí sinh
- **ai_processing_logs**: Log xử lý AI (debug)

## Cài đặt

### Yêu cầu
- Java 17+
- Maven 3.6+
- MySQL 8.0+
- Python 3.9+
- CUDA (tùy chọn, cho GPU acceleration)

### 1. Cài đặt Database

```bash
# Tạo database
mysql -u root -p
CREATE DATABASE ai_speaking_db;

# Import schema
mysql -u root -p ai_speaking_db < database/schema.sql
```

### 2. Cài đặt Backend (Spring Boot)

```bash
cd backend

# Cấu hình database trong application.properties
# spring.datasource.username=root
# spring.datasource.password=your_password

# Build và chạy
mvn clean install
mvn spring-boot:run
```

Backend sẽ chạy tại: `http://localhost:8080`

### 3. Cài đặt Whisper Server

```bash
cd whisper_server

# Tạo virtual environment
python -m venv venv

# Activate (Windows)
venv\Scripts\activate
# Activate (Linux/Mac)
# source venv/bin/activate

# Cài đặt dependencies
pip install -r requirements.txt

# Chạy server
python whisper_server.py
```

Whisper Server sẽ chạy tại: `http://localhost:5000`

**Lưu ý**: Lần đầu chạy sẽ tải model Whisper (~150MB)

### 4. Cài đặt Qwen Server

```bash
cd qwen_server

# Tạo virtual environment
python -m venv venv

# Activate (Windows)
venv\Scripts\activate
# Activate (Linux/Mac)
# source venv/bin/activate

# Cài đặt dependencies
pip install -r requirements.txt

# Chạy server
python qwen_server.py
```

Qwen Server sẽ chạy tại: `http://localhost:5001`

**Lưu ý**: Lần đầu chạy sẽ tải model Qwen (~500MB-1GB)

## API Endpoints

### Questions API
- `GET /api/questions` - Lấy danh sách câu hỏi
- `POST /api/questions` - Tạo câu hỏi mới
- `PUT /api/questions/{id}` - Cập nhật câu hỏi
- `DELETE /api/questions/{id}` - Xóa câu hỏi
- `GET /api/questions/random?level=EASY&count=10` - Lấy câu hỏi random

### Exams API
- `GET /api/exams` - Lấy danh sách đề thi
- `POST /api/exams` - Tạo đề thi mới
- `POST /api/exams/{id}/questions` - Thêm câu hỏi vào đề (thủ công)
- `POST /api/exams/{id}/generate-random` - Tạo đề random
- `GET /api/exams/{id}/questions` - Xem câu hỏi trong đề

### Test Sessions API
- `POST /api/test-sessions` - Bắt đầu bài thi
- `GET /api/test-sessions/{id}` - Xem thông tin bài thi
- `POST /api/test-sessions/{id}/submit-answer` - Nộp câu trả lời (audio)
- `POST /api/test-sessions/{id}/complete` - Hoàn thành bài thi

### 🆕 User Management API
- `GET /api/users` - Lấy danh sách users
- `POST /api/users` - Tạo user mới
- `PUT /api/users/{id}` - Cập nhật user
- `PUT /api/users/{id}/change-password` - Đổi mật khẩu
- `PUT /api/users/{id}/toggle-status` - Bật/tắt user
- `DELETE /api/users/{id}` - Xóa user

### 🆕 Statistics API
- `GET /api/statistics/dashboard` - Thống kê tổng quan
- `GET /api/statistics/questions/by-level` - Câu hỏi theo level
- `GET /api/statistics/exams/by-status` - Đề thi theo status
- `GET /api/statistics/test-sessions/{id}` - Chi tiết bài thi
- `GET /api/statistics/exams/{id}` - Chi tiết đề thi
- `GET /api/statistics/test-sessions/recent` - Bài thi gần đây

### 🆕 Admin API
- `DELETE /api/admin/questions/bulk-delete` - Xóa nhiều câu hỏi
- `PUT /api/admin/exams/bulk-update-status` - Cập nhật status nhiều đề
- `GET /api/admin/test-sessions` - Xem tất cả bài thi
- `PUT /api/admin/test-sessions/{id}/cancel` - Hủy bài thi
- `GET /api/admin/health` - Kiểm tra hệ thống

### 🆕 Reports API
- `GET /api/reports/test-session/{id}/export-csv` - Export CSV
- `GET /api/reports/test-session/{id}/detailed` - Report chi tiết
- `GET /api/reports/exam/{examId}/export-csv` - Export tất cả bài thi

📖 **Chi tiết API**: Xem [API_DOCUMENTATION_ADMIN.md](API_DOCUMENTATION_ADMIN.md)

## Ví dụ sử dụng

### 1. Tạo câu hỏi với đáp án mẫu

```bash
POST /api/questions
{
  "content": "Describe your favorite place to visit",
  "level": "MEDIUM",
  "category": "Travel",
  "sampleAnswers": [
    {
      "content": "My favorite place is the beach. I love the sound of waves...",
      "score": 8.5,
      "explanation": "Good vocabulary and structure"
    }
  ]
}
```

### 2. Tạo đề thi random

```bash
# Tạo đề thi trước
POST /api/exams
{
  "name": "Speaking Test Level 1",
  "totalQuestions": 5,
  "durationMinutes": 15,
  "status": "ACTIVE"
}

# Sau đó generate random
POST /api/exams/1/generate-random
{
  "level": "MEDIUM",
  "count": 5
}
```

### 3. Thí sinh làm bài

```bash
# Bắt đầu bài thi
POST /api/test-sessions
{
  "examId": 1,
  "studentName": "Nguyen Van A",
  "studentOrganization": "ABC Company"
}

# Nộp câu trả lời
POST /api/test-sessions/1/submit-answer?questionId=1
Form data: audio=<file.wav>

# Hoàn thành
POST /api/test-sessions/1/complete
```

## Chức năng

### Dành cho Admin/Teacher
- ✅ CRUD câu hỏi (có level, category)
- ✅ CRUD đáp án mẫu
- ✅ Tạo đề thi thủ công
- ✅ Tạo đề thi random (theo level, thời gian tạo, category)
- ✅ Xem danh sách bài thi
- ✅ Xem chi tiết điểm của thí sinh
- ✅ Quản lý user (Admin only)

### Dành cho Thí sinh
- ✅ Nhập thông tin (họ tên, đơn vị) - không cần tạo tài khoản
- ✅ Làm bài thi (thu âm câu trả lời)
- ✅ Tự động chấm điểm bằng AI
- ✅ Xem kết quả và nhận xét chi tiết

## Cấu hình nâng cao

### Whisper Model
Trong `whisper_server.py`, có thể đổi model:
```python
# Các lựa chọn: tiny, base, small, medium, large
model = whisper.load_model("base")  # Đổi thành "small" hoặc "medium" cho độ chính xác cao hơn
```

### Qwen Model
Trong `qwen_server.py`, có thể đổi model:
```python
# Có thể dùng: Qwen2.5-0.5B, Qwen2.5-1.5B, Qwen2.5-3B
model_name = "Qwen/Qwen2.5-0.5B-Instruct"
```

### System Prompt
Trong `AIProcessingService.java`, có thể tùy chỉnh prompt chấm điểm:
```java
private String buildScoringPrompt(Question question, List<SampleAnswer> sampleAnswers) {
    // Customize scoring criteria here
}
```

## Troubleshooting

### Backend không kết nối được database
- Kiểm tra MySQL đã chạy chưa
- Kiểm tra username/password trong `application.properties`
- Tạo database: `CREATE DATABASE ai_speaking_db;`

### Whisper Server lỗi
- Cài ffmpeg: `pip install ffmpeg-python` hoặc tải từ https://ffmpeg.org/
- Kiểm tra Python version >= 3.9
- Nếu lỗi CUDA: Cài torch CPU version

### Qwen Server chạy chậm
- Giảm `max_new_tokens` trong code
- Dùng model nhỏ hơn (0.5B thay vì 1.5B)
- Nếu có GPU: Cài CUDA và PyTorch GPU version

### Upload file lỗi
- Kiểm tra `file.upload.dir` trong `application.properties`
- Tạo thư mục `uploads/audio` trong project
- Kiểm tra quyền ghi file

## Tác giả
- Backend: Spring Boot + JPA + MySQL
- AI Services: Whisper (OpenAI) + Qwen (Alibaba)
- Database: MySQL 8.0

## License
MIT License
