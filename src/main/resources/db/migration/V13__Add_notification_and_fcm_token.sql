-- Notification 테이블 생성
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    body VARCHAR(500) NOT NULL,
    type VARCHAR(50) NOT NULL,
    reference_id BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    is_sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6),
    updated_at DATETIME(6),

    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_notification_user_read (user_id, is_read),
    INDEX idx_notification_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- FCM Token 테이블 생성
CREATE TABLE IF NOT EXISTS fcm_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL,
    device_id VARCHAR(255),
    device_type VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6),
    updated_at DATETIME(6),

    CONSTRAINT fk_fcm_token_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    CONSTRAINT uk_fcm_token UNIQUE (token),
    INDEX idx_fcm_token_user (user_id),
    INDEX idx_fcm_token_token (token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
