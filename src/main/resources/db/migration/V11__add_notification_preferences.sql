CREATE TABLE notification_types (
    id   SERIAL       PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

INSERT INTO notification_types (code, name) VALUES
    ('GROUP_INVITATION',     'Приглашение в группу'),
    ('EXPENSE_ADDED',        'Новая трата'),
    ('SETTLEMENT_REQUESTED', 'Запрос на подтверждение пополнения'),
    ('SETTLEMENT_CONFIRMED', 'Пополнение подтверждено');

CREATE TABLE notification_channels (
    id   SERIAL       PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

INSERT INTO notification_channels (code, name) VALUES
    ('EMAIL', 'Электронная почта'),
    ('PUSH',  'Push-уведомления');

CREATE TABLE notification_preferences (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT    NOT NULL REFERENCES users(id),
    notification_type_id INTEGER   NOT NULL REFERENCES notification_types(id),
    channel_id           INTEGER   NOT NULL REFERENCES notification_channels(id),
    enabled              BOOLEAN   NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, notification_type_id, channel_id)
);

CREATE INDEX idx_notification_preferences_user ON notification_preferences (user_id);
