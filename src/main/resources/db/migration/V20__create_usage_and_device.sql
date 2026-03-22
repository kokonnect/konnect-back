-- V20__create_usage_and_device.sql

CREATE TABLE user_usage (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,

                            identity_type VARCHAR(20) NOT NULL,
                            identity_key VARCHAR(255) NOT NULL,
                            usage_type VARCHAR(20) NOT NULL,

                            date DATE NOT NULL,
                            count INT NOT NULL DEFAULT 0,

                            CONSTRAINT uk_usage_identity_date
                                UNIQUE (identity_type, identity_key, usage_type, date)
);

CREATE TABLE device (
                        device_uuid VARCHAR(255) PRIMARY KEY,

                        user_id BIGINT,

                        created_at DATETIME,
                        last_used_at DATETIME,

                        CONSTRAINT fk_device_user
                            FOREIGN KEY (user_id) REFERENCES `user`(id)
                                ON DELETE SET NULL
);