-- Cơ sở dữ liệu cho ứng dụng AI Speaking
-- Tối ưu hóa với Spring Data JPA và MySQL

-- Quản trị người dùng
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    role ENUM('ADMIN', 'TEACHER') NOT NULL DEFAULT 'TEACHER',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_role (role)
);

-- Ngân hàng câu hỏi
CREATE TABLE questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content TEXT NOT NULL,
    level ENUM('EASY', 'HARD') DEFAULT 'EASY',
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_level (level),
    INDEX idx_created_by (created_by),
    INDEX idx_created_at (created_at)
);

-- Mẫu câu trả lời cho câu hỏi
CREATE TABLE sample_answers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    score DECIMAL(5,2) NOT NULL CHECK (score >= 0 AND score <= 10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    INDEX idx_question_id (question_id)
);

-- Đề thi
CREATE TABLE exams (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    duration_minutes INT,
    total_questions INT NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE', 'DRAFT') DEFAULT 'DRAFT',
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_status (status),
    INDEX idx_created_by (created_by),
    INDEX idx_created_at (created_at)
);

-- Câu hỏi trong đề thi
CREATE TABLE exam_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    question_order INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    UNIQUE KEY unique_exam_question (exam_id, question_id),
    INDEX idx_exam_id (exam_id),
    INDEX idx_question_id (question_id)
);

-- Phiên thi
CREATE TABLE test_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    student_name VARCHAR(100) NOT NULL,
    student_organization VARCHAR(200),
    student_email VARCHAR(100),
    total_score DECIMAL(5,2) DEFAULT 0.00 COMMENT 'Average score of all answers (scale 0-10)',
    status ENUM('IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'IN_PROGRESS',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (exam_id) REFERENCES exams(id),
    INDEX idx_exam_id (exam_id),
    INDEX idx_student_name (student_name),
    INDEX idx_status (status),
    INDEX idx_started_at (started_at)
);

-- Câu trả lời trong phiên thi
CREATE TABLE test_answers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    test_session_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    audio_url VARCHAR(500),
    transcribed_text TEXT,
    score DECIMAL(5,2) DEFAULT 0.00,
    feedback TEXT,
    processing_status ENUM('PENDING', 'TRANSCRIBING', 'SCORING', 'COMPLETED', 'FAILED') DEFAULT 'PENDING',
    answered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (test_session_id) REFERENCES test_sessions(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id),
    INDEX idx_test_session_id (test_session_id),
    INDEX idx_question_id (question_id),
    INDEX idx_processing_status (processing_status)
);

-- Nhật ký xử lý AI
CREATE TABLE ai_processing_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    test_answer_id BIGINT NOT NULL,
    service_type ENUM('WHISPER', 'QWEN') NOT NULL,
    request_data TEXT,
    response_data TEXT,
    processing_time_ms INT,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (test_answer_id) REFERENCES test_answers(id) ON DELETE CASCADE,
    INDEX idx_test_answer_id (test_answer_id),
    INDEX idx_service_type (service_type),
    INDEX idx_created_at (created_at)
);