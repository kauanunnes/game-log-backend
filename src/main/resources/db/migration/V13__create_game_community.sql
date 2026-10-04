-- Números da comunidade pré-calculados (RF23), só com perfis públicos (RN11). Os gatilhos do fim do arquivo
-- refazem a linha do jogo a cada escrita, inclusive quando uma conta muda a privacidade ou é excluída.
CREATE TABLE game_community (
    game_id            bigint PRIMARY KEY REFERENCES games (id) ON DELETE CASCADE,
    average_rating     numeric(3, 2),
    ratings_count      integer     NOT NULL DEFAULT 0,
    -- Uma faixa a cada meia estrela, de 0 a 5
    rating_counts      integer[]   NOT NULL DEFAULT array_fill(0, ARRAY [11]),
    recommend_percent  smallint,
    players_count      integer     NOT NULL DEFAULT 0,
    want_to_play_count integer     NOT NULL DEFAULT 0,
    updated_at         timestamptz NOT NULL DEFAULT now()
);

CREATE FUNCTION refresh_game_community(target bigint) RETURNS void
    LANGUAGE plpgsql AS
$$
BEGIN
    -- Trava a linha antes de contar: duas escritas no mesmo jogo esperam uma pela outra, e a segunda já conta a primeira.
    INSERT INTO game_community (game_id) VALUES (target) ON CONFLICT DO NOTHING;
    PERFORM 1 FROM game_community WHERE game_id = target FOR UPDATE;

    WITH public_entries AS (SELECT e.status, e.rating, e.recommends
                            FROM library_entries e
                                     JOIN users u ON u.id = e.user_id
                            WHERE e.game_id = target
                              AND u.profile_visibility = 'PUBLIC'),
         halves AS (SELECT floor(rating * 2)::int AS half, count(*)::int AS total
                    FROM public_entries
                    WHERE rating IS NOT NULL
                    GROUP BY 1)
    UPDATE game_community
    SET average_rating     = (SELECT round(avg(rating), 2) FROM public_entries),
        ratings_count      = (SELECT count(rating) FROM public_entries),
        rating_counts      = (SELECT array_agg(coalesce(h.total, 0) ORDER BY s.half)
                              FROM generate_series(0, 10) AS s (half)
                                       LEFT JOIN halves h USING (half)),
        recommend_percent  = (SELECT round(100.0 * count(*) FILTER (WHERE recommends) / nullif(count(recommends), 0))
                              FROM public_entries),
        players_count      = (SELECT count(*) FROM public_entries WHERE status IN ('PLAYING', 'PLAYED', 'DROPPED')),
        want_to_play_count = (SELECT count(*) FROM public_entries WHERE status IN ('BACKLOG', 'WISHLIST')),
        updated_at         = now()
    WHERE game_id = target;
END;
$$;

CREATE FUNCTION refresh_community_of_entry() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    PERFORM refresh_game_community(CASE WHEN TG_OP = 'DELETE' THEN OLD.game_id ELSE NEW.game_id END);
    RETURN NULL;
END;
$$;

-- Também roda nas exclusões em cascata de uma conta
CREATE TRIGGER library_entries_added_or_removed
    AFTER INSERT OR DELETE
    ON library_entries
    FOR EACH ROW
EXECUTE FUNCTION refresh_community_of_entry();

CREATE TRIGGER library_entries_changed
    AFTER UPDATE
    ON library_entries
    FOR EACH ROW
    WHEN (OLD.status IS DISTINCT FROM NEW.status
        OR OLD.rating IS DISTINCT FROM NEW.rating
        OR OLD.recommends IS DISTINCT FROM NEW.recommends)
EXECUTE FUNCTION refresh_community_of_entry();

CREATE FUNCTION refresh_community_of_user() RETURNS trigger
    LANGUAGE plpgsql AS
$$
DECLARE
    game bigint;
BEGIN
    -- Na ordem dos jogos, para duas contas mudando ao mesmo tempo não travarem uma à outra.
    FOR game IN SELECT game_id FROM library_entries WHERE user_id = NEW.id ORDER BY game_id
        LOOP
            PERFORM refresh_game_community(game);
        END LOOP;
    RETURN NULL;
END;
$$;

CREATE TRIGGER users_visibility_changed
    AFTER UPDATE OF profile_visibility
    ON users
    FOR EACH ROW
    WHEN (OLD.profile_visibility IS DISTINCT FROM NEW.profile_visibility)
EXECUTE FUNCTION refresh_community_of_user();

SELECT refresh_game_community(game_id)
FROM (SELECT DISTINCT game_id FROM library_entries) AS games_with_entries;
