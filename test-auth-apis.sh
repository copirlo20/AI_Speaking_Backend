#!/bin/bash
# Test Authentication APIs

BASE_URL="http://localhost:8080/api"

echo "=========================================="
echo "Testing Authentication APIs"
echo "=========================================="
echo ""

# Test 1: Register new account
echo "1. Register new TEACHER account"
curl -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "teacher_test",
    "password": "password123",
    "fullName": "Test Teacher"
  }' | jq '.'
echo -e "\n"

# Test 2: Check username exists
echo "2. Check if username exists"
curl -X GET "$BASE_URL/auth/check-username/teacher_test" | jq '.'
echo -e "\n"

# Test 3: Login with correct credentials
echo "3. Login with correct credentials"
curl -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "teacher_test",
    "password": "password123"
  }' | jq '.'
echo -e "\n"

# Test 4: Login with wrong password
echo "4. Login with wrong password (should fail)"
curl -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "teacher_test",
    "password": "wrongpassword"
  }' | jq '.'
echo -e "\n"

# Test 5: Login with non-existent user
echo "5. Login with non-existent user (should fail)"
curl -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "notexist",
    "password": "password123"
  }' | jq '.'
echo -e "\n"

# Test 6: Login with admin (if exists)
echo "6. Login with admin account"
curl -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }' | jq '.'
echo -e "\n"

echo "=========================================="
echo "Test completed!"
echo "=========================================="
