-- ================================================
-- Konnect Database Schema Migration
-- Version: 4.0 - Rename nickname column to name
-- ================================================

-- Rename nickname column to name in user table
ALTER TABLE user
    CHANGE COLUMN nickname name VARCHAR(100);

-- ================================================
-- End of Migration V4
-- ================================================