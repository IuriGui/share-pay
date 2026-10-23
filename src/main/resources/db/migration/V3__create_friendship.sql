-- accepted_at nulo = pedido pendente, enviado por requester para addressee
CREATE TABLE friendship (
    requester_id BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    addressee_id BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    accepted_at  TIMESTAMPTZ,
    PRIMARY KEY (requester_id, addressee_id),
    CHECK (requester_id <> addressee_id)
);

-- um único vínculo por par, qualquer que seja a direção do pedido
CREATE UNIQUE INDEX uq_friendship_pair ON friendship (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id));
CREATE INDEX idx_friendship_addressee ON friendship(addressee_id);
