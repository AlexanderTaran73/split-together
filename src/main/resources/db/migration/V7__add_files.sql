CREATE TABLE file_owner_types (
    id   SERIAL       PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

INSERT INTO file_owner_types (code, name) VALUES
    ('GROUP',   'Файл группы'),
    ('EXPENSE', 'Чек траты');

CREATE TABLE files (
    id            BIGSERIAL    PRIMARY KEY,
    owner_type_id INTEGER                  NOT NULL REFERENCES file_owner_types(id),
    owner_id      BIGINT                   NOT NULL,
    uploaded_by   BIGINT                   NOT NULL REFERENCES users(id),
    object_key    VARCHAR(512)             NOT NULL UNIQUE,
    original_name VARCHAR(255)             NOT NULL,
    content_type  VARCHAR(255),
    size_bytes    BIGINT                   NOT NULL,
    description   VARCHAR(500),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMP WITH TIME ZONE,
    deleted_by    BIGINT                             REFERENCES users(id)
);

CREATE INDEX idx_files_owner ON files (owner_type_id, owner_id) WHERE deleted_at IS NULL;
