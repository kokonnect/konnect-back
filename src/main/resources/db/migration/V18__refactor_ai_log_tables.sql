# schedule 테이블에서 문서 분석 제거
DELIMITER //

DROP PROCEDURE IF EXISTS DropFkAndColumn //

CREATE PROCEDURE DropFkAndColumn()
BEGIN
    # 1. 외래 키 제약 조건이 존재하는지 확인 후 삭제
    IF EXISTS (SELECT 1
               FROM information_schema.TABLE_CONSTRAINTS
               WHERE CONSTRAINT_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'schedule'
                 AND CONSTRAINT_NAME = 'FK_schedule_document_analysis') THEN
        ALTER TABLE schedule
            DROP FOREIGN KEY FK_schedule_document_analysis;
    END IF;

    # 2. 컬럼이 존재하는지 확인 후 삭제 (MySQL 8.0.19 이상이면 IF EXISTS 사용 가능)
    # 만약 8.0.19 미만 버전을 쓰신다면 아래 IF 문을 유지하세요.
    IF EXISTS (SELECT 1
               FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'schedule'
                 AND COLUMN_NAME = 'document_analysis_id') THEN
        ALTER TABLE schedule
            DROP COLUMN document_analysis_id;
    END IF;
END //

DELIMITER ;

# 프로시저 실행
CALL DropFkAndColumn();

# 사용 후 프로시저 삭제 (깔끔한 정리)
DROP PROCEDURE DropFkAndColumn;

# 기존 로그 테이블 제거
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS analysis_step_log;
DROP TABLE IF EXISTS document_file;
DROP TABLE IF EXISTS document_translation;
DROP TABLE IF EXISTS document_analysis;
SET FOREIGN_KEY_CHECKS = 1;

# 사용자의 API 요청 기록
CREATE TABLE IF NOT EXISTS analyze_request_log
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_uuid BINARY(16) NOT NULL,
    user_id      BIGINT,
    status       VARCHAR(32),
    latency_ms   INT,
    created_at   TIMESTAMP  NOT NULL,

    UNIQUE KEY uk_request_uuid (request_uuid),
    INDEX idx_user_created (user_id, created_at) -- 특정 유저(또는 비로그인군) 최근 기록 조회
) ENGINE = InnoDB;

# 모든 LLM 호출의 맥락 기록
CREATE TABLE IF NOT EXISTS llm_call_metadata
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_uuid       BINARY(16)  NOT NULL,
    model              VARCHAR(30) NOT NULL,
    max_tokens         INT,
    input_tokens       INT,
    output_tokens      INT,
    latency_ms         INT,
    status             VARCHAR(32),
    prompt_version     INT,
    prompt_module_name VARCHAR(100),
    finish_reason      VARCHAR(32),
    created_at         TIMESTAMP   NOT NULL,

    INDEX idx_created_at (created_at),
    INDEX idx_status_created (status, created_at),
    INDEX idx_prompt_module_name_created (prompt_module_name, prompt_version, created_at)
) ENGINE = InnoDB;

# 실제 요청과 응답 (Todo 필요한 데이터만 따로 저장)
CREATE TABLE IF NOT EXISTS llm_call_raw_data
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    llm_call_id BIGINT NOT NULL,
    prompt      MEDIUMTEXT,
    response    MEDIUMTEXT,

    UNIQUE KEY uk_llm_call_id (llm_call_id) -- Metadata 조회 후 내용 호출 시 필수
) ENGINE = InnoDB;