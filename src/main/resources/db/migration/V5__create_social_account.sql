CREATE TABLE IF NOT EXISTS social_account (
                                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                              user_id BIGINT NOT NULL,
                                              provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    email_from_idp VARCHAR(255),
    picture_url VARCHAR(1024),
    CONSTRAINT uk_provider_id UNIQUE (provider, provider_user_id),
    CONSTRAINT fk_social_user FOREIGN KEY (user_id) REFERENCES user(id)
    );
