-- 1. 기존 FK 제거
ALTER TABLE user_generated_message
DROP FOREIGN KEY fk_user_generated_message_user;

-- 2. NULL 허용으로 변경
ALTER TABLE user_generated_message
    MODIFY COLUMN user_id BIGINT NULL;

-- 3. FK 다시 생성 (NULL 허용 상태)
ALTER TABLE user_generated_message
    ADD CONSTRAINT fk_user_generated_message_user
        FOREIGN KEY (user_id) REFERENCES user(id)
            ON DELETE SET NULL;
