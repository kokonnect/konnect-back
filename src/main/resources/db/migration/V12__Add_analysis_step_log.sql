-- 분석 단계별 로그 테이블 생성
CREATE TABLE analysis_step_log (
    log_id BIGINT NOT NULL AUTO_INCREMENT,
    analysis_id BIGINT NOT NULL COMMENT 'document_analysis FK',
    step_name VARCHAR(50) NOT NULL COMMENT '단계명 (OCR, Classification, Extraction 등)',
    step_order INT NOT NULL COMMENT '단계 순서',

    -- 입력 정보
    input_text TEXT COMMENT '입력 텍스트 (일부)',
    input_length INT COMMENT '입력 텍스트 길이',

    -- 프롬프트 정보
    prompt_template VARCHAR(100) COMMENT '사용된 프롬프트 템플릿명',
    model_used VARCHAR(50) COMMENT '사용된 AI 모델명',
    temperature DOUBLE COMMENT '온도 설정값',
    max_tokens INT COMMENT '최대 토큰 수',

    -- 출력 정보
    raw_response TEXT COMMENT 'AI 원본 응답',
    parsed_result TEXT COMMENT '파싱된 결과 (JSON)',
    output_summary VARCHAR(500) COMMENT '출력 요약',

    -- 분류 관련 (Classification 단계만)
    classification_type VARCHAR(20) COMMENT '분류된 문서 유형',
    classification_confidence DOUBLE COMMENT '분류 신뢰도',
    classification_keywords VARCHAR(500) COMMENT '분류 키워드 (JSON 배열)',
    classification_reasoning TEXT COMMENT '분류 근거',

    -- 상태 및 시간
    status VARCHAR(20) NOT NULL COMMENT 'SUCCESS, FAILED, SKIPPED',
    error_message TEXT COMMENT '에러 메시지 (실패 시)',
    processing_time_ms BIGINT COMMENT '처리 시간 (밀리초)',
    created_at DATETIME(6),

    PRIMARY KEY (log_id),
    CONSTRAINT FK_analysis_step_log_analysis
        FOREIGN KEY (analysis_id) REFERENCES document_analysis(analysis_id) ON DELETE CASCADE,
    INDEX idx_step_log_analysis_id (analysis_id),
    INDEX idx_step_log_step_name (step_name),
    INDEX idx_step_log_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
