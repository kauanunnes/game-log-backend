-- Com ~10 mil jogos, as ordenações mais usadas do catálogo e a procura dos jogos a atualizar no IGDB vão pelo índice.
CREATE INDEX games_popularity_idx ON games (igdb_rating_count DESC NULLS LAST, title);
CREATE INDEX games_rating_idx ON games (igdb_rating DESC NULLS LAST, title);
CREATE INDEX games_synced_at_idx ON games (synced_at NULLS FIRST) WHERE igdb_id IS NOT NULL;
