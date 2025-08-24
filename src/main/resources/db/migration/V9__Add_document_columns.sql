-- Add new columns to document_file table
ALTER TABLE document_file 
DROP COLUMN file_url,
ADD COLUMN file_size BIGINT AFTER file_type,
ADD COLUMN extracted_text TEXT AFTER file_size,
ADD COLUMN page_count INT AFTER extracted_text;