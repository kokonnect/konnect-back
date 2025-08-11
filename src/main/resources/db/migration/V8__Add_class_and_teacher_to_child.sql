-- ================================================
-- Konnect Database Schema Migration
-- Version: 8.0 - Add class_name and teacher_name to child table
-- ================================================

-- Add class_name and teacher_name columns to child table
ALTER TABLE child 
ADD COLUMN class_name VARCHAR(255),
ADD COLUMN teacher_name VARCHAR(255);

-- ================================================
-- End of Migration V8
-- ================================================