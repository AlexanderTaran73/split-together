CREATE TABLE device_platforms (
    id   SERIAL       PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

INSERT INTO device_platforms (code, name) VALUES
    ('ANDROID', 'Android'),
    ('IOS',     'iOS'),
    ('WEB',     'Веб');

CREATE TABLE device_tokens (
    id           BIGSERIAL     PRIMARY KEY,
    user_id      BIGINT        NOT NULL REFERENCES users(id),
    platform_id  INTEGER       NOT NULL REFERENCES device_platforms(id),
    token        VARCHAR(512)  NOT NULL UNIQUE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_device_tokens_user ON device_tokens (user_id);
