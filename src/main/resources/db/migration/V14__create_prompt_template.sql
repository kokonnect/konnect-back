CREATE TABLE IF NOT EXISTS prompt_template
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    module_name VARCHAR(100) NOT NULL,
    version     INT          NOT NULL,
    template    TEXT         NOT NULL,
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL,

    CONSTRAINT pk_prompt_template
        PRIMARY KEY (id),

    CONSTRAINT uk_prompt_template_module_version
        UNIQUE (module_name, version)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;