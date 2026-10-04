-- Embeddings dos jogos (Fase 3), com pgvector. O modelo de agora (all-MiniLM-L6-v2) gera 384 dimensões; outro modelo
-- pede uma migration nova e reindexar tudo, porque vetores de modelos diferentes não se comparam.
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE game_embeddings (
    game_id    bigint PRIMARY KEY REFERENCES games (id) ON DELETE CASCADE,
    embedding  vector(384)  NOT NULL,
    -- SHA-256 do texto que gerou o vetor: com o mesmo texto e o mesmo modelo, não recalcula
    text_hash  char(64)     NOT NULL,
    model      varchar(100) NOT NULL,
    updated_at timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX game_embeddings_embedding_idx ON game_embeddings USING hnsw (embedding vector_cosine_ops);
