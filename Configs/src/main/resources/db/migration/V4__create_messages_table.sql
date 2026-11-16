
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    alert_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    sender VARCHAR(20) NOT NULL,
    source VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_messages_alert
        FOREIGN KEY (alert_id)
        REFERENCES alerts(id)
        ON DELETE CASCADE
);
