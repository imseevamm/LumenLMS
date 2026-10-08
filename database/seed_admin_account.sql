-- Run once on an existing LumenLMS database if the admin demo account is missing.
-- Create-only: an existing ogadmin@gmail.com account is left untouched.
-- Demo login: ogadmin@gmail.com / ogadmin980
-- Password is stored as a BCrypt hash; no plaintext password is stored in the database.

USE lms_db;

INSERT INTO users (full_name, email, password_hash, role, is_active, bio)
VALUES (
    'System Administrator',
    'ogadmin@gmail.com',
    '$2a$10$oTsCYNp8nVmzhRgSOCmn1uRc3LluwEbPChUBlcADEvv8TSxvd/AKC',
    'ADMIN',
    TRUE,
    'Platform administrator'
)
ON DUPLICATE KEY UPDATE user_id = user_id;
