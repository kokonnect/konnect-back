-- ================================================
-- Konnect Database Schema Migration
-- Version: 1.0 - Initial Schema
-- ================================================

-- User Domain Tables
-- ================================================

-- User table
CREATE TABLE user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    social_id VARCHAR(255) NOT NULL,
    provider VARCHAR(50),
    nickname VARCHAR(100),
    registered_at DATETIME(6),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY UK_user_social_id (social_id),
    INDEX idx_user_provider (provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Child table
CREATE TABLE child (
    child_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(100),
    school VARCHAR(200),
    grade VARCHAR(50),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (child_id),
    CONSTRAINT FK_child_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_child_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- AI Domain Tables
-- ================================================

-- Message Template table
CREATE TABLE message_template (
    message_template_id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255),
    scenario_category VARCHAR(50),
    korean_content TEXT,
    content TEXT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (message_template_id),
    INDEX idx_message_template_category (scenario_category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User Generated Message table
CREATE TABLE user_generated_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    input_prompt VARCHAR(500),
    generated_korean TEXT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT FK_user_generated_message_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_user_generated_message_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User Template Scrap table
CREATE TABLE user_template_scrap (
    user_template_scrap_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    message_template_id BIGINT NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (user_template_scrap_id),
    CONSTRAINT FK_user_template_scrap_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    CONSTRAINT FK_user_template_scrap_template FOREIGN KEY (message_template_id) REFERENCES message_template(message_template_id) ON DELETE CASCADE,
    UNIQUE KEY UK_user_template_scrap (user_id, message_template_id),
    INDEX idx_user_template_scrap_user_id (user_id),
    INDEX idx_user_template_scrap_template_id (message_template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Document Domain Tables
-- ================================================

-- Document table
CREATE TABLE document (
    document_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    description TEXT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (document_id),
    CONSTRAINT FK_document_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_document_user_id (user_id),
    INDEX idx_document_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Document File table
CREATE TABLE document_file (
    document_file_id BIGINT NOT NULL AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    file_name VARCHAR(255),
    file_type VARCHAR(50),
    file_url TEXT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (document_file_id),
    CONSTRAINT FK_document_file_document FOREIGN KEY (document_id) REFERENCES document(document_id) ON DELETE CASCADE,
    INDEX idx_document_file_document_id (document_id),
    INDEX idx_document_file_type (file_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Document Translation table
CREATE TABLE document_translation (
    translation_id BIGINT NOT NULL AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    translated_language VARCHAR(10) DEFAULT 'en',
    translated_text TEXT,
    summary TEXT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (translation_id),
    CONSTRAINT FK_document_translation_document FOREIGN KEY (document_id) REFERENCES document(document_id) ON DELETE CASCADE,
    INDEX idx_document_translation_document_id (document_id),
    INDEX idx_document_translation_language (translated_language)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Schedule Domain Tables
-- ================================================

-- Schedule table
CREATE TABLE schedule (
    schedule_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    child_id BIGINT,
    title VARCHAR(255),
    memo TEXT,
    start_date DATETIME(6),
    end_date DATETIME(6),
    is_all_day BOOLEAN,
    created_from_notice BOOLEAN,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (schedule_id),
    CONSTRAINT FK_schedule_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    CONSTRAINT FK_schedule_child FOREIGN KEY (child_id) REFERENCES child(child_id) ON DELETE SET NULL,
    INDEX idx_schedule_user_id (user_id),
    INDEX idx_schedule_child_id (child_id),
    INDEX idx_schedule_start_date (start_date),
    INDEX idx_schedule_end_date (end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Schedule Alarm table
CREATE TABLE schedule_alarm (
    id BIGINT NOT NULL AUTO_INCREMENT,
    schedule_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    alarm_time_type VARCHAR(50),
    custom_minutes_before DATETIME(6),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT FK_schedule_alarm_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id) ON DELETE CASCADE,
    CONSTRAINT FK_schedule_alarm_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_schedule_alarm_schedule_id (schedule_id),
    INDEX idx_schedule_alarm_user_id (user_id),
    INDEX idx_schedule_alarm_time_type (alarm_time_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Schedule Repeat table
CREATE TABLE schedule_repeat (
    id BIGINT NOT NULL AUTO_INCREMENT,
    schedule_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    repeat_type VARCHAR(50),
    repeat_end_type VARCHAR(50),
    repeat_end_date DATETIME(6),
    repeat_count BIGINT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT FK_schedule_repeat_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id) ON DELETE CASCADE,
    CONSTRAINT FK_schedule_repeat_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_schedule_repeat_schedule_id (schedule_id),
    INDEX idx_schedule_repeat_user_id (user_id),
    INDEX idx_schedule_repeat_type (repeat_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ================================================
-- End of Initial Schema Migration
-- ================================================