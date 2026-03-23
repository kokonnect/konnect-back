-- UserGeneratedMessage 수정

ALTER TABLE user_generated_message
    ADD COLUMN device_uuid VARCHAR(255);

-- 기존 user_id NOT NULL → NULL 허용으로 변경
ALTER TABLE user_generated_message
    MODIFY COLUMN user_id BIGINT NULL;


-- AnalysisHistory 수정

ALTER TABLE analysis_history
    ADD COLUMN device_uuid VARCHAR(255);

-- (이미 nullable이면 생략 가능)
ALTER TABLE analysis_history
    MODIFY COLUMN user_id BIGINT NULL;


-- 인덱스 추가 (조회 성능용, 추천)

CREATE INDEX idx_message_device_uuid ON user_generated_message(device_uuid);
CREATE INDEX idx_analysis_device_uuid ON analysis_history(device_uuid);