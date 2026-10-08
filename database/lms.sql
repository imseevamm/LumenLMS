-- ============================================================
-- LMS Platform - Complete Database Schema
-- MySQL 8+
-- ============================================================

-- Safe to re-run: this script never drops existing data. To wipe everything and start over,
-- run database/reset_database.sql first.
CREATE DATABASE IF NOT EXISTS lms_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lms_db;

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    user_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name       VARCHAR(120)  NOT NULL,
    email           VARCHAR(150)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255)  NOT NULL,
    role            ENUM('ADMIN','INSTRUCTOR','STUDENT') NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    avatar_path     VARCHAR(255) NULL,
    bio             VARCHAR(500) NULL,
    roll_number     VARCHAR(20) NULL UNIQUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_role (role),
    INDEX idx_users_active (is_active)
) ENGINE=InnoDB;

-- ============================================================
-- COURSES
-- ============================================================
CREATE TABLE IF NOT EXISTS courses (
    course_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    instructor_id   BIGINT NOT NULL,
    title           VARCHAR(180) NOT NULL,
    description     TEXT NULL,
    syllabus        TEXT NULL,
    category        VARCHAR(80)  NOT NULL DEFAULT 'General',
    difficulty      ENUM('BEGINNER','INTERMEDIATE','ADVANCED') NOT NULL DEFAULT 'BEGINNER',
    duration_hours  INT NOT NULL DEFAULT 0,
    thumbnail_color VARCHAR(20) NOT NULL DEFAULT '#6366F1',
    is_published    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_course_instructor FOREIGN KEY (instructor_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_course_category (category),
    INDEX idx_course_instructor (instructor_id)
) ENGINE=InnoDB;

-- ============================================================
-- COURSE MODULES
-- ============================================================
CREATE TABLE IF NOT EXISTS course_modules (
    module_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id       BIGINT NOT NULL,
    title           VARCHAR(150) NOT NULL,
    position        INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_module_course FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
    INDEX idx_module_course (course_id)
) ENGINE=InnoDB;

-- ============================================================
-- LESSONS
-- ============================================================
CREATE TABLE IF NOT EXISTS lessons (
    lesson_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    module_id       BIGINT NOT NULL,
    title           VARCHAR(150) NOT NULL,
    content         MEDIUMTEXT NULL,
    position        INT NOT NULL DEFAULT 0,
    duration_minutes INT NOT NULL DEFAULT 10,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lesson_module FOREIGN KEY (module_id) REFERENCES course_modules(module_id) ON DELETE CASCADE,
    INDEX idx_lesson_module (module_id)
) ENGINE=InnoDB;

-- ============================================================
-- MATERIALS (links / resources attached to a lesson)
-- ============================================================
CREATE TABLE IF NOT EXISTS materials (
    material_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    lesson_id       BIGINT NOT NULL,
    title           VARCHAR(150) NOT NULL,
    material_type   ENUM('LINK','TEXT','FILE') NOT NULL DEFAULT 'LINK',
    url_or_path     VARCHAR(500) NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_material_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(lesson_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- DOWNLOAD HISTORY
-- ============================================================
CREATE TABLE IF NOT EXISTS download_history (
    download_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    title           VARCHAR(200) NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    category        VARCHAR(80) NOT NULL DEFAULT 'File',
    downloaded_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_download_user_time (user_id, downloaded_at),
    CONSTRAINT fk_download_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- ENROLLMENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS enrollments (
    enrollment_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id      BIGINT NOT NULL,
    course_id       BIGINT NOT NULL,
    enrolled_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status          ENUM('ACTIVE','COMPLETED','DROPPED') NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_enroll_student FOREIGN KEY (student_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_enroll_course FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
    CONSTRAINT uq_enrollment UNIQUE (student_id, course_id)
) ENGINE=InnoDB;

-- ============================================================
-- PROGRESS (per-lesson completion tracking)
-- ============================================================
CREATE TABLE IF NOT EXISTS progress (
    progress_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id   BIGINT NOT NULL,
    lesson_id       BIGINT NOT NULL,
    is_completed    BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at    TIMESTAMP NULL,
    CONSTRAINT fk_progress_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id) ON DELETE CASCADE,
    CONSTRAINT fk_progress_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(lesson_id) ON DELETE CASCADE,
    CONSTRAINT uq_progress UNIQUE (enrollment_id, lesson_id)
) ENGINE=InnoDB;

-- ============================================================
-- QUIZZES
-- ============================================================
CREATE TABLE IF NOT EXISTS quizzes (
    quiz_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id       BIGINT NOT NULL,
    title           VARCHAR(150) NOT NULL,
    description     VARCHAR(500) NULL,
    duration_minutes INT NOT NULL DEFAULT 15,
    is_published    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_quiz_course FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS quiz_questions (
    question_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_id         BIGINT NOT NULL,
    question_text   VARCHAR(500) NOT NULL,
    option_a        VARCHAR(255) NOT NULL,
    option_b        VARCHAR(255) NOT NULL,
    option_c        VARCHAR(255) NOT NULL,
    option_d        VARCHAR(255) NOT NULL,
    correct_option  CHAR(1) NOT NULL, -- 'A','B','C','D'
    marks           INT NOT NULL DEFAULT 1,
    position         INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_question_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(quiz_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS quiz_attempts (
    attempt_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_id         BIGINT NOT NULL,
    student_id      BIGINT NOT NULL,
    score           DECIMAL(6,2) NOT NULL DEFAULT 0,
    total_marks     DECIMAL(6,2) NOT NULL DEFAULT 0,
    started_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at    TIMESTAMP NULL,
    CONSTRAINT fk_attempt_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(quiz_id) ON DELETE CASCADE,
    CONSTRAINT fk_attempt_student FOREIGN KEY (student_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT uq_quiz_attempt UNIQUE (quiz_id, student_id)   -- one attempt per student per quiz, enforced by the DB
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS quiz_answers (
    answer_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_id      BIGINT NOT NULL,
    question_id     BIGINT NOT NULL,
    selected_option CHAR(1) NULL,
    is_correct      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_answer_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempts(attempt_id) ON DELETE CASCADE,
    CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES quiz_questions(question_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- ASSIGNMENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS assignments (
    assignment_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id       BIGINT NOT NULL,
    title           VARCHAR(150) NOT NULL,
    description     TEXT NULL,
    instructions    TEXT NULL,
    deadline        DATETIME NOT NULL,
    max_marks       INT NOT NULL DEFAULT 100,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_assignment_course FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS submissions (
    submission_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id   BIGINT NOT NULL,
    student_id      BIGINT NOT NULL,
    submission_text MEDIUMTEXT NULL,
    attachment_path VARCHAR(500) NULL,
    attachment_name VARCHAR(255) NULL,
    attachment_size BIGINT NULL,
    submitted_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    grade           INT NULL,
    feedback        VARCHAR(1000) NULL,
    status          ENUM('SUBMITTED','GRADED','LATE') NOT NULL DEFAULT 'SUBMITTED',
    CONSTRAINT fk_submission_assignment FOREIGN KEY (assignment_id) REFERENCES assignments(assignment_id) ON DELETE CASCADE,
    CONSTRAINT fk_submission_student FOREIGN KEY (student_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT uq_submission UNIQUE (assignment_id, student_id)
) ENGINE=InnoDB;

-- ============================================================
-- NOTIFICATIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    title           VARCHAR(150) NOT NULL,
    message         VARCHAR(500) NOT NULL,
    type            ENUM('INFO','SUCCESS','WARNING','ERROR') NOT NULL DEFAULT 'INFO',
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_notification_user (user_id)
) ENGINE=InnoDB;

-- ============================================================
-- SYSTEM SETTINGS
-- ============================================================
CREATE TABLE IF NOT EXISTS system_settings (
    setting_key     VARCHAR(80) PRIMARY KEY,
    setting_value   VARCHAR(255) NOT NULL
) ENGINE=InnoDB;

-- ============================================================
-- The fresh schema provisions only the administrator account; no demo courses are inserted by default.
-- Create your own users, courses, modules, lessons, quizzes and assignments from the application.

-- System settings
INSERT INTO system_settings (setting_key, setting_value) VALUES
('platform_name', 'LumenLMS'),
('support_email', 'support@lumenlms.com'),
('allow_registration', 'true'),
('default_theme', 'light')
ON DUPLICATE KEY UPDATE setting_key = setting_key;   -- keep existing values on re-run


-- Default administrator account for first-time setup.
-- Demo login: ogadmin@gmail.com / ogadmin980  (change this password after first sign-in: Profile -> Change Password)
-- Create-only: re-running this script never resets an existing administrator's password.
INSERT INTO users (full_name, email, password_hash, role, is_active, bio) VALUES
('System Administrator', 'ogadmin@gmail.com', '$2a$10$oTsCYNp8nVmzhRgSOCmn1uRc3LluwEbPChUBlcADEvv8TSxvd/AKC', 'ADMIN', TRUE, 'Platform administrator')
ON DUPLICATE KEY UPDATE user_id = user_id;
