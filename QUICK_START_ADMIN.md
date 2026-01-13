# 🚀 Quick Start Guide - Admin Features

## Prerequisites
✅ Backend is running on `http://localhost:8080`  
✅ MySQL database is set up  
✅ You have curl or Postman installed  

---

## 🎯 5-Minute Quick Test

### Step 1: Check System Health
```bash
curl http://localhost:8080/api/admin/health
```

Expected Response:
```json
{
  "database": "OK",
  "questionCount": 0,
  "timestamp": "2026-01-13T14:30:00",
  "status": "RUNNING"
}
```

---

### Step 2: Create Your First Admin User
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123",
    "fullName": "System Administrator",
    "role": "ADMIN",
    "isActive": true
  }'
```

---

### Step 3: Create a Teacher User
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "teacher1",
    "password": "teacher123",
    "fullName": "English Teacher",
    "role": "TEACHER",
    "isActive": true
  }'
```

---

### Step 4: View Dashboard Statistics
```bash
curl http://localhost:8080/api/statistics/dashboard
```

Expected Response:
```json
{
  "totalQuestions": 0,
  "totalExams": 0,
  "activeExams": 0,
  "totalTestSessions": 0,
  "completedSessions": 0,
  "activeUsers": 2,
  "averageScore": 0.0
}
```

---

### Step 5: Create Sample Questions
```bash
# Question 1 - Easy
curl -X POST http://localhost:8080/api/questions \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Introduce yourself",
    "level": "EASY",
    "category": "Introduction"
  }'

# Question 2 - Hard
curl -X POST http://localhost:8080/api/questions \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Describe a complex problem you solved at work",
    "level": "HARD",
    "category": "Work Experience"
  }'
```

---

### Step 6: View Questions by Level
```bash
curl http://localhost:8080/api/statistics/questions/by-level
```

Expected Response:
```json
{
  "EASY": 1,
  "HARD": 1
}
```

---

### Step 7: Create an Exam
```bash
curl -X POST http://localhost:8080/api/exams \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Speaking Test - Level 1",
    "description": "Basic speaking assessment",
    "totalQuestions": 2,
    "durationMinutes": 10,
    "status": "ACTIVE",
    "passingScore": 5.0
  }'
```

---

### Step 8: View All Users
```bash
curl http://localhost:8080/api/users?page=0&size=10
```

---

### Step 9: Get Recent Activity
```bash
curl http://localhost:8080/api/statistics/test-sessions/recent?limit=5
```

---

### Step 10: Export a Report (after having test sessions)
```bash
# First, you need to have at least one test session
# Then export it:
curl -O http://localhost:8080/api/reports/test-session/1/export-csv
```

---

## 📊 Dashboard Data Example

After adding some questions and exams, your dashboard will look like:

```json
{
  "totalQuestions": 50,
  "totalExams": 5,
  "activeExams": 3,
  "totalTestSessions": 120,
  "completedSessions": 100,
  "activeUsers": 10,
  "averageScore": 7.2
}
```

---

## 🎨 Frontend Integration Examples

### React Dashboard Component
```jsx
import React, { useEffect, useState } from 'react';

function Dashboard() {
  const [stats, setStats] = useState(null);

  useEffect(() => {
    fetch('http://localhost:8080/api/statistics/dashboard')
      .then(res => res.json())
      .then(data => setStats(data));
  }, []);

  if (!stats) return <div>Loading...</div>;

  return (
    <div className="dashboard">
      <h1>Dashboard</h1>
      <div className="stats-grid">
        <StatCard title="Total Questions" value={stats.totalQuestions} />
        <StatCard title="Active Exams" value={stats.activeExams} />
        <StatCard title="Completed Sessions" value={stats.completedSessions} />
        <StatCard title="Average Score" value={stats.averageScore} />
      </div>
    </div>
  );
}

function StatCard({ title, value }) {
  return (
    <div className="stat-card">
      <h3>{title}</h3>
      <p className="value">{value}</p>
    </div>
  );
}
```

---

### Vue.js User Management
```vue
<template>
  <div class="user-management">
    <h2>User Management</h2>
    
    <button @click="showCreateForm = true">Create New User</button>
    
    <table>
      <thead>
        <tr>
          <th>Username</th>
          <th>Full Name</th>
          <th>Role</th>
          <th>Status</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="user in users" :key="user.id">
          <td>{{ user.username }}</td>
          <td>{{ user.fullName }}</td>
          <td>{{ user.role }}</td>
          <td>
            <span :class="user.isActive ? 'active' : 'inactive'">
              {{ user.isActive ? 'Active' : 'Inactive' }}
            </span>
          </td>
          <td>
            <button @click="toggleStatus(user.id)">Toggle Status</button>
            <button @click="editUser(user)">Edit</button>
            <button @click="deleteUser(user.id)">Delete</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
export default {
  data() {
    return {
      users: [],
      showCreateForm: false
    };
  },
  mounted() {
    this.loadUsers();
  },
  methods: {
    async loadUsers() {
      const response = await fetch('http://localhost:8080/api/users?page=0&size=20');
      const data = await response.json();
      this.users = data.content;
    },
    async toggleStatus(userId) {
      await fetch(`http://localhost:8080/api/users/${userId}/toggle-status`, {
        method: 'PUT'
      });
      this.loadUsers();
    },
    async deleteUser(userId) {
      if (confirm('Are you sure?')) {
        await fetch(`http://localhost:8080/api/users/${userId}`, {
          method: 'DELETE'
        });
        this.loadUsers();
      }
    }
  }
};
</script>
```

---

### Angular Statistics Service
```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

interface DashboardStats {
  totalQuestions: number;
  totalExams: number;
  activeExams: number;
  totalTestSessions: number;
  completedSessions: number;
  activeUsers: number;
  averageScore: number;
}

@Injectable({
  providedIn: 'root'
})
export class StatisticsService {
  private apiUrl = 'http://localhost:8080/api/statistics';

  constructor(private http: HttpClient) {}

  getDashboard(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.apiUrl}/dashboard`);
  }

  getQuestionsByLevel(): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(`${this.apiUrl}/questions/by-level`);
  }

  getExamsByStatus(): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(`${this.apiUrl}/exams/by-status`);
  }

  getExamStats(examId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/exams/${examId}`);
  }

  exportTestSessionCsv(sessionId: number): void {
    window.location.href = `http://localhost:8080/api/reports/test-session/${sessionId}/export-csv`;
  }
}
```

---

## 🔧 Common Operations

### Bulk Delete Old Questions
```bash
curl -X DELETE http://localhost:8080/api/admin/questions/bulk-delete \
  -H "Content-Type: application/json" \
  -d '{
    "questionIds": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
  }'
```

### Activate Multiple Exams
```bash
curl -X PUT http://localhost:8080/api/admin/exams/bulk-update-status \
  -H "Content-Type: application/json" \
  -d '{
    "examIds": [1, 2, 3],
    "status": "ACTIVE"
  }'
```

### Change User Password
```bash
curl -X PUT http://localhost:8080/api/users/1/change-password \
  -H "Content-Type: application/json" \
  -d '{
    "newPassword": "newSecurePassword123"
  }'
```

### Get Detailed Exam Statistics
```bash
curl http://localhost:8080/api/statistics/exams/1
```

Response:
```json
{
  "examId": 1,
  "examName": "Speaking Test - Level 1",
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

---

## 📱 Mobile App Integration

### Flutter Example
```dart
import 'package:http/http.dart' as http;
import 'dart:convert';

class ApiService {
  final String baseUrl = 'http://localhost:8080/api';

  Future<Map<String, dynamic>> getDashboardStats() async {
    final response = await http.get(Uri.parse('$baseUrl/statistics/dashboard'));
    
    if (response.statusCode == 200) {
      return json.decode(response.body);
    } else {
      throw Exception('Failed to load dashboard stats');
    }
  }

  Future<void> createUser(String username, String password, String fullName, String role) async {
    final response = await http.post(
      Uri.parse('$baseUrl/users'),
      headers: {'Content-Type': 'application/json'},
      body: json.encode({
        'username': username,
        'password': password,
        'fullName': fullName,
        'role': role,
        'isActive': true,
      }),
    );

    if (response.statusCode != 200) {
      throw Exception('Failed to create user');
    }
  }
}
```

---

## 🐛 Troubleshooting

### Issue: "Connection refused"
**Solution**: Make sure backend is running
```bash
cd backend
mvn spring-boot:run
```

### Issue: "404 Not Found"
**Solution**: Check the base URL includes `/api`
```
✅ http://localhost:8080/api/statistics/dashboard
❌ http://localhost:8080/statistics/dashboard
```

### Issue: "Empty statistics"
**Solution**: Add some test data first
```bash
# Run the test script
./test-admin-apis.bat  # Windows
./test-admin-apis.sh   # Linux/Mac
```

### Issue: "JSON parse error"
**Solution**: Make sure Content-Type header is set
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \  ← Important!
  -d '{"username":"test"}'
```

---

## 📚 Next Steps

1. ✅ Test all endpoints with Postman
2. ✅ Build a frontend dashboard
3. ✅ Add authentication/authorization
4. ✅ Deploy to production
5. ✅ Set up monitoring and logging

---

## 💡 Tips

- Use Postman for API testing (import `postman_collection_admin.json`)
- Check [API_DOCUMENTATION_ADMIN.md](API_DOCUMENTATION_ADMIN.md) for full details
- Use pagination for large datasets (`?page=0&size=20`)
- Cache statistics data on frontend (refresh every 5 minutes)
- Monitor `/api/admin/health` endpoint regularly

---

**Ready to build something amazing! 🚀**
