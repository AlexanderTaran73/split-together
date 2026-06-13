CREATE TABLE outbox_events (
    id              BIGSERIAL     PRIMARY KEY,
    event_type      VARCHAR(64)   NOT NULL,
    payload         JSONB         NOT NULL,
    version         SMALLINT      NOT NULL DEFAULT 1,
    attempts        INTEGER       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMP WITH TIME ZONE,
    failed_at       TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_outbox_events_pending
    ON outbox_events (next_attempt_at)
    WHERE processed_at IS NULL AND failed_at IS NULL;
