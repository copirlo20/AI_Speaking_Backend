#!/bin/bash
# Test script for user registration
# Role is automatically set to TEACHER (cannot be changed by client)

BASE_URL="http://localhost:8080"

echo "=========================================="
echo "Testing User Registration (TEACHER role)"
echo "=========================================="
echo ""

# Test 1: Register new TEACHER account
echo "1. Register new TEACHER account"
curl -X POST "$BASE_URL/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "teacher01",
    "password": "password123",
    "fullName": "Nguyen Van A"
  }' | jq '.'
echo -e "\n"

# Test 2: Register another TEACHER account
echo "2. Register another TEACHER account"
curl -X POST "$BASE_URL/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "teacher02",
    "password": "password456",
    "fullName": "Tran Thi B"
  }' | jq '.'
echo -e "\n"

# Test 3: Try to register with duplicate username (should fail)
echo "3. Try duplicate username (should fail)"
curl -X POST "$BASE_URL/users/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "teacher01",
    "password": "newpassword",
    "fullName": "Another Person"
  }' | jq '.'
echo -e "\n"

# Test 4: Get all users to verify TEACHER role
echo "4. Get all users (verify TEACHER role)"
curl -X GET "$BASE_URL/users" | jq '.'
echo -e "\n"

# Test 5: Get user by username
echo "5. Get user by username (teacher01)"
curl -X GET "$BASE_URL/users/username/teacher01" | jq '.'
echo -e "\n"

echo "=========================================="
echo "Note: All registered users have TEACHER role by default"
echo "To create ADMIN users, use POST /users endpoint (admin only)"
echo "=========================================="
