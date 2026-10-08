-- LumenLMS: download history
-- Safe to run multiple times.
CREATE TABLE IF NOT EXISTS download_history (
    download_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    category VARCHAR(80) NOT NULL DEFAULT 'File',
    downloaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_download_user_time (user_id, downloaded_at),
    CONSTRAINT fk_download_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;
