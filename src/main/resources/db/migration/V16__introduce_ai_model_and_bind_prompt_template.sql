CREATE TABLE IF NOT EXISTS ai_model
(
    id       BIGINT PRIMARY KEY AUTO_INCREMENT,
    name     VARCHAR(30),
    use_case VARCHAR(100)
);

INSERT INTO ai_model(id, name, use_case)
VALUES (1, 'gemini-2.0-flash-lite', '단순 번역, 요약, 간단한 텍스트 처리'),
       (2, 'gemini-2.0-flash', '복잡한 분석, 문서 분류, 정보 추출, Vision');

# temperature, top-p, top-k는 전체 통일하여 하드코딩
ALTER TABLE prompt_template
    ADD COLUMN status     ENUM ('DEPRECATED', 'ACTIVE', 'DRAFT'),
    ADD COLUMN max_tokens INTEGER,
    ADD COLUMN model_id   BIGINT;

# 초기값은 전부 활성화 상태
UPDATE prompt_template SET status = 'ACTIVE';

UPDATE prompt_template SET model_id = 2, max_tokens = 8000 WHERE module_name = 'OCR';
UPDATE prompt_template SET model_id = 1, max_tokens = 500 WHERE module_name = 'CLASSIFICATION';
UPDATE prompt_template SET model_id = 1, max_tokens = 500 WHERE module_name = 'SUMMARIZATION';
UPDATE prompt_template SET model_id = 1, max_tokens = 4000 WHERE module_name = 'TRANSLATION';
UPDATE prompt_template SET model_id = 1, max_tokens = 4000 WHERE module_name = 'SIMPLIFICATION';
UPDATE prompt_template SET model_id = 1, max_tokens = 1500 WHERE module_name = 'DIFFICULT_EXPRESSION_EXTRACTION';
UPDATE prompt_template SET model_id = 2, max_tokens = 3000 WHERE module_name = 'UNIFIED_EXTRACTION';

ALTER TABLE prompt_template
    MODIFY max_tokens BIGINT NOT NULL,
    MODIFY model_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_prompt_model FOREIGN KEY (model_id) REFERENCES ai_model (id);