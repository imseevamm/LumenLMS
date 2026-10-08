-- Run once on an EXISTING LumenLMS database.
-- Enforces "one attempt per student per quiz" in the database itself (the app already checks this).
-- If it fails with a duplicate-key error, the table already contains two attempts for the same
-- quiz/student pair: delete the extra row(s) from quiz_attempts and run this again.
USE lms_db;

SET @idx_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quiz_attempts' AND INDEX_NAME = 'uq_quiz_attempt'
);
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE quiz_attempts ADD CONSTRAINT uq_quiz_attempt UNIQUE (quiz_id, student_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
