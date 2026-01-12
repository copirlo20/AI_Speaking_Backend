# AI Speaking Test Backend

Ứng dụng backend cho hệ thống thi speaking tích hợp AI local sử dụng Spring Boot.

## Tính năng chính

- 🔐 **Xác thực và Phân quyền**: JWT authentication, đăng ký/đăng nhập người dùng
- 👤 **Quản lý người dùng**: Profile, thống kê điểm số trung bình
- 📝 **Quản lý câu hỏi**: CRUD operations, phân loại theo chủ đề, độ khó, loại câu hỏi
- 📋 **Quản lý bài thi**: Tạo bài thi với nhiều câu hỏi, theo cấp độ (A1-C2)
- 🎤 **Xử lý âm thanh**: Upload và lưu trữ file audio
- 🤖 **Tích hợp AI**:
  - Speech-to-text sử dụng Whisper (local)
  - **Chấm điểm tự động thông minh** với thuật toán AI tiên tiến
  - Đánh giá chi tiết 5 tiêu chí: Phát âm, Lưu loát, Ngữ pháp, Từ vựng, Nội dung
  - Phát hiện filler words (um, uh, like, you know...)
  - Phân tích tốc độ nói (words per minute)
  - Đánh giá độ đa dạng từ vựng (lexical diversity)
  - Kiểm tra lỗi ngữ pháp tự động
  - Feedback chi tiết và suggestions cải thiện cụ thể
  - Hỗ trợ fallback khi AI service offline
- 📊 **Kết quả và thống kê**: Lưu trữ và phân tích kết quả thi

## Công nghệ sử dụng

- **Backend Framework**: Spring Boot 3.4.1
- **Java Version**: 17
- **Database**: H2 (development), MySQL (production)
- **Security**: Spring Security + JWT
- **AI Integration**: 
  - Local Whisper API (Speech-to-Text)
  - Advanced Rule-Based Scoring Algorithm
  - Natural Language Processing
- **Build Tool**: Maven

## Tính năng AI Auto-Scoring

Hệ thống chấm điểm tự động sử dụng thuật toán AI tiên tiến để đánh giá speaking:

### 1. Pronunciation Scoring (Đánh giá Phát âm)
- Phát hiện từ ngữ phát âm sai
- Đánh giá độ rõ ràng của giọng nói
- Tính toán clarity score

### 2. Fluency Scoring (Đánh giá Lưu loát)
- Tính words per minute (WPM)
- Phát hiện filler words: um, uh, er, like, you know, etc.
- Đánh giá số lượng pauses
- Tốc độ nói tối ưu: 130-170 WPM

### 3. Grammar Scoring (Đánh giá Ngữ pháp)
- Phát hiện lỗi subject-verb agreement
- Kiểm tra câu không hoàn chỉnh
- Tính toán grammatical accuracy

### 4. Vocabulary Scoring (Đánh giá Từ vựng)
- Đếm unique words
- Tính lexical diversity (Type-Token Ratio)
- Phát hiện advanced words (từ nâng cao)
- Phân loại vocabulary level: BEGINNER, INTERMEDIATE, ADVANCED

### 5. Content Scoring (Đánh giá Nội dung)
- So sánh với sample answer
- Kiểm tra key topics được cover
- Đánh giá độ đầy đủ và liên quan
- Tính relevance và completeness score

### Feedback & Suggestions
- Feedback chi tiết cho từng tiêu chí
- Gợi ý cải thiện cụ thể và actionable
- Highlighting điểm mạnh và điểm yếu

## Cấu trúc dự án

```
src/main/java/com/aispeaking/
├── config/              # Configuration classes
├── controller/          # REST API endpoints
├── dto/                 # Data Transfer Objects
├── model/              # Entity models
├── repository/         # JPA repositories
├── security/           # Security & JWT
└── service/            # Business logic
```

## API Endpoints

### Authentication
- `POST /api/auth/register` - Đăng ký người dùng mới
- `POST /api/auth/login` - Đăng nhập

### Users
- `GET /api/users` - Lấy danh sách người dùng
- `GET /api/users/{id}` - Lấy thông tin người dùng
- `PUT /api/users/{id}` - Cập nhật thông tin
- `DELETE /api/users/{id}` - Xóa người dùng

### Questions
- `POST /api/questions` - Tạo câu hỏi mới
- `GET /api/questions` - Lấy tất cả câu hỏi
- `GET /api/questions/active` - Lấy câu hỏi active
- `GET /api/questions/{id}` - Lấy chi tiết câu hỏi
- `GET /api/questions/type/{type}` - Lọc theo loại
- `GET /api/questions/difficulty/{level}` - Lọc theo độ khó
- `PUT /api/questions/{id}` - Cập nhật câu hỏi
- `DELETE /api/questions/{id}` - Xóa câu hỏi

### Speaking Tests
- `POST /api/tests` - Tạo bài thi mới
- `GET /api/tests` - Lấy tất cả bài thi
- `GET /api/tests/active` - Lấy bài thi active
- `GET /api/tests/{id}` - Lấy chi tiết bài thi
- `GET /api/tests/level/{level}` - Lọc theo cấp độ
- `POST /api/tests/{id}/start` - Bắt đầu làm bài thi
- `PUT /api/tests/{id}` - Cập nhật bài thi
- `DELETE /api/tests/{id}` - Xóa bài thi

### Test Results
- `POST /api/results/submit` - Submit bài thi (upload audio)
- `GET /api/results` - Lấy tất cả kết quả
- `GET /api/results/{id}` - Lấy chi tiết kết quả
- `GET /api/results/user/{userId}` - Kết quả của người dùng
- `GET /api/results/test/{testId}` - Kết quả của bài thi
- `DELETE /api/results/{id}` - Xóa kết quả

## Cài đặt và chạy

### Yêu cầu
- JDK 17 trở lên
- Maven 3.6+
- (Optional) MySQL Server
- Local AI Service (Whisper API)

### Bước 1: Clone repository
```bash
cd "d:\AI Speaking"
```

### Bước 2: Cấu hình database (tùy chọn)
Mặc định ứng dụng sử dụng H2 in-memory database. Để sử dụng MySQL:

1. Tạo database MySQL:
```sql
CREATE DATABASE aispeakingdb;
```

2. Cập nhật `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/aispeakingdb
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

### Bước 3: Cấu hình AI Service
Cập nhật URL của Local AI service trong `application.properties`:
```properties
ai.service.url=http://localhost:5000
```

### Bước 4: Build project
```bash
mvn clean install
```

### Bước 5: Chạy ứng dụng
```bash
mvn spring-boot:run
```

Ứng dụng sẽ chạy tại: `http://localhost:8080`

### Bước 6: Truy cập H2 Console (development)
URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:aispeakingdb`
- Username: `sa`
- Password: (để trống)

## Cấu hình Local AI Service

### Cài đặt Whisper API (Python)

1. Tạo file `whisper_service.py`:
```python
from flask import Flask, request, jsonify
import whisper
import os

app = Flask(__name__)
model = whisper.load_model("base")

@app.route('/api/transcribe', methods=['POST'])
def transcribe():
    data = request.json
    audio_file = data.get('audio_file')
    
    result = model.transcribe(audio_file)
    return jsonify({'text': result['text']})

@app.route('/api/evaluate', methods=['POST'])
def evaluate():
    # Implement evaluation logic
    return jsonify({
        'pronunciation': 8.0,
        'fluency': 7.5,
        'grammar': 8.5,
        'vocabulary': 8.0,
        'content': 7.0
    })

@app.route('/api/feedback', methods=['POST'])
def feedback():
    # Implement feedback generation
    return jsonify({
        'feedback': 'Good performance! Keep practicing.'
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
```

2. Cài đặt dependencies:
```bash
pip install flask openai-whisper
```

3. Chạy service:
```bash
python whisper_service.py
```

## Testing API

### Sử dụng cURL

#### Đăng ký
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "fullName": "Test User"
  }'
```

#### Đăng nhập
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

#### Submit bài thi (với JWT token)
```bash
curl -X POST http://localhost:8080/api/results/submit \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "userId=1" \
  -F "testId=1" \
  -F "questionId=1" \
  -F "audioFile=@/path/to/audio.wav"
```

## Cấu trúc Database

### Tables
- `users` - Thông tin người dùng
- `questions` - Ngân hàng câu hỏi
- `speaking_tests` - Bài thi
- `test_questions` - Liên kết test và questions
- `test_results` - Kết quả thi và đánh giá

## Đóng góp

Mọi đóng góp đều được chào đón! Vui lòng tạo pull request hoặc báo cáo issues.

## License

MIT License

## Liên hệ

- Email: support@aispeaking.com
- Website: https://aispeaking.com
