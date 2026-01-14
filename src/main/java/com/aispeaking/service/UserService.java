package com.aispeaking.service;

import com.aispeaking.dto.*;
import com.aispeaking.entity.User;
import com.aispeaking.entity.enums.UserRole;
import com.aispeaking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return UserResponse.from(user);
    }
    
    @Transactional(readOnly = true)
    public User getUserEntityById(Long id) {
        return userRepository.findById(id)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return UserResponse.from(user);
    }
    
    @Transactional(readOnly = true)
    public User getUserEntityByUsername(String username) {
        return userRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(UserRole.TEACHER); // Default role
        user.setIsActive(true);
        
        User savedUser = userRepository.save(user);
        log.info("Creating new user: {}", user.getUsername());
        return UserResponse.from(savedUser);
    }

    /**
     * Create new user account with default TEACHER role
     * This method ensures role is always TEACHER and cannot be overridden
     * 
     * @param username Username (must be unique)
     * @param password Password (will be encrypted)
     * @param fullName Full name of user
     * @return UserResponse DTO
     */
    @Transactional
    public UserResponse createTeacherAccount(String username, String password, String fullName) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists: " + username);
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // Encrypt password
        user.setFullName(fullName);
        user.setRole(UserRole.TEACHER); // Always TEACHER by default
        user.setIsActive(true);
        
        User savedUser = userRepository.save(user);
        log.info("Created new TEACHER account: {} (ID: {})", username, savedUser.getId());
        return UserResponse.from(savedUser);
    }

    /**
     * Check if username exists
     * 
     * @param username Username to check
     * @return true if username exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = getUserEntityById(id);
        
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        
        User savedUser = userRepository.save(user);
        log.info("Updated user with id: {}", id);
        return UserResponse.from(savedUser);
    }

    @Transactional
    public void changePassword(Long id, String newPassword) {
        User user = getUserEntityById(id);
        // Encrypt password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Changed password for user: {}", user.getUsername());
    }

    @Transactional
    public void toggleUserStatus(Long id) {
        User user = getUserEntityById(id);
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
        log.info("Toggled status for user {}: {}", user.getUsername(), user.getIsActive());
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = getUserEntityById(id);
        user.softDelete();
        userRepository.save(user);
        log.info("Soft deleted user with id: {}", id);
    }

    @Transactional(readOnly = true)
    public long countActiveUsers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getIsActive() && u.getDeletedAt() == null)
                .count();
    }
}
