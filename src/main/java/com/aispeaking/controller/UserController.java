package com.aispeaking.controller;

import com.aispeaking.dto.CreateUserRequest;
import com.aispeaking.entity.User;
import com.aispeaking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed.origins}")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<Page<User>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    /**
     * Create new user account with TEACHER role (default)
     * POST /users/register
     * 
     * Request body:
     * {
     *   "username": "teacher01",
     *   "password": "password123",
     *   "fullName": "John Doe"
     * }
     * 
     * @param request User registration details
     * @return Created user with TEACHER role
     */
    @PostMapping("/register")
    public ResponseEntity<User> registerTeacher(@RequestBody CreateUserRequest request) {
        User user = userService.createTeacherAccount(
            request.getUsername(),
            request.getPassword(),
            request.getFullName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Create user (admin only - can specify role)
     * POST /users
     */
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.ok(userService.createUser(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody User user) {
        return ResponseEntity.ok(userService.updateUser(id, user));
    }

    @PutMapping("/{id}/change-password")
    public ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String newPassword = request.get("newPassword");
        userService.changePassword(id, newPassword);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<Void> toggleUserStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count/active")
    public ResponseEntity<Map<String, Long>> countActiveUsers() {
        long count = userService.countActiveUsers();
        return ResponseEntity.ok(Map.of("count", count));
    }
}
