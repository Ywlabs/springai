CREATE TABLE IF NOT EXISTS chats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_message VARCHAR(1000) NOT NULL,
    assistant_message TEXT,
    request_time TIMESTAMP NOT NULL,
    response_time TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(1000)
); 