-- ================================================
-- Konnect Database Schema Migration
-- Version: 2.0 - Add user columns and language support
-- ================================================

-- User table updates
-- ================================================

-- 1. Add new columns to user table
ALTER TABLE user
    ADD COLUMN birth_date DATE,
    ADD COLUMN language VARCHAR(50),
    ADD COLUMN guest BOOLEAN NOT NULL DEFAULT true;

-- 2. Modify social_id to allow NULL for guest users
ALTER TABLE user MODIFY COLUMN social_id VARCHAR(255);

-- 3. Add birth_date column to child table
ALTER TABLE child
    ADD COLUMN birth_date DATE;

-- ================================================
-- End of Migration V2
-- ================================================