CREATE TABLE IF NOT EXISTS analysis_history
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_log_id      BIGINT,
    user_id             BIGINT,
    file_name           VARCHAR(150),
    file_type           VARCHAR(30),
    extracted_text      TEXT,
    translated_language VARCHAR(20),
    translated_text     TEXT,
    summary             VARCHAR(1000),
    created_at          DATETIME NOT NULL
);

CREATE INDEX idx_user_created_at
    ON analysis_history (user_id, created_at);