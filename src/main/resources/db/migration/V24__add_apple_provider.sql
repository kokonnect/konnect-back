-- V24__add_apple_provider.sql

-- 기존 enum 컬럼에 APPLE 추가
ALTER TABLE social_account
    MODIFY COLUMN provider ENUM('GOOGLE', 'KAKAO', 'NAVER', 'APPLE') NOT NULL;