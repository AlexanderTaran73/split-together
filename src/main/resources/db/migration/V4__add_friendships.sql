CREATE TABLE friendship_statuses (
    id   SERIAL       PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

INSERT INTO friendship_statuses (code, name) VALUES
    ('PENDING',  'Ожидает подтверждения'),
    ('ACCEPTED', 'В друзьях'),
    ('BLOCKED',  'Заблокирован');

CREATE TABLE friendships (
    id           BIGSERIAL PRIMARY KEY,
    requester_id BIGINT                   NOT NULL REFERENCES users(id),
    addressee_id BIGINT                   NOT NULL REFERENCES users(id),
    status_id    INTEGER                  NOT NULL REFERENCES friendship_statuses(id),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    responded_at TIMESTAMP WITH TIME ZONE,
    CHECK (requester_id <> addressee_id)
);

CREATE UNIQUE INDEX uq_friendships_pair
    ON friendships (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id));

CREATE INDEX idx_friendships_requester_id ON friendships(requester_id);
CREATE INDEX idx_friendships_addressee_id ON friendships(addressee_id);
