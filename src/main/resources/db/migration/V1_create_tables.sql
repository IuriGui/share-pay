CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE payment (
    id          BIGSERIAL PRIMARY KEY,
    payer_id    BIGINT        NOT NULL REFERENCES users(id),
    description VARCHAR(255),
    amount      NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE payment_user (
    payment_id BIGINT        NOT NULL REFERENCES payment(id) ON DELETE CASCADE,
    user_id    BIGINT        NOT NULL REFERENCES users(id),
    amount     NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    paid       BOOLEAN       NOT NULL DEFAULT false,
    paid_at    TIMESTAMPTZ,
    PRIMARY KEY (payment_id, user_id)
);

CREATE INDEX idx_payment_payer      ON payment(payer_id);
CREATE INDEX idx_payment_user_user  ON payment_user(user_id);
