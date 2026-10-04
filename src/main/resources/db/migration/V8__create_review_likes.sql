-- Curtidas em avaliações (RF52). Apagar a conta ou a entrada leva as curtidas junto.
CREATE TABLE review_likes (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    entry_id BIGINT NOT NULL REFERENCES library_entries (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, entry_id)
);

-- A PK atende "o que eu curti"; este índice conta as curtidas de cada avaliação.
CREATE INDEX review_likes_entry_idx ON review_likes (entry_id);
