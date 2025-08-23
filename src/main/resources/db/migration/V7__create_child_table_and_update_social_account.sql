-- V7: SocialAccount 테이블 업데이트

-- Child 테이블은 이미 올바른 구조로 존재함 (id, user_id, name, school, grade, birth_date, class_name, teacher_name, created_at, updated_at)

-- 1. SocialAccount 테이블에 BaseEntity 컬럼 추가
ALTER TABLE social_account
ADD COLUMN created_at DATETIME(6),
ADD COLUMN updated_at DATETIME(6);

-- 2. SocialAccount 테이블의 불필요한 컬럼들 삭제
ALTER TABLE social_account
DROP COLUMN display_name,
DROP COLUMN email_from_idp,
DROP COLUMN picture_url;