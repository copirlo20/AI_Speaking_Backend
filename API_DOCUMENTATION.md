# Tài Liệu API - Hệ Thống Thi Speaking

## Mục Lục
1. [Authentication](#1-authentication)
2. [User Management](#2-user-management)
3. [Question Management](#3-question-management)
4. [Exam Management](#4-exam-management)
5. [Test Session](#5-test-session)
6. [Statistics](#6-statistics)
7. [Reports](#7-reports)
8. [Admin Operations](#8-admin-operations)

---

## 1. Authentication

### 1.1 Đăng nhập
**Nhiệm vụ:** Xác thực người dùng và trả về JWT token

**URL:** `POST /auth/login`

**Quyền:** PUBLIC (không cần xác thực)

**Request:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response Success (200 OK):**
```json
{
  "token": "eyJhbGc...",
  "id": 1,
  "username": "admin",
  "fullName": "Administrator",
  "role": "ADMIN",
  "isActive": true,
  "message": "Login successful"
}
```

**Response Error (401 Unauthorized):**
```json
{
  "message": "Invalid username or password"
}
```

---

### 1.2 Đăng ký tài khoản giáo viên
**Nhiệm vụ:** Tạo tài khoản mới với role TEACHER

**URL:** `POST /auth/register`

**Quyền:** PUBLIC (không cần xác thực)

**Request:**
```json
{
  "username": "teacher01",
  "password": "password123",
  "fullName": "Nguyen Van A"
}
```

**Response Success (201 Created):**
```json
{
  "id": 2,
  "username": "teacher01",
  "fullName": "Nguyen Van A",
  "role": "TEACHER",
  "isActive": true,
  "message": "Registration successful"
}
```

**Response Error (400 Bad Request):**
```json
{
  "message": "Username already exists"
}
```

---

### 1.3 Kiểm tra username tồn tại
**Nhiệm vụ:** Kiểm tra xem username đã được sử dụng chưa

**URL:** `GET /auth/check-username/{username}`

**Quyền:** PUBLIC (không cần xác thực)

**Response:**
```json
{
  "exists": true
}
```

---

## 2. User Management
**Quyền:** ADMIN only

### 2.1 Lấy danh sách người dùng
**Nhiệm vụ:** Lấy tất cả người dùng với phân trang

**URL:** `GET /users?page=0&size=10&sort=id,desc`

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "username": "admin",
      "fullName": "Administrator",
      "role": "ADMIN",
      "isActive": true,
      "createdAt": "2026-01-15T10:00:00",
      "updatedAt": "2026-01-15T10:00:00"
    }
  ],
  "totalElements": 50,
  "totalPages": 5,
  "size": 10,
  "number": 0
}
```

---

### 2.2 Lấy thông tin người dùng theo ID
**Nhiệm vụ:** Lấy chi tiết một người dùng

**URL:** `GET /users/{id}`

**Response:**
```json
{
  "id": 1,
  "username": "admin",
  "fullName": "Administrator",
  "role": "ADMIN",
  "isActive": true,
  "createdAt": "2026-01-15T10:00:00",
  "updatedAt": "2026-01-15T10:00:00"
}
```

---

### 2.3 Lấy thông tin người dùng theo username
**Nhiệm vụ:** Lấy chi tiết người dùng bằng username

**URL:** `GET /users/username/{username}`

**Response:** Giống như 2.2

---

### 2.4 Tạo người dùng mới
**Nhiệm vụ:** Admin tạo người dùng mới (có thể chọn role)

**URL:** `POST /users`

**Request:**
```json
{
  "username": "newuser",
  "password": "password123",
  "fullName": "New User Name",
  "role": "TEACHER"
}
```

**Response:** Giống như 2.2

---

### 2.5 Cập nhật thông tin người dùng
**Nhiệm vụ:** Cập nhật thông tin của người dùng

**URL:** `PUT /users/{id}`

**Request:**
```json
{
  "fullName": "Updated Full Name",
  "role": "TEACHER",
  "isActive": true
}
```

**Response:** Giống như 2.2

---

### 2.6 Đổi mật khẩu
**Nhiệm vụ:** Thay đổi mật khẩu người dùng

**URL:** `PUT /users/{id}/change-password`

**Request:**
```json
{
  "newPassword": "newPassword123"
}
```

**Response:** `200 OK` (empty body)

---

### 2.7 Bật/Tắt trạng thái người dùng
**Nhiệm vụ:** Chuyển đổi trạng thái active/inactive

**URL:** `PUT /users/{id}/toggle-status`

**Response:** `200 OK` (empty body)

---

### 2.8 Xóa người dùng
**Nhiệm vụ:** Xóa người dùng khỏi hệ thống

**URL:** `DELETE /users/{id}`

**Response:** `204 No Content`

---

### 2.9 Đếm số người dùng active
**Nhiệm vụ:** Lấy tổng số người dùng đang hoạt động

**URL:** `GET /users/count/active`

**Response:**
```json
{
  "count": 42
}
```

---

## 3. Question Management
**Quyền:** TEACHER + ADMIN

### 3.1 Lấy danh sách câu hỏi
**Nhiệm vụ:** Lấy tất cả câu hỏi với phân trang

**URL:** `GET /questions?page=0&size=10&sort=id,desc`

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "content": "Describe your hometown",
      "level": "EASY",
      "createdByUsername": "admin",
      "createdAt": "2026-01-15T10:00:00",
      "updatedAt": "2026-01-15T10:00:00"
    }
  ],
  "totalElements": 100,
  "totalPages": 10,
  "size": 10,
  "number": 0
}
```

---

### 3.2 Lấy câu hỏi theo ID
**Nhiệm vụ:** Lấy chi tiết một câu hỏi

**URL:** `GET /questions/{id}`

**Response:**
```json
{
  "id": 1,
  "content": "Describe your hometown",
  "level": "EASY",
  "createdByUsername": "admin",
  "createdAt": "2026-01-15T10:00:00",
  "updatedAt": "2026-01-15T10:00:00"
}
```

---

### 3.3 Tìm kiếm câu hỏi
**Nhiệm vụ:** Tìm kiếm câu hỏi theo các tiêu chí

**URL:** `GET /questions/search?level=EASY&createdByUsername=admin&fromDate=2026-01-01T00:00:00`

**Query Parameters:**
- `level`: EASY hoặc HARD
- `createdByUsername`: Username của người tạo
- `fromDate`: Từ ngày (ISO format)
- `toDate`: Đến ngày (ISO format)

**Response:** Giống như 3.1

---

### 3.4 Lấy câu hỏi ngẫu nhiên
**Nhiệm vụ:** Lấy số lượng câu hỏi ngẫu nhiên theo level

**URL:** `GET /questions/random?level=EASY&count=10`

**Response:** Giống như 3.1

---

### 3.5 Tạo câu hỏi mới
**Nhiệm vụ:** Thêm câu hỏi mới vào ngân hàng câu hỏi

**URL:** `POST /questions`

**Request:**
```json
{
  "content": "Describe your hometown",
  "level": "EASY"
}
```

**Response:** Giống như 3.2

---

### 3.6 Cập nhật câu hỏi
**Nhiệm vụ:** Sửa thông tin câu hỏi

**URL:** `PUT /questions/{id}`

**Request:**
```json
{
  "content": "Updated question content",
  "level": "HARD"
}
```

**Response:** Giống như 3.2

---

### 3.7 Xóa câu hỏi
**Nhiệm vụ:** Xóa câu hỏi khỏi hệ thống

**URL:** `DELETE /questions/{id}`

**Response:** `204 No Content`

---

## 4. Exam Management
**Quyền:** TEACHER + ADMIN

### 4.1 Lấy danh sách kỳ thi
**Nhiệm vụ:** Lấy tất cả kỳ thi với phân trang

**URL:** `GET /exams?page=0&size=10&sort=id,desc`

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "name": "English Speaking Test Level 1",
      "description": "Basic speaking test",
      "durationMinutes": 60,
      "totalQuestions": 10,
      "status": "ACTIVE",
      "createdByUsername": "admin",
      "createdAt": "2026-01-15T10:00:00",
      "updatedAt": "2026-01-15T10:00:00"
    }
  ],
  "totalElements": 20,
  "totalPages": 2,
  "size": 10,
  "number": 0
}
```

---

### 4.2 Tìm kiếm kỳ thi
**Nhiệm vụ:** Tìm kiếm kỳ thi theo các tiêu chí

**URL:** `GET /exams/search?status=ACTIVE&createdByUsername=admin&fromDate=2026-01-01T00:00:00`

**Query Parameters:**
- `status`: ACTIVE, INACTIVE, DRAFT
- `createdByUsername`: Username của người tạo
- `fromDate`: Từ ngày (ISO format)
- `toDate`: Đến ngày (ISO format)

**Response:** Giống như 4.1

---

### 4.3 Lấy kỳ thi theo ID
**Nhiệm vụ:** Lấy chi tiết một kỳ thi

**URL:** `GET /exams/{id}`

**Response:**
```json
{
  "id": 1,
  "name": "English Speaking Test Level 1",
  "description": "Basic speaking test for beginners",
  "durationMinutes": 60,
  "totalQuestions": 10,
  "status": "ACTIVE",
  "createdByUsername": "admin",
  "createdAt": "2026-01-15T10:00:00",
  "updatedAt": "2026-01-15T10:00:00"
}
```

---

### 4.4 Tạo kỳ thi mới
**Nhiệm vụ:** Tạo kỳ thi mới

**URL:** `POST /exams`

**Request:**
```json
{
  "name": "English Speaking Test Level 1",
  "description": "Basic speaking test for beginners",
  "durationMinutes": 60,
  "status": "DRAFT"
}
```

**Response:** Giống như 4.3

---

### 4.5 Cập nhật kỳ thi
**Nhiệm vụ:** Sửa thông tin kỳ thi

**URL:** `PUT /exams/{id}`

**Request:**
```json
{
  "name": "Updated Exam Name",
  "description": "Updated description",
  "durationMinutes": 90,
  "status": "ACTIVE"
}
```

**Response:** Giống như 4.3

---

### 4.6 Xóa kỳ thi
**Nhiệm vụ:** Xóa kỳ thi khỏi hệ thống

**URL:** `DELETE /exams/{id}`

**Response:** `204 No Content`

---

### 4.7 Thêm câu hỏi vào kỳ thi
**Nhiệm vụ:** Thêm danh sách câu hỏi vào kỳ thi

**URL:** `POST /exams/{id}/questions`

**Request:**
```json
{
  "questionIds": [1, 2, 3, 4, 5]
}
```

**Response:** `200 OK` (empty body)

---

### 4.8 Tạo đề thi ngẫu nhiên
**Nhiệm vụ:** Tự động tạo đề thi với câu hỏi ngẫu nhiên

**URL:** `POST /exams/{id}/generate-random`

**Request:**
```json
{
  "level": "EASY",
  "count": 10
}
```

**Response:** `200 OK` (empty body)

---

### 4.9 Lấy danh sách câu hỏi trong kỳ thi
**Nhiệm vụ:** Xem tất cả câu hỏi của một kỳ thi

**URL:** `GET /exams/{id}/questions`

**Response:**
```json
[
  {
    "id": 1,
    "questionId": 5,
    "questionContent": "Describe your hometown",
    "questionLevel": "EASY",
    "orderNumber": 1
  },
  {
    "id": 2,
    "questionId": 8,
    "questionContent": "What are your career goals?",
    "questionLevel": "HARD",
    "orderNumber": 2
  }
]
```

---

## 5. Test Session
**Quyền:** PUBLIC cho học sinh, TEACHER + ADMIN để quản lý

### 5.1 Tạo bài thi mới (PUBLIC)
**Nhiệm vụ:** Học sinh bắt đầu làm bài thi

**URL:** `POST /test-sessions`

**Quyền:** PUBLIC (không cần xác thực)

**Request:**
```json
{
  "examId": 1,
  "studentName": "Nguyen Van A",
  "studentOrganization": "University ABC"
}
```

**Response:**
```json
{
  "id": 1,
  "examId": 1,
  "examName": "English Speaking Test",
  "studentName": "Nguyen Van A",
  "studentOrganization": "University ABC",
  "status": "IN_PROGRESS",
  "totalScore": 0.0,
  "startedAt": "2026-01-15T14:30:00",
  "completedAt": null,
  "createdAt": "2026-01-15T14:30:00",
  "updatedAt": "2026-01-15T14:30:00"
}
```

---

### 5.2 Xem thông tin bài thi (PUBLIC)
**Nhiệm vụ:** Học sinh xem thông tin bài thi của mình

**URL:** `GET /test-sessions/{id}`

**Quyền:** PUBLIC (không cần xác thực)

**Response:** Giống như 5.1

---

### 5.3 Lấy danh sách bài thi (TEACHER + ADMIN)
**Nhiệm vụ:** Giáo viên xem tất cả bài thi

**URL:** `GET /test-sessions?page=0&size=10&sort=id,desc`

**Quyền:** TEACHER + ADMIN

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "examId": 1,
      "examName": "English Speaking Test",
      "studentName": "Nguyen Van A",
      "studentOrganization": "University ABC",
      "status": "COMPLETED",
      "totalScore": 85.5,
      "startedAt": "2026-01-15T10:00:00",
      "completedAt": "2026-01-15T11:30:00",
      "createdAt": "2026-01-15T10:00:00",
      "updatedAt": "2026-01-15T11:30:00"
    }
  ],
  "totalElements": 50,
  "totalPages": 5,
  "size": 10,
  "number": 0
}
```

---

### 5.4 Tìm kiếm bài thi (TEACHER + ADMIN)
**Nhiệm vụ:** Tìm kiếm bài thi theo điều kiện

**URL:** `GET /test-sessions/search?examId=1&studentName=Nguyen&status=COMPLETED`

**Quyền:** TEACHER + ADMIN

**Query Parameters:**
- `examId`: ID kỳ thi
- `studentName`: Tên học sinh (tìm kiếm partial match)
- `status`: IN_PROGRESS, COMPLETED, CANCELLED
- `minScore`: Điểm tối thiểu
- `maxScore`: Điểm tối đa
- `fromDate`: Từ ngày (ISO format)
- `toDate`: Đến ngày (ISO format)

**Response:** Giống như 5.3

---

### 5.5 Xem câu trả lời của bài thi (PUBLIC)
**Nhiệm vụ:** Xem tất cả câu trả lời trong bài thi

**URL:** `GET /test-sessions/{id}/answers`

**Quyền:** PUBLIC (không cần xác thực)

**Response:**
```json
[
  {
    "id": 1,
    "testSessionId": 1,
    "questionId": 5,
    "questionContent": "Describe your hometown",
    "transcribedText": "My hometown is Ha Noi. It is a beautiful city...",
    "score": 8.5,
    "feedback": "Phát âm: 9/10. Ngữ pháp tốt, từ vựng phong phú.",
    "processingStatus": "COMPLETED",
    "answeredAt": "2026-01-15T14:35:00",
    "createdAt": "2026-01-15T14:35:00",
    "updatedAt": "2026-01-15T14:36:00"
  }
]
```

---

### 5.6 Nộp câu trả lời (PUBLIC)
**Nhiệm vụ:** Học sinh nộp file audio trả lời câu hỏi

**URL:** `POST /test-sessions/{id}/submit-answer`

**Quyền:** PUBLIC (không cần xác thực)

**Request:** Multipart Form Data
- `questionId`: Long (ID câu hỏi)
- `audio`: MultipartFile (file audio: wav, mp3, m4a, max 50MB)

**Response:** Một object TestAnswerResponse (giống item trong 5.5)

**Lưu ý:** 
- Việc xử lý âm thanh (transcribe và score) diễn ra bất đồng bộ
- Ban đầu `processingStatus` sẽ là `PENDING`
- Sau khi xử lý xong sẽ chuyển thành `COMPLETED` hoặc `FAILED`
- Client cần poll lại để lấy kết quả cuối cùng

---

### 5.7 Hoàn thành bài thi (PUBLIC)
**Nhiệm vụ:** Học sinh hoàn thành và nộp bài thi

**URL:** `POST /test-sessions/{id}/complete`

**Quyền:** PUBLIC (không cần xác thực)

**Response:** `200 OK` (empty body)

---

## 6. Statistics
**Quyền:** TEACHER + ADMIN

### 6.1 Thống kê tổng quan
**Nhiệm vụ:** Lấy các số liệu thống kê cho dashboard

**URL:** `GET /statistics/dashboard`

**Response:**
```json
{
  "totalQuestions": 150,
  "totalExams": 25,
  "totalTestSessions": 500,
  "completedTestSessions": 450,
  "averageScore": 75.5,
  "totalUsers": 50,
  "activeUsers": 45
}
```

---

### 6.2 Thống kê câu hỏi theo độ khó
**Nhiệm vụ:** Đếm số câu hỏi theo từng level

**URL:** `GET /statistics/questions/by-level`

**Response:**
```json
{
  "EASY": 80,
  "HARD": 70
}
```

---

### 6.3 Thống kê kỳ thi theo trạng thái
**Nhiệm vụ:** Đếm số kỳ thi theo từng trạng thái

**URL:** `GET /statistics/exams/by-status`

**Response:**
```json
{
  "ACTIVE": 15,
  "INACTIVE": 5,
  "DRAFT": 5
}
```

---

### 6.4 Thống kê bài thi theo trạng thái
**Nhiệm vụ:** Đếm số bài thi theo từng trạng thái

**URL:** `GET /statistics/test-sessions/by-status`

**Response:**
```json
{
  "IN_PROGRESS": 50,
  "COMPLETED": 450,
  "CANCELLED": 5
}
```

---

### 6.5 Thống kê chi tiết bài thi
**Nhiệm vụ:** Lấy thống kê chi tiết của một bài thi

**URL:** `GET /statistics/test-sessions/{id}`

**Response:**
```json
{
  "totalQuestions": 10,
  "answeredQuestions": 8,
  "pendingQuestions": 2,
  "averageScore": 7.5,
  "maxScore": 9.5,
  "minScore": 5.0,
  "completionRate": 80.0
}
```

---

### 6.6 Thống kê chi tiết kỳ thi
**Nhiệm vụ:** Lấy thống kê chi tiết của một kỳ thi

**URL:** `GET /statistics/exams/{id}`

**Response:**
```json
{
  "totalAttempts": 50,
  "completedAttempts": 45,
  "completionRate": 90.0,
  "averageScore": 75.5,
  "passRate": 80.0,
  "maxScore": 95.0,
  "minScore": 50.0
}
```

---

### 6.7 Danh sách bài thi gần đây
**Nhiệm vụ:** Lấy danh sách bài thi mới nhất

**URL:** `GET /statistics/test-sessions/recent?limit=10`

**Response:**
```json
[
  {
    "sessionId": 100,
    "studentName": "Nguyen Van A",
    "examName": "English Speaking Test",
    "score": 85.5,
    "status": "COMPLETED",
    "completedAt": "2026-01-15T11:30:00"
  }
]
```

---

### 6.8 Thống kê theo khoảng thời gian
**Nhiệm vụ:** Lấy thống kê trong một khoảng thời gian

**URL:** `GET /statistics/by-date-range?startDate=2026-01-01T00:00:00&endDate=2026-01-31T23:59:59`

**Response:**
```json
{
  "questionsCreated": 20,
  "examsCreated": 5,
  "testsTaken": 100,
  "testsCompleted": 90,
  "averageScore": 75.5,
  "dateRange": {
    "from": "2026-01-01T00:00:00",
    "to": "2026-01-31T23:59:59"
  }
}
```

---

## 7. Reports
**Quyền:** TEACHER + ADMIN

### 7.1 Xuất báo cáo bài thi CSV
**Nhiệm vụ:** Tải xuống báo cáo bài thi dạng CSV

**URL:** `GET /reports/test-session/{id}/export-csv`

**Response:** File CSV download với header:
```
Content-Type: text/csv
Content-Disposition: attachment; filename="test-session-{id}.csv"
```

**CSV Format:**
```
Question,Transcribed Text,Score,Feedback,Status
"Describe your hometown","My hometown is Ha Noi...",8.5,"Good pronunciation",COMPLETED
```

---

### 7.2 Báo cáo chi tiết bài thi
**Nhiệm vụ:** Xem báo cáo chi tiết của một bài thi

**URL:** `GET /reports/test-session/{id}/detailed`

**Response:**
```json
{
  "sessionId": 1,
  "studentName": "Nguyen Van A",
  "studentOrganization": "University ABC",
  "examName": "English Speaking Test",
  "totalScore": 85.5,
  "status": "COMPLETED",
  "startedAt": "2026-01-15T10:00:00",
  "completedAt": "2026-01-15T11:30:00",
  "answers": [
    {
      "questionId": 5,
      "questionContent": "Describe your hometown",
      "questionLevel": "EASY",
      "transcribedText": "My hometown is Ha Noi...",
      "score": 8.5,
      "feedback": "Good pronunciation and grammar",
      "status": "COMPLETED",
      "answeredAt": "2026-01-15T10:15:00"
    }
  ],
  "completedAnswers": 10,
  "totalQuestions": 10,
  "completionRate": 100.0
}
```

---

### 7.3 Xuất báo cáo tất cả bài thi của kỳ thi
**Nhiệm vụ:** Tải xuống báo cáo tất cả bài thi trong một kỳ thi

**URL:** `GET /reports/exam/{examId}/export-csv`

**Response:** File CSV download với header:
```
Content-Type: text/csv
Content-Disposition: attachment; filename="exam-{examId}-sessions.csv"
```

**CSV Format:**
```
Session ID,Student Name,Organization,Total Score,Status,Started At,Completed At
1,"Nguyen Van A","University ABC",85.5,COMPLETED,2026-01-15 10:00:00,2026-01-15 11:30:00
```

---

## 8. Admin Operations
**Quyền:** ADMIN only

### 8.1 Xóa nhiều câu hỏi cùng lúc
**Nhiệm vụ:** Xóa hàng loạt câu hỏi

**URL:** `DELETE /admin/questions/bulk-delete`

**Request:**
```json
{
  "questionIds": [1, 2, 3, 4, 5]
}
```

**Response:**
```json
{
  "requested": 5,
  "deleted": 5
}
```

---

### 8.2 Cập nhật trạng thái nhiều kỳ thi
**Nhiệm vụ:** Thay đổi trạng thái của nhiều kỳ thi cùng lúc

**URL:** `PUT /admin/exams/bulk-update-status`

**Request:**
```json
{
  "examIds": [1, 2, 3],
  "status": "ACTIVE"
}
```

**Response:**
```json
{
  "requested": 3,
  "updated": 3
}
```

---

### 8.3 Xem tất cả bài thi (Admin)
**Nhiệm vụ:** Admin xem và quản lý tất cả bài thi

**URL:** `GET /admin/test-sessions?status=COMPLETED&examId=1&page=0&size=10`

**Query Parameters:**
- `status`: Lọc theo trạng thái (IN_PROGRESS, COMPLETED, CANCELLED)
- `examId`: Lọc theo ID kỳ thi
- Pagination: page, size, sort

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "examId": 1,
      "examName": "English Speaking Test",
      "studentName": "Nguyen Van A",
      "studentOrganization": "University ABC",
      "status": "COMPLETED",
      "totalScore": 85.5,
      "startedAt": "2026-01-15T10:00:00",
      "completedAt": "2026-01-15T11:30:00",
      "createdAt": "2026-01-15T10:00:00",
      "updatedAt": "2026-01-15T11:30:00"
    }
  ],
  "totalElements": 50,
  "totalPages": 5,
  "size": 10,
  "number": 0
}
```

---

### 8.4 Hủy bài thi
**Nhiệm vụ:** Admin hủy một bài thi đang diễn ra

**URL:** `PUT /admin/test-sessions/{id}/cancel`

**Response:** `200 OK` (empty body)

---

### 8.5 Xóa bài thi
**Nhiệm vụ:** Admin xóa bài thi khỏi hệ thống

**URL:** `DELETE /admin/test-sessions/{id}`

**Response:** `204 No Content`

---

### 8.6 Kiểm tra sức khỏe hệ thống
**Nhiệm vụ:** Kiểm tra tình trạng hoạt động của hệ thống

**URL:** `GET /admin/health`

**Response:**
```json
{
  "database": "OK",
  "questionCount": 150,
  "timestamp": "2026-01-15T14:30:00",
  "status": "RUNNING"
}
```

---

### 8.7 Xem cấu hình hệ thống
**Nhiệm vụ:** Xem các thông số cấu hình hiện tại

**URL:** `GET /admin/config`

**Response:**
```json
{
  "maxFileSize": "50MB",
  "allowedAudioFormats": ["wav", "mp3", "m4a"],
  "aiWhisperUrl": "http://localhost:5000",
  "aiQwenUrl": "http://localhost:5001"
}
```

---

## Ghi Chú

### Authentication Header
Hầu hết các API (trừ PUBLIC) yêu cầu JWT token trong header:
```
Authorization: Bearer <your-jwt-token>
```

### Status Codes
- `200 OK`: Thành công
- `201 Created`: Tạo mới thành công
- `204 No Content`: Xóa thành công
- `400 Bad Request`: Dữ liệu không hợp lệ
- `401 Unauthorized`: Chưa đăng nhập hoặc token không hợp lệ
- `403 Forbidden`: Không có quyền truy cập
- `404 Not Found`: Không tìm thấy resource
- `500 Internal Server Error`: Lỗi server

### Roles và Quyền
- **ADMIN**: Toàn quyền trên hệ thống
  - Quản lý users (/users/**)
  - Tất cả chức năng của TEACHER
  - Các thao tác admin đặc biệt (/admin/**)
  
- **TEACHER**: Quản lý nội dung và xem báo cáo
  - Quản lý câu hỏi (/questions/**)
  - Quản lý kỳ thi (/exams/**)
  - Xem bài thi của học sinh (/test-sessions - GET only)
  - Xem thống kê (/statistics/**)
  - Xuất báo cáo (/reports/**)
  
- **PUBLIC**: Không cần đăng nhập (học sinh làm bài)
  - Đăng nhập/đăng ký (/auth/**)
  - Tạo bài thi mới (/test-sessions - POST)
  - Xem bài thi của mình (/test-sessions/{id} - GET)
  - Nộp câu trả lời (/test-sessions/{id}/submit-answer)
  - Hoàn thành bài thi (/test-sessions/{id}/complete)

### Pagination
Hầu hết API danh sách hỗ trợ phân trang với các tham số:
- `page`: Số trang (bắt đầu từ 0)
- `size`: Số lượng items mỗi trang (default: 10)
- `sort`: Sắp xếp theo field, format: `field,direction` (ví dụ: `id,desc`, `createdAt,asc`)

### AI Processing
- **Whisper Server** (port 5000): Chuyển đổi audio thành text
- **Qwen Server** (port 5001): Chấm điểm và đưa ra feedback bằng tiếng Việt
- Processing diễn ra bất đồng bộ với timeout 60 giây
- Feedback bao gồm đánh giá về phát âm, ngữ pháp, từ vựng và nội dung

### File Upload
- Max file size: 50MB
- Supported formats: wav, mp3, m4a
- Upload directory: ./uploads/audio
- Content-Type: multipart/form-data
