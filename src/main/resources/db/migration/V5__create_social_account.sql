CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                     email VARCHAR(255) UNIQUE,
    name VARCHAR(255),
    language VARCHAR(50),
    email_verified TINYINT(1),
    guest TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6), updated_at DATETIME(6)
    );

CREATE TABLE IF NOT EXISTS social_account (
                                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                              user_id BIGINT NOT NULL,
                                              provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    email_from_idp VARCHAR(255),
    picture_url VARCHAR(1024),
    CONSTRAINT uk_provider_id UNIQUE (provider, provider_user_id),
    CONSTRAINT fk_social_user FOREIGN KEY (user_id) REFERENCES users(id)
    );
