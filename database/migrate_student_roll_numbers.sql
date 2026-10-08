-- Safe, repeatable migration for student roll numbers.
-- Run this against the existing lms_db database.
USE lms_db;

-- Add the column only when it does not already exist.
SET @column_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'roll_number'
);
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE users ADD COLUMN roll_number VARCHAR(20) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Assign deterministic unique roll numbers to every existing student.
UPDATE users
SET roll_number = CONCAT('ST25', LPAD(user_id, 6, '0'))
WHERE role = 'STUDENT' AND (roll_number IS NULL OR roll_number = '');

-- Add the unique constraint only if it is not already present.
SET @index_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND INDEX_NAME = 'uq_users_roll_number'
);
SET @sql = IF(@index_exists = 0,
    'ALTER TABLE users ADD UNIQUE KEY uq_users_roll_number (roll_number)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
