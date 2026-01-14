# Hướng dẫn Phân quyền - AI Speaking Backend

## Tổng quan

Hệ thống sử dụng Spring Security với JWT (JSON Web Token) để xác thực và phân quyền. Có 2 vai trò chính:

### 1. ADMIN - Toàn quyền
Quản trị viên có quyền truy cập toàn bộ hệ thống, bao gồm:
- ✅ Quản lý người dùng (CRUD users)
- ✅ Quản lý câu hỏi (CRUD questions)
- ✅ Quản lý kì thi (CRUD exams)
- ✅ Xem và quản lý bài thi (test sessions)
- ✅ Chấm lại điểm bài thi
- ✅ Xem thống kê toàn hệ thống
- ✅ Truy cập các endpoint admin đặc biệt (/admin/*)

### 2. TEACHER - Giáo viên
Giáo viên có quyền hạn chế:
- ✅ CRUD câu hỏi (questions)
- ✅ CRUD kì thi (exams)
- ✅ Xem danh sách bài thi
- ✅ Chấm lại điểm bài thi (PUT /test-sessions/*)
- ✅ Xem thống kê về bài thi và kì thi
- ❌ KHÔNG thể quản lý người dùng
- ❌ KHÔNG thể xóa bài thi
- ❌ KHÔNG thể truy cập /admin/* endpoints

## Chi tiết phân quyền theo endpoint

### Endpoints công khai (Public)
```
POST /api/auth/login            - Đăng nhập
POST /api/auth/register         - Đăng ký tài khoản TEACHER
GET  /api/auth/check-username/* - Kiểm tra username tồn tại
POST /api/test-sessions         - Học sinh tạo bài thi
POST /api/test-sessions/*/submit-answer - Học sinh nộp bài
POST /api/test-sessions/*/complete      - Học sinh hoàn thành bài thi
```

### Questions (Câu hỏi) - TEACHER & ADMIN
```
GET    /api/questions/**        - Xem câu hỏi
POST   /api/questions           - Tạo câu hỏi mới
PUT    /api/questions/*         - Cập nhật câu hỏi
DELETE /api/questions/*         - Xóa câu hỏi
```

### Exams (Kì thi) - TEACHER & ADMIN
```
GET    /api/exams/**            - Xem kì thi
POST   /api/exams               - Tạo kì thi mới
PUT    /api/exams/*             - Cập nhật kì thi
DELETE /api/exams/*             - Xóa kì thi
```

### Test Sessions (Bài thi) - TEACHER & ADMIN (Xem/Chấm), ADMIN (Xóa)
```
GET    /api/test-sessions/**    - Xem bài thi (TEACHER & ADMIN)
PUT    /api/test-sessions/**    - Chấm lại điểm (TEACHER & ADMIN)
DELETE /api/test-sessions/*     - Xóa bài thi (chỉ ADMIN)
```

### Statistics (Thống kê) - TEACHER & ADMIN
```
GET /api/statistics/**           - Xem tất cả thống kê
```

### Reports (Báo cáo) - TEACHER & ADMIN
```
GET /api/reports/**              - Xem báo cáo
```

### Admin - CHỈ ADMIN
```
GET    /api/admin/**             - Tất cả admin endpoints
POST   /api/admin/**
PUT    /api/admin/**
DELETE /api/admin/**
```

### Users (Quản lý người dùng) - CHỈ ADMIN
```
GET    /api/users/**             - Xem danh sách người dùng
POST   /api/users                - Tạo người dùng (có thể chọn role)
PUT    /api/users/**             - Cập nhật người dùng
DELETE /api/users/*              - Xóa người dùng
```

## Cách sử dụng JWT Token

### 1. Đăng nhập
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "id": 1,
  "username": "admin",
  "fullName": "Admin User",
  "role": "ADMIN",
  "isActive": true,
  "message": "Login successful"
}
```

### 2. Sử dụng token trong các request tiếp theo
Thêm header `Authorization` với giá trị `Bearer {token}`:

```http
GET /api/questions
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## Tạo tài khoản

### Đăng ký tài khoản TEACHER (Public)
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "teacher01",
  "password": "password123",
  "fullName": "Nguyễn Văn A"
}
```

### Tạo tài khoản với role tùy chỉnh (chỉ ADMIN)
```http
POST /api/users
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "username": "admin02",
  "password": "admin123",
  "fullName": "Admin User 2",
  "role": "ADMIN",
  "isActive": true
}
```

## Cấu hình JWT

File `application.properties`:
```properties
jwt.secret=YourSuperSecretKeyForJWTTokenGenerationChangeThisInProduction
jwt.expiration=86400000  # 24 giờ (milliseconds)
```

## Kiến trúc bảo mật

### Các thành phần chính:

1. **JwtTokenProvider** - Tạo và validate JWT tokens
2. **JwtAuthenticationFilter** - Filter kiểm tra token trong mỗi request
3. **CustomUserDetailsService** - Load thông tin user từ database
4. **UserPrincipal** - User details implementation cho Spring Security
5. **SecurityConfig** - Cấu hình phân quyền và security filter chain

### Luồng xác thực:

1. User đăng nhập → AuthController nhận request
2. AuthenticationManager xác thực username/password
3. JwtTokenProvider tạo JWT token
4. Client lưu token và gửi trong header Authorization
5. JwtAuthenticationFilter kiểm tra và validate token
6. CustomUserDetailsService load user từ DB
7. Spring Security kiểm tra role và quyền truy cập endpoint

## Lưu ý bảo mật

- ✅ Passwords được mã hóa bằng BCrypt
- ✅ JWT token hết hạn sau 24 giờ
- ✅ Stateless authentication (không lưu session)
- ✅ CORS được cấu hình cho frontend origins
- ✅ CSRF protection được tắt (do dùng JWT)
- ⚠️ Nên thay đổi jwt.secret trong production
- ⚠️ Nên cấu hình HTTPS trong production

## Testing

### Test với Postman:
1. Import file `postman_collection_admin.json`
2. Login để lấy token
3. Set token vào biến Environment
4. Test các endpoints với các role khác nhau

### Test với curl:
```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Sử dụng token
curl -X GET http://localhost:8080/api/questions \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

## Troubleshooting

### "403 Forbidden" error
- Kiểm tra token có hợp lệ không
- Kiểm tra role của user có đủ quyền không
- Kiểm tra token có hết hạn không

### "401 Unauthorized" error
- Token không hợp lệ hoặc thiếu
- Token đã hết hạn
- Sai format header Authorization

### Token không hoạt động
- Kiểm tra jwt.secret trong application.properties
- Kiểm tra JwtAuthenticationFilter có được add vào SecurityFilterChain không
- Xem log để debug
