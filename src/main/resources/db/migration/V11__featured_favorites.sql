-- Favoritos em destaque (RF38): até 5, em ordem, no topo do perfil. Só um favorito pode ficar em destaque.
ALTER TABLE library_entries
    ADD COLUMN favorite_position SMALLINT CHECK (favorite_position BETWEEN 1 AND 5),
    ADD CONSTRAINT library_entries_featured_is_favorite CHECK (favorite_position IS NULL OR favorite),
    -- Adiado até o commit, para trocar a ordem numa transação só.
    ADD CONSTRAINT library_entries_featured_unique UNIQUE (user_id, favorite_position) DEFERRABLE INITIALLY DEFERRED;
