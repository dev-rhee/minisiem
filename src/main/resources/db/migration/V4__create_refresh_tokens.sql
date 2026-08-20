CREATE TABLE refresh_tokens (
    id         BIGSERIAL    PRIMARY KEY,
    username   VARCHAR(100) NOT NULL UNIQUE,
    token      VARCHAR(1000) NOT NULL,
    expires_at TIMESTAMP    NOT NULL
);
