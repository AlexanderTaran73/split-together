CREATE TABLE search_visibilities (
    id   SERIAL       PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

INSERT INTO search_visibilities (code, name) VALUES
    ('EVERYONE', 'Все пользователи'),
    ('FRIENDS',  'Только друзья'),
    ('NOBODY',   'Никто');

CREATE TABLE group_invite_policies (
    id   SERIAL       PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

INSERT INTO group_invite_policies (code, name) VALUES
    ('ANYONE',      'Любой пользователь'),
    ('FRIENDS',     'Только друзья'),
    ('INVITE_ONLY', 'Только по ссылке');

ALTER TABLE users ADD COLUMN search_visibility_id   INTEGER REFERENCES search_visibilities(id);
ALTER TABLE users ADD COLUMN group_invite_policy_id INTEGER REFERENCES group_invite_policies(id);

UPDATE users SET
    search_visibility_id   = (SELECT id FROM search_visibilities WHERE code = 'EVERYONE'),
    group_invite_policy_id = (SELECT id FROM group_invite_policies WHERE code = 'ANYONE');

ALTER TABLE users ALTER COLUMN search_visibility_id   SET NOT NULL;
ALTER TABLE users ALTER COLUMN group_invite_policy_id SET NOT NULL;
