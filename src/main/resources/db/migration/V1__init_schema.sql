-- =============================================================
-- Справочники
-- =============================================================

CREATE TABLE platform_roles (
    id   SERIAL      PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE group_roles (
    id   SERIAL      PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE group_statuses (
    id   SERIAL      PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE membership_statuses (
    id   SERIAL      PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE invitation_types (
    id   SERIAL      PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE invitation_statuses (
    id   SERIAL      PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE email_verification_purposes (
    id   SERIAL      PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE settlement_statuses (
    id   SERIAL      PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE split_methods (
    id   SERIAL      PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE expense_categories (
    id   SERIAL      PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE currencies (
    id   SERIAL      PRIMARY KEY,
    code VARCHAR(10)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

-- =============================================================
-- Пользователи
-- =============================================================

CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    avatar_url    VARCHAR(512),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE user_platform_roles (
    user_id BIGINT  NOT NULL REFERENCES users(id),
    role_id INTEGER NOT NULL REFERENCES platform_roles(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT                   NOT NULL REFERENCES users(id),
    token_hash VARCHAR(255)             NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE email_verifications (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT                   NOT NULL REFERENCES users(id),
    code       VARCHAR(10)              NOT NULL,
    purpose_id INTEGER                  NOT NULL REFERENCES email_verification_purposes(id),
    new_email  VARCHAR(255),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    used_at    TIMESTAMP WITH TIME ZONE
);

-- =============================================================
-- Группы
-- =============================================================

CREATE TABLE groups (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    owner_id    BIGINT                   NOT NULL REFERENCES users(id),
    currency_id INTEGER                  NOT NULL REFERENCES currencies(id),
    status_id   INTEGER                  NOT NULL REFERENCES group_statuses(id),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE group_members (
    id         BIGSERIAL PRIMARY KEY,
    group_id   BIGINT                   NOT NULL REFERENCES groups(id),
    user_id    BIGINT                   NOT NULL REFERENCES users(id),
    role_id    INTEGER                  NOT NULL REFERENCES group_roles(id),
    status_id  INTEGER                  NOT NULL REFERENCES membership_statuses(id),
    joined_at  TIMESTAMP WITH TIME ZONE,
    left_at    TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (group_id, user_id)
);

CREATE TABLE group_invitations (
    id              BIGSERIAL    PRIMARY KEY,
    group_id        BIGINT                   NOT NULL REFERENCES groups(id),
    created_by      BIGINT                   NOT NULL REFERENCES users(id),
    type_id         INTEGER                  NOT NULL REFERENCES invitation_types(id),
    status_id       INTEGER                  NOT NULL REFERENCES invitation_statuses(id),
    invited_user_id BIGINT                             REFERENCES users(id),
    invite_code     VARCHAR(100)             UNIQUE,
    max_uses        INTEGER,
    expires_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    revoked_at      TIMESTAMP WITH TIME ZONE
);

CREATE TABLE invitation_uses (
    id            BIGSERIAL PRIMARY KEY,
    invitation_id BIGINT                   NOT NULL REFERENCES group_invitations(id),
    user_id       BIGINT                   NOT NULL REFERENCES users(id),
    used_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (invitation_id, user_id)
);

-- =============================================================
-- Финансы
-- =============================================================

CREATE TABLE expenses (
    id              BIGSERIAL     PRIMARY KEY,
    group_id        BIGINT                   NOT NULL REFERENCES groups(id),
    paid_by         BIGINT                   NOT NULL REFERENCES users(id),
    title           VARCHAR(200)             NOT NULL,
    description     VARCHAR(1000),
    amount          NUMERIC(19, 2)           NOT NULL CHECK (amount > 0),
    currency_id     INTEGER                  NOT NULL REFERENCES currencies(id),
    category_id     INTEGER                            REFERENCES expense_categories(id),
    split_method_id INTEGER                  NOT NULL REFERENCES split_methods(id),
    expense_date    DATE                     NOT NULL,
    created_by      BIGINT                   NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP WITH TIME ZONE,
    deleted_by      BIGINT                             REFERENCES users(id)
);

CREATE TABLE expense_participants (
    id         BIGSERIAL     PRIMARY KEY,
    expense_id BIGINT         NOT NULL REFERENCES expenses(id),
    user_id    BIGINT         NOT NULL REFERENCES users(id),
    share      NUMERIC(19, 2) NOT NULL CHECK (share >= 0),
    weight     NUMERIC(10, 4),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (expense_id, user_id)
);

CREATE TABLE balances (
    id          BIGSERIAL     PRIMARY KEY,
    group_id    BIGINT         NOT NULL REFERENCES groups(id),
    debtor_id   BIGINT         NOT NULL REFERENCES users(id),
    creditor_id BIGINT         NOT NULL REFERENCES users(id),
    amount      NUMERIC(19, 2) NOT NULL CHECK (amount >= 0),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (group_id, debtor_id, creditor_id),
    CHECK (debtor_id != creditor_id)
);

CREATE TABLE settlements (
    id           BIGSERIAL     PRIMARY KEY,
    group_id     BIGINT         NOT NULL REFERENCES groups(id),
    payer_id     BIGINT         NOT NULL REFERENCES users(id),
    receiver_id  BIGINT         NOT NULL REFERENCES users(id),
    amount       NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    currency_id  INTEGER        NOT NULL REFERENCES currencies(id),
    status_id    INTEGER        NOT NULL REFERENCES settlement_statuses(id),
    created_by   BIGINT         NOT NULL REFERENCES users(id),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    confirmed_at TIMESTAMP WITH TIME ZONE,
    rejected_at  TIMESTAMP WITH TIME ZONE
);

-- =============================================================
-- Индексы
-- =============================================================

CREATE INDEX idx_refresh_tokens_user_id       ON refresh_tokens(user_id);
CREATE INDEX idx_email_verifications_user_id  ON email_verifications(user_id);
CREATE INDEX idx_group_members_user_id        ON group_members(user_id);
CREATE INDEX idx_group_members_group_id       ON group_members(group_id);
CREATE INDEX idx_group_invitations_group_id   ON group_invitations(group_id);
CREATE INDEX idx_invitation_uses_invitation_id ON invitation_uses(invitation_id);
CREATE INDEX idx_expenses_group_id            ON expenses(group_id);
CREATE INDEX idx_expenses_paid_by             ON expenses(paid_by);
CREATE INDEX idx_expense_participants_expense_id ON expense_participants(expense_id);
CREATE INDEX idx_expense_participants_user_id ON expense_participants(user_id);
CREATE INDEX idx_balances_group_id            ON balances(group_id);
CREATE INDEX idx_balances_debtor_id           ON balances(debtor_id);
CREATE INDEX idx_balances_creditor_id         ON balances(creditor_id);
CREATE INDEX idx_settlements_group_id         ON settlements(group_id);
CREATE INDEX idx_settlements_payer_id         ON settlements(payer_id);
CREATE INDEX idx_settlements_receiver_id      ON settlements(receiver_id);
