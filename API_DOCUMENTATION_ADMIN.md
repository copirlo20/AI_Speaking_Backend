# API Documentation - Admin & Statistics

## 📊 Statistics APIs

### 1. Dashboard Statistics
```http
GET /api/statistics/dashboard
```
**Response:**
```json
{
  "totalQuestions": 150,
  "totalExams": 20,
  "activeExams": 12,
  "totalTestSessions": 500,
  "completedSessions": 450,
  "activeUsers": 15,
  "averageScore": 7.5
}
```

### 2. Questions by Level
```http
GET /api/statistics/questions/by-level
```
**Response:**
```json
{
  "EASY": 75,
  "HARD": 75
}
```

### 3. Exams by Status
```http
GET /api/statistics/exams/by-status
```
**Response:**
```json
{
  "ACTIVE": 12,
  "INACTIVE": 5,
  "DRAFT": 3
}
```

### 4. Test Sessions by Status
```http
GET /api/statistics/test-sessions/by-status
```
**Response:**
```json
{
  "IN_PROGRESS": 50,
  "COMPLETED": 450,
  "CANCELLED": 10
}
```

### 5. Test Session Statistics
```http
GET /api/statistics/test-sessions/{id}
```
**Response:**
```json
{
  "totalQuestions": 10,
  "answeredQuestions": 8,
  "pendingQuestions": 2,
  "failedQuestions": 0,
  "averageScore": 7.8,
  "maxScore": 9.5,
  "minScore": 6.0
}
```

### 6. Exam Statistics
```http
GET /api/statistics/exams/{id}
```
**Response:**
```json
{
  "examId": 1,
  "examName": "Speaking Test Level 1",
  "totalQuestions": 5,
  "totalAttempts": 100,
  "completedAttempts": 95,
  "averageScore": 7.2,
  "maxScore": 9.8,
  "minScore": 4.5,
  "passedCount": 80,
  "passRate": 84.21
}
```

### 7. Recent Test Sessions
```http
GET /api/statistics/test-sessions/recent?limit=10
```
**Response:**
```json
[
  {
    "id": 123,
    "studentName": "Nguyen Van A",
    "examName": "Speaking Test Level 1",
    "status": "COMPLETED",
    "totalScore": 8.5,
    "startedAt": "2026-01-13T10:30:00",
    "completedAt": "2026-01-13T10:45:00"
  }
]
```

### 8. Statistics by Date Range
```http
GET /api/statistics/by-date-range?startDate=2026-01-01T00:00:00&endDate=2026-01-13T23:59:59
```
**Response:**
```json
{
  "questionsCreated": 25,
  "examsCreated": 5,
  "testsTaken": 150,
  "testsCompleted": 140
}
```

---

## 👥 User Management APIs

### 1. Get All Users
```http
GET /api/users?page=0&size=20
```

### 2. Get User by ID
```http
GET /api/users/{id}
```

### 3. Get User by Username
```http
GET /api/users/username/{username}
```

### 4. Create User
```http
POST /api/users
Content-Type: application/json

{
  "username": "teacher1",
  "password": "password123",
  "fullName": "Nguyen Van Teacher",
  "role": "TEACHER",
  "isActive": true
}
```

### 5. Update User
```http
PUT /api/users/{id}
Content-Type: application/json

{
  "username": "teacher1",
  "fullName": "Nguyen Van Teacher Updated",
  "role": "TEACHER",
  "isActive": true
}
```

### 6. Change Password
```http
PUT /api/users/{id}/change-password
Content-Type: application/json

{
  "newPassword": "newpassword123"
}
```

### 7. Toggle User Status
```http
PUT /api/users/{id}/toggle-status
```

### 8. Delete User
```http
DELETE /api/users/{id}
```

### 9. Count Active Users
```http
GET /api/users/count/active
```
**Response:**
```json
{
  "count": 15
}
```

---

## 🔧 Admin APIs

### 1. Bulk Delete Questions
```http
DELETE /api/admin/questions/bulk-delete
Content-Type: application/json

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

### 2. Bulk Update Exam Status
```http
PUT /api/admin/exams/bulk-update-status
Content-Type: application/json

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

### 3. Get All Test Sessions (Admin View)
```http
GET /api/admin/test-sessions?status=COMPLETED&examId=1&page=0&size=20
```

### 4. Cancel Test Session
```http
PUT /api/admin/test-sessions/{id}/cancel
```

### 5. Delete Test Session
```http
DELETE /api/admin/test-sessions/{id}
```

### 6. System Health Check
```http
GET /api/admin/health
```
**Response:**
```json
{
  "database": "OK",
  "questionCount": 150,
  "timestamp": "2026-01-13T14:30:00",
  "status": "RUNNING"
}
```

### 7. Get System Configuration
```http
GET /api/admin/config
```
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

## 📄 Report APIs

### 1. Export Test Session as CSV
```http
GET /api/reports/test-session/{id}/export-csv
```
**Returns:** CSV file download

### 2. Get Detailed Test Session Report
```http
GET /api/reports/test-session/{id}/detailed
```
**Response:**
```json
{
  "sessionId": 123,
  "studentName": "Nguyen Van A",
  "studentOrganization": "ABC Company",
  "examName": "Speaking Test Level 1",
  "totalScore": 7.8,
  "status": "COMPLETED",
  "startedAt": "2026-01-13T10:30:00",
  "completedAt": "2026-01-13T10:45:00",
  "answers": [
    {
      "questionId": 1,
      "questionContent": "Describe your favorite place",
      "questionLevel": "EASY",
      "transcribedText": "My favorite place is...",
      "score": 8.0,
      "feedback": "Good vocabulary and grammar",
      "status": "COMPLETED",
      "answeredAt": "2026-01-13T10:32:00"
    }
  ],
  "completedAnswers": 10,
  "totalQuestions": 10,
  "completionRate": 100.0
}
```

### 3. Export Exam Sessions as CSV
```http
GET /api/reports/exam/{examId}/export-csv
```
**Returns:** CSV file with all test sessions for the exam

---

## 🎯 Use Cases

### Dashboard for Admin
```javascript
// Fetch dashboard data
const dashboard = await fetch('/api/statistics/dashboard');
const questionsByLevel = await fetch('/api/statistics/questions/by-level');
const examsByStatus = await fetch('/api/statistics/exams/by-status');
const recentSessions = await fetch('/api/statistics/test-sessions/recent?limit=5');
```

### User Management
```javascript
// Create new teacher
await fetch('/api/users', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'teacher1',
    password: 'pass123',
    fullName: 'Teacher Name',
    role: 'TEACHER'
  })
});

// Toggle user active status
await fetch('/api/users/1/toggle-status', { method: 'PUT' });
```

### Report Generation
```javascript
// Download test session report
window.location.href = '/api/reports/test-session/123/export-csv';

// Get detailed report
const report = await fetch('/api/reports/test-session/123/detailed');
```

### Bulk Operations
```javascript
// Delete multiple questions
await fetch('/api/admin/questions/bulk-delete', {
  method: 'DELETE',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    questionIds: [1, 2, 3, 4, 5]
  })
});

// Activate multiple exams
await fetch('/api/admin/exams/bulk-update-status', {
  method: 'PUT',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    examIds: [1, 2, 3],
    status: 'ACTIVE'
  })
});
```

---

## 📊 Statistics Calculation Logic

### Average Score
- Calculated from all completed test sessions
- Formula: `sum(scores) / count(completed_sessions)`

### Pass Rate
- Based on exam's passing score threshold
- Formula: `(passed_count / completed_count) * 100`

### Completion Rate
- Percentage of answered questions in a test session
- Formula: `(completed_answers / total_questions) * 100`

---

## 🔐 Authorization (Future Enhancement)

These APIs should be protected with role-based access:

- **Admin Only**: 
  - All `/api/admin/*` endpoints
  - User management CRUD
  - Bulk operations
  
- **Teacher & Admin**:
  - Statistics APIs
  - Report generation
  - View test sessions
  
- **Public** (no auth):
  - Test session creation
  - Submit answers

---

## 📈 Performance Tips

1. **Pagination**: Always use pagination for list endpoints
2. **Filtering**: Use query parameters to reduce data transfer
3. **Caching**: Consider caching dashboard statistics (5-10 minutes)
4. **Async**: Statistics calculation can be async for large datasets
5. **Indexes**: Database indexes are already set up in schema

---

## 🔍 Error Responses

All APIs follow standard error format:

```json
{
  "error": "Error message description",
  "timestamp": "2026-01-13T14:30:00",
  "status": 400
}
```

Common HTTP Status Codes:
- `200 OK` - Success
- `201 Created` - Resource created
- `204 No Content` - Success with no response body
- `400 Bad Request` - Invalid input
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error
