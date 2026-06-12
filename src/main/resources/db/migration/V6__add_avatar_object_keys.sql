ALTER TABLE users RENAME COLUMN avatar_url TO avatar_object_key;

ALTER TABLE groups ADD COLUMN avatar_object_key VARCHAR(512);
