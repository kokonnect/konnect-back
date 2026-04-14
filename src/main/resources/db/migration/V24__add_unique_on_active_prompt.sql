CREATE UNIQUE INDEX idx_one_active_per_module
    ON prompt_template((CASE WHEN status = 'ACTIVE' THEN module_name END));