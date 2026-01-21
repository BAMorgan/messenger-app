-- V1__init.sql
-- Initial schema creation for messenger application

-- Create app_user table
CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE
);

-- Create conversation table
CREATE TABLE conversation (
    id BIGSERIAL PRIMARY KEY,
    usera_id BIGINT NOT NULL,
    userb_id BIGINT NOT NULL,
    CONSTRAINT fk_conversation_user_a FOREIGN KEY (usera_id) REFERENCES app_user(id),
    CONSTRAINT fk_conversation_user_b FOREIGN KEY (userb_id) REFERENCES app_user(id)
);

-- Create message table
CREATE TABLE message (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    body VARCHAR(4096) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(id),
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES app_user(id)
);

-- Create indexes for better query performance
CREATE INDEX idx_conversation_user_a ON conversation(usera_id);
CREATE INDEX idx_conversation_user_b ON conversation(userb_id);
CREATE INDEX idx_message_conversation ON message(conversation_id);
CREATE INDEX idx_message_sender ON message(sender_id);
CREATE INDEX idx_message_created_at ON message(created_at);
