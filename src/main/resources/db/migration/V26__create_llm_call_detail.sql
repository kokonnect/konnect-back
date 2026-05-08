CREATE TABLE llm_call_detail
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_uuid       BINARY(16)  NOT NULL,
    prompt_module_name VARCHAR(100),
    input_vars         TEXT,
    response_text      TEXT,
    created_at         TIMESTAMP   NOT NULL,

    INDEX idx_request_uuid (request_uuid)
) ENGINE = InnoDB;
