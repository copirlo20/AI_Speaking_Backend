package com.aispeaking.service;

import com.aispeaking.entity.User;
import com.aispeaking.entity.enums.UserRole;
import com.aispeaking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    // Note: You'll need to add PasswordEncoder bean in SecurityConfig
    // For now, using plain text (should be encrypted in production)

    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }

    @Transactional
    public User createUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists: " + user.getUsername());
        }
        
        // In production, encrypt password
        // user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        log.info("Creating new user: {}", user.getUsername());
        return userRepository.save(user);
    }

    /**
     * Create new user account with default TEACHER role
     * This method ensures role is always TEACHER and cannot be overridden
     * 
     * @param username Username (must be unique)
     * @param password Password (should be encrypted in production)
     * @param fullName Full name of user
     * @return Created user entity
     */
    @Transactional
    public User createTeacherAccount(String username, String password, String fullName) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists: " + username);
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(password); // TODO: Encrypt with BCrypt in production
        user.setFullName(fullName);
        user.setRole(UserRole.TEACHER); // Always TEACHER by default
        user.setIsActive(true);
        
        User savedUser = userRepository.save(user);
        log.info("Created new TEACHER account: {} (ID: {})", username, savedUser.getId());
        return savedUser;
    }

    /**
     * Login user with username and password
     * 
     * @param username Username
     * @param password Plain text password
     * @return User entity if login successful
     * @throws RuntimeException if credentials are invalid or account is inactive
     */
    @Transactional(readOnly = true)
    public User login(String username, String password) {
        User user = userRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));
        
        // TODO: Use passwordEncoder.matches() in production
        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Invalid username or password");
        }
        
        if (!user.getIsActive()) {
            throw new RuntimeException("Account is inactive");
        }
        
        log.info("User {} logged in successfully", username);
        return user;
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
    public User updateUser(Long id, User userDetails) {
        User user = getUserById(id);
        
        if (!user.getUsername().equals(userDetails.getUsername()) 
                && userRepository.existsByUsername(userDetails.getUsername())) {
            throw new RuntimeException("Username already exists: " + userDetails.getUsername());
        }
        
        user.setFullName(userDetails.getFullName());
        user.setUsername(userDetails.getUsername());
        user.setRole(userDetails.getRole());
        user.setIsActive(userDetails.getIsActive());
        
        log.info("Updated user with id: {}", id);
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long id, String newPassword) {
        User user = getUserById(id);
        // In production, encrypt password
        // user.setPassword(passwordEncoder.encode(newPassword));
        user.setPassword(newPassword);
        userRepository.save(user);
        log.info("Changed password for user: {}", user.getUsername());
    }

    @Transactional
    public void toggleUserStatus(Long id) {
        User user = getUserById(id);
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
        log.info("Toggled status for user {}: {}", user.getUsername(), user.getIsActive());
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
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

    @Transactional(readOnly = true)
    public long countUsersByRole(UserRole role) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == role && u.getDeletedAt() == null)
                .count();
    }
}
