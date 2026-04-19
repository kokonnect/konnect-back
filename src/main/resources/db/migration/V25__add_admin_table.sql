CREATE TABLE `admin`
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    login_id   VARCHAR(64)  NOT NULL,
    password   VARCHAR(255) NOT NULL,
    name       VARCHAR(100) NULL,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_login_id (login_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;