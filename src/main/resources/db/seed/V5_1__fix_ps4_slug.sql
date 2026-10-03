-- No IGDB, o slug do PlayStation 4 é "ps4--1"; com "ps4" no seed, a importação criava uma segunda linha.

-- Quem já tem as duas: o que apontava para a linha do seed passa para a do IGDB, e ela sai.
UPDATE library_entries e
SET played_platform_id = igdb.id
FROM platforms seed, platforms igdb
WHERE seed.slug = 'ps4' AND seed.igdb_id IS NULL AND igdb.slug = 'ps4--1' AND e.played_platform_id = seed.id;

INSERT INTO game_platforms (game_id, platform_id)
SELECT gp.game_id, igdb.id
FROM game_platforms gp
JOIN platforms seed ON seed.id = gp.platform_id AND seed.slug = 'ps4' AND seed.igdb_id IS NULL
JOIN platforms igdb ON igdb.slug = 'ps4--1'
ON CONFLICT DO NOTHING;

DELETE FROM game_platforms gp
USING platforms seed
WHERE gp.platform_id = seed.id AND seed.slug = 'ps4' AND seed.igdb_id IS NULL
  AND EXISTS (SELECT 1 FROM platforms igdb WHERE igdb.slug = 'ps4--1');

DELETE FROM platforms
WHERE slug = 'ps4' AND igdb_id IS NULL AND EXISTS (SELECT 1 FROM platforms WHERE slug = 'ps4--1');

-- Quem ainda não importou o PS4: o seed passa a usar o slug do IGDB, e a importação adota a linha.
UPDATE platforms SET slug = 'ps4--1' WHERE slug = 'ps4' AND igdb_id IS NULL;
