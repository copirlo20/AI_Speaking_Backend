package com.aispeaking.controller;

import com.aispeaking.dto.*;
import com.aispeaking.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed.origins}")
public class UserController {

    private final UserService userService;

    /**
     * Get all users with pagination
     * GET /users?page=0&size=10&sort=id,desc
     * 
     * Response JSON (Page):
     * {
     *   "content": [
     *     {
     *       "id": 1,
     *       "username": "admin",
     *       "fullName": "Administrator",
     *       "role": "ADMIN",
     *       "isActive": true,
     *       "createdAt": "2026-01-15T10:00:00"
     *     }
     *   ],
     *   "totalElements": 50,
     *   "totalPages": 5,
     *   "size": 10,
     *   "number": 0
     * }
     */
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    /**
     * Get user by ID
     * GET /users/{id}
     * 
     * Response JSON:
     * {
     *   "id": 1,
     *   "username": "admin",
     *   "fullName": "Administrator",
     *   "role": "ADMIN",
     *   "isActive": true,
     *   "createdAt": "2026-01-15T10:00:00",
     *   "updatedAt": "2026-01-15T10:00:00"
     * }
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * Get user by username
     * GET /users/username/{username}
     * 
     * Response JSON: Same as getUserById
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    /**
     * Create user (admin only - can specify role)
     * POST /users
     * 
     * Request JSON:
     * {
     *   "username": "newuser",
     *   "password": "password123",
     *   "fullName": "New User Name",
     *   "role": "TEACHER"  // ADMIN, TEACHER, STUDENT
     * }
     * 
     * Response JSON: Same as getUserById
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    /**
     * Update user
     * PUT /users/{id}
     * 
     * Request JSON:
     * {
     *   "fullName": "Updated Full Name",
     *   "role": "TEACHER",
     *   "isActive": true
     * }
     * 
     * Response JSON: Same as getUserById
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    /**
     * Change user password
     * PUT /users/{id}/change-password
     * 
     * Request JSON:
     * {
     *   "newPassword": "newPassword123"
     * }
     * 
     * Response: 200 OK (empty body)
     */
    @PutMapping("/{id}/change-password")
    public ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(id, request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    /**
     * Toggle user active status
     * PUT /users/{id}/toggle-status
     * 
     * Response: 200 OK (empty body)
     */
    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<Void> toggleUserStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Delete user
     * DELETE /users/{id}
     * 
     * Response: 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Count active users
     * GET /users/count/active
     * 
     * Response JSON:
     * {
     *   "count": 42
     * }
     */
    @GetMapping("/count/active")
    public ResponseEntity<Map<String, Long>> countActiveUsers() {
        long count = userService.countActiveUsers();
        return ResponseEntity.ok(Map.of("count", count));
    }
}
