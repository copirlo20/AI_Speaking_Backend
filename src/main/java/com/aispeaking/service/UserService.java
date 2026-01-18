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
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return UserResponse.from(user);
    }
    
    @Transactional(readOnly = true)
    public User getUserEntityById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return UserResponse.from(user);
    }
    
    @Transactional(readOnly = true)
    public User getUserEntityByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found with username: " + username));
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
        user.setRole(UserRole.TEACHER); // Vai trò mặc định
        user.setIsActive(true);
        User savedUser = userRepository.save(user);
        log.info("Creating new user: {}", user.getUsername());
        return UserResponse.from(savedUser);
    }

    /**
     * Tạo tài khoản người dùng mới với vai trò TEACHER mặc định
     * Phương thức này đảm bảo vai trò luôn là TEACHER và không thể bị ghi đè
     * 
     * param username Tên đăng nhập (phải là duy nhất)
     * param password Mật khẩu (sẽ được mã hóa)
     * param fullName Họ và tên đầy đủ của người dùng
     * return UserResponse DTO
     */
    @Transactional
    public UserResponse createTeacherAccount(String username, String password, String fullName) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists: " + username);
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // Mã hóa mật khẩu
        user.setFullName(fullName);
        user.setRole(UserRole.TEACHER); // Luôn là TEACHER theo mặc định
        user.setIsActive(true);
        User savedUser = userRepository.save(user);
        log.info("Created new TEACHER account: {} (ID: {})", username, savedUser.getId());
        return UserResponse.from(savedUser);
    }

    /**
     * Kiểm tra xem tên đăng nhập đã tồn tại chưa
     * 
     * param username Tên đăng nhập cần kiểm tra
     * return true nếu tên đăng nhập tồn tại, false nếu không
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
        // Mã hóa mật khẩu
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
        userRepository.deleteById(id);
        log.info("Hard deleted user with id: {}", id);
    }
    
    @Transactional(readOnly = true)
    public long countActiveUsers() {
        return userRepository.findAll().stream().filter(u -> u.getIsActive()).count();
    }
}