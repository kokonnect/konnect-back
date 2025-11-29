-- Document Analysis 테이블 생성
CREATE TABLE document_analysis (
    analysis_id BIGINT NOT NULL AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    document_type VARCHAR(20) COMMENT 'SCHEDULE, PENALTY, EVENT, NOTICE',
    classification_confidence DOUBLE,
    classification_keywords VARCHAR(500),
    classification_reasoning TEXT,
    extracted_schedules_json TEXT COMMENT 'JSON array of extracted schedules',
    additional_info_json TEXT COMMENT 'JSON object of additional extracted info',
    processing_time_ms BIGINT,
    ocr_method VARCHAR(50) COMMENT 'GOOGLE_VISION, PDF_READER, HYBRID',
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (analysis_id),
    CONSTRAINT FK_document_analysis_document
        FOREIGN KEY (document_id) REFERENCES document(document_id) ON DELETE CASCADE,
    UNIQUE KEY UK_document_analysis_document (document_id),
    INDEX idx_document_analysis_type (document_type),
    INDEX idx_document_analysis_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Schedule 테이블에 document_analysis_id 컬럼 추가
ALTER TABLE schedule
ADD COLUMN document_analysis_id BIGINT NULL COMMENT 'Reference to document analysis if created from notice',
ADD CONSTRAINT FK_schedule_document_analysis
    FOREIGN KEY (document_analysis_id)
    REFERENCES document_analysis(analysis_id) ON DELETE SET NULL,
ADD INDEX idx_schedule_document_analysis_id (document_analysis_id);
