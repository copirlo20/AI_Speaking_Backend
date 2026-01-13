#!/bin/bash

# Test Admin & Statistics APIs
BASE_URL="http://localhost:8080/api"

echo "=========================================="
echo "Testing Admin & Statistics APIs"
echo "=========================================="
echo ""

echo "1. Testing Dashboard Statistics..."
curl -s -X GET "$BASE_URL/statistics/dashboard" | json_pp
echo ""

echo "2. Testing Questions by Level..."
curl -s -X GET "$BASE_URL/statistics/questions/by-level" | json_pp
echo ""

echo "3. Testing Exams by Status..."
curl -s -X GET "$BASE_URL/statistics/exams/by-status" | json_pp
echo ""

echo "4. Testing Recent Test Sessions..."
curl -s -X GET "$BASE_URL/statistics/test-sessions/recent?limit=5" | json_pp
echo ""

echo "5. Creating a Test User..."
curl -s -X POST "$BASE_URL/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "teacher_test",
    "password": "test123",
    "fullName": "Test Teacher",
    "role": "TEACHER",
    "isActive": true
  }' | json_pp
echo ""

echo "6. Getting All Users..."
curl -s -X GET "$BASE_URL/users?page=0&size=10" | json_pp
echo ""

echo "7. Testing System Health..."
curl -s -X GET "$BASE_URL/admin/health" | json_pp
echo ""

echo "8. Testing System Config..."
curl -s -X GET "$BASE_URL/admin/config" | json_pp
echo ""

echo "=========================================="
echo "All tests completed!"
echo "=========================================="
