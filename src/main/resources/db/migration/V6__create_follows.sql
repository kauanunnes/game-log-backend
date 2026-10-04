-- Quem segue quem (RF50). Apagar a conta apaga as duas pontas (RN12).
CREATE TABLE follows (
    follower_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    followee_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (follower_id, followee_id),
    CHECK (follower_id <> followee_id)
);

-- A PK atende "quem a pessoa segue"; este índice atende "quem segue a pessoa", dos mais recentes primeiro.
CREATE INDEX follows_followee_idx ON follows (followee_id, created_at DESC);
