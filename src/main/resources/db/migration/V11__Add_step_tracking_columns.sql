-- document_analysis 테이블에 단계 추적 컬럼 추가
ALTER TABLE document_analysis
ADD COLUMN total_steps INT DEFAULT 0 COMMENT '총 처리 단계 수',
ADD COLUMN completed_steps INT DEFAULT 0 COMMENT '완료된 단계 수',
ADD COLUMN failed_step VARCHAR(100) NULL COMMENT '실패한 단계명 (있는 경우)';

-- 인덱스 추가 (실패한 분석 조회용)
ALTER TABLE document_analysis
ADD INDEX idx_document_analysis_failed_step (failed_step);
