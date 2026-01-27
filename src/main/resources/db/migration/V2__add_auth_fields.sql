-- V2__add_auth_fields.sql
-- Add authentication fields to app_user and create refresh_token table

-- Add authentication fields to app_user table
ALTER TABLE app_user
    ADD COLUMN email VARCHAR(255),
    ADD COLUMN password_hash VARCHAR(255) DEFAULT '',
    ADD COLUMN display_name VARCHAR(255),
    ADD COLUMN avatar_url VARCHAR(512);

-- Set defaults for existing rows (if any)
UPDATE app_user SET password_hash = '' WHERE password_hash IS NULL;

-- Make email and password_hash required and unique
ALTER TABLE app_user ALTER COLUMN email SET NOT NULL;
ALTER TABLE app_user ALTER COLUMN password_hash SET NOT NULL;
ALTER TABLE app_user ALTER COLUMN password_hash DROP DEFAULT;
ALTER TABLE app_user ADD CONSTRAINT uk_app_user_email UNIQUE (email);

-- Create refresh_token table
CREATE TABLE refresh_token (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

-- Create indexes for better query performance
CREATE INDEX idx_refresh_token_user_id ON refresh_token(user_id);
CREATE INDEX idx_refresh_token_token ON refresh_token(token);
CREATE INDEX idx_refresh_token_expires_at ON refresh_token(expires_at);
CREATE INDEX idx_app_user_email ON app_user(email);
