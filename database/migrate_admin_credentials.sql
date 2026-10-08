-- Run once on an existing LumenLMS database to migrate the administrator login.
-- New administrator login: ogadmin@gmail.com / ogadmin980
-- Password is stored as a BCrypt hash, never as plaintext in the database.

USE lms_db;

UPDATE users
SET email = 'ogadmin@gmail.com',
    password_hash = '$2a$10$oTsCYNp8nVmzhRgSOCmn1uRc3LluwEbPChUBlcADEvv8TSxvd/AKC',
    role = 'ADMIN',
    is_active = TRUE
WHERE email = 'admin@lms.com' AND role = 'ADMIN';

INSERT INTO users (full_name, email, password_hash, role, is_active, bio)
VALUES (
    'System Administrator',
    'ogadmin@gmail.com',
    '$2a$10$oTsCYNp8nVmzhRgSOCmn1uRc3LluwEbPChUBlcADEvv8TSxvd/AKC',
    'ADMIN',
    TRUE,
    'Platform administrator'
)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    role = 'ADMIN',
    is_active = TRUE;
