-- Run once on an existing LMS database.
-- Student assignment submissions may optionally include one PDF attachment.
ALTER TABLE submissions
    ADD COLUMN attachment_path VARCHAR(500) NULL AFTER submission_text,
    ADD COLUMN attachment_name VARCHAR(255) NULL AFTER attachment_path,
    ADD COLUMN attachment_size BIGINT NULL AFTER attachment_name;
