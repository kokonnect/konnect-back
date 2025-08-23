-- ================================================
-- Konnect Database Schema Migration
-- Version: 3.0 - Update user table to match User entity
-- ================================================

-- 1. Add email column
ALTER TABLE user
ADD COLUMN email VARCHAR(255) AFTER id;

-- 2. Copy social_id data to email if exists (임시)
UPDATE user 
SET email = social_id 
WHERE social_id IS NOT NULL;

-- 3. Add unique constraint to email
ALTER TABLE user 
ADD UNIQUE KEY UK_user_email (email);

-- 4. Drop unused columns
ALTER TABLE user
DROP COLUMN social_id,
DROP COLUMN provider,
DROP COLUMN registered_at,
DROP COLUMN birth_date;

-- 5. Update guest default value from true to false
ALTER TABLE user
ALTER COLUMN guest SET DEFAULT false;

-- ================================================
-- End of Migration V3
-- ================================================