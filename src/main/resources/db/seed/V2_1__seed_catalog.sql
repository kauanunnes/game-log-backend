-- Dados de exemplo para desenvolvimento. Notas e contagens do IGDB são aproximadas.

INSERT INTO genres (name, slug) VALUES
    ('Role-playing (RPG)', 'role-playing-rpg'),
    ('Adventure', 'adventure'),
    ('Platform', 'platform'),
    ('Indie', 'indie'),
    ('Shooter', 'shooter'),
    ('Simulator', 'simulator'),
    ('Strategy', 'strategy'),
    ('Puzzle', 'puzzle'),
    ('Hack and slash/Beat ''em up', 'hack-and-slash-beat-em-up');

INSERT INTO platforms (name, abbreviation, slug) VALUES
    ('PC (Microsoft Windows)', 'PC', 'win'),
    ('PlayStation 3', 'PS3', 'ps3'),
    ('PlayStation 4', 'PS4', 'ps4'),
    ('PlayStation 5', 'PS5', 'ps5'),
    ('Xbox 360', 'X360', 'xbox360'),
    ('Xbox One', 'XONE', 'xboxone'),
    ('Xbox Series X|S', 'Series X|S', 'series-x-s'),
    ('Nintendo Switch', 'Switch', 'switch'),
    ('Wii U', 'WiiU', 'wiiu'),
    ('Super Nintendo Entertainment System', 'SNES', 'snes'),
    ('Game Boy', 'GB', 'gb');

INSERT INTO games (slug, title, title_normalized, summary, release_date, igdb_rating, igdb_rating_count) VALUES
    ('the-witcher-3-wild-hunt', 'The Witcher 3: Wild Hunt', 'the witcher 3: wild hunt',
     'Geralt de Rívia atravessa um continente devastado pela guerra em busca de Ciri.', '2015-05-19', 93.5, 3900),
    ('hollow-knight', 'Hollow Knight', 'hollow knight',
     'Um cavaleiro silencioso explora as ruínas do reino subterrâneo de Hallownest.', '2017-02-24', 89.6, 2100),
    ('hollow-knight-silksong', 'Hollow Knight: Silksong', 'hollow knight: silksong',
     'Hornet é levada a um reino desconhecido e precisa chegar ao topo de uma cidadela.', '2025-09-04', 91.0, 600),
    ('celeste', 'Celeste', 'celeste',
     'Madeline enfrenta a escalada da montanha Celeste e os próprios medos.', '2018-01-25', 89.9, 1400),
    ('hades', 'Hades', 'hades',
     'Zagreus, filho de Hades, tenta fugir do submundo, uma tentativa de cada vez.', '2020-09-17', 92.1, 1500),
    ('elden-ring', 'Elden Ring', 'elden ring',
     'Um Maculado percorre as Terras Intermédias em busca do Anel Prístino.', '2022-02-25', 94.2, 2500),
    ('red-dead-redemption-2', 'Red Dead Redemption 2', 'red dead redemption 2',
     'Arthur Morgan e a gangue de Dutch van der Linde tentam sobreviver ao fim do Velho Oeste.', '2018-10-26', 92.4, 2600),
    ('stardew-valley', 'Stardew Valley', 'stardew valley',
     'Herde a fazenda do avô e reconstrua a vida no vilarejo de Pelican Town.', '2016-02-26', 87.8, 1800),
    ('chrono-trigger', 'Chrono Trigger', 'chrono trigger',
     'Crono e seus amigos viajam no tempo para impedir o fim do mundo.', '1995-03-11', 91.7, 1100),
    ('super-mario-world', 'Super Mario World', 'super mario world',
     'Mario e Yoshi atravessam Dinosaur Land para resgatar a princesa.', '1990-11-21', 89.0, 1000),
    ('baldurs-gate-3', 'Baldur''s Gate 3', 'baldur''s gate 3',
     'Um grupo infectado por um parasita mental tenta se salvar antes da transformação.', '2023-08-03', 94.6, 1300),
    ('pokemon-red', 'Pokémon Red', 'pokemon red',
     'Capture, treine e batalhe com Pokémon pela região de Kanto.', '1996-02-27', 82.3, 1200),
    ('the-legend-of-zelda-breath-of-the-wild', 'The Legend of Zelda: Breath of the Wild',
     'the legend of zelda: breath of the wild',
     'Link desperta depois de cem anos e explora Hyrule para derrotar Calamity Ganon.', '2017-03-03', 93.8, 3000),
    ('disco-elysium', 'Disco Elysium', 'disco elysium',
     'Um detetive sem memória investiga um assassinato na cidade de Revachol.', '2019-10-15', 90.4, 900),
    ('portal-2', 'Portal 2', 'portal 2',
     'Chell volta à Aperture Science e enfrenta GLaDOS com a arma de portais.', '2011-04-19', 93.2, 3200),
    ('sekiro-shadows-die-twice', 'Sekiro: Shadows Die Twice', 'sekiro: shadows die twice',
     'Um shinobi busca vingança e o resgate do seu jovem senhor no Japão feudal.', '2019-03-22', 90.8, 1600);

INSERT INTO game_genres (game_id, genre_id)
SELECT g.id, ge.id
FROM (VALUES
    ('the-witcher-3-wild-hunt', 'role-playing-rpg'), ('the-witcher-3-wild-hunt', 'adventure'),
    ('hollow-knight', 'platform'), ('hollow-knight', 'adventure'), ('hollow-knight', 'indie'),
    ('hollow-knight-silksong', 'platform'), ('hollow-knight-silksong', 'adventure'), ('hollow-knight-silksong', 'indie'),
    ('celeste', 'platform'), ('celeste', 'indie'),
    ('hades', 'role-playing-rpg'), ('hades', 'hack-and-slash-beat-em-up'), ('hades', 'indie'),
    ('elden-ring', 'role-playing-rpg'), ('elden-ring', 'adventure'),
    ('red-dead-redemption-2', 'shooter'), ('red-dead-redemption-2', 'adventure'),
    ('stardew-valley', 'simulator'), ('stardew-valley', 'role-playing-rpg'), ('stardew-valley', 'indie'),
    ('chrono-trigger', 'role-playing-rpg'),
    ('super-mario-world', 'platform'),
    ('baldurs-gate-3', 'role-playing-rpg'), ('baldurs-gate-3', 'strategy'), ('baldurs-gate-3', 'adventure'),
    ('pokemon-red', 'role-playing-rpg'),
    ('the-legend-of-zelda-breath-of-the-wild', 'adventure'),
    ('disco-elysium', 'role-playing-rpg'), ('disco-elysium', 'adventure'), ('disco-elysium', 'indie'),
    ('portal-2', 'puzzle'), ('portal-2', 'shooter'), ('portal-2', 'platform'),
    ('sekiro-shadows-die-twice', 'adventure'), ('sekiro-shadows-die-twice', 'hack-and-slash-beat-em-up')
) AS v (game, genre)
JOIN games g ON g.slug = v.game
JOIN genres ge ON ge.slug = v.genre;

INSERT INTO game_platforms (game_id, platform_id)
SELECT g.id, p.id
FROM (VALUES
    ('the-witcher-3-wild-hunt', 'win'), ('the-witcher-3-wild-hunt', 'ps4'), ('the-witcher-3-wild-hunt', 'xboxone'),
    ('the-witcher-3-wild-hunt', 'switch'), ('the-witcher-3-wild-hunt', 'ps5'), ('the-witcher-3-wild-hunt', 'series-x-s'),
    ('hollow-knight', 'win'), ('hollow-knight', 'switch'), ('hollow-knight', 'ps4'), ('hollow-knight', 'xboxone'),
    ('hollow-knight-silksong', 'win'), ('hollow-knight-silksong', 'switch'), ('hollow-knight-silksong', 'ps5'),
    ('hollow-knight-silksong', 'series-x-s'),
    ('celeste', 'win'), ('celeste', 'switch'), ('celeste', 'ps4'), ('celeste', 'xboxone'),
    ('hades', 'win'), ('hades', 'switch'), ('hades', 'ps4'), ('hades', 'ps5'), ('hades', 'xboxone'), ('hades', 'series-x-s'),
    ('elden-ring', 'win'), ('elden-ring', 'ps4'), ('elden-ring', 'ps5'), ('elden-ring', 'xboxone'), ('elden-ring', 'series-x-s'),
    ('red-dead-redemption-2', 'win'), ('red-dead-redemption-2', 'ps4'), ('red-dead-redemption-2', 'xboxone'),
    ('stardew-valley', 'win'), ('stardew-valley', 'switch'), ('stardew-valley', 'ps4'), ('stardew-valley', 'xboxone'),
    ('chrono-trigger', 'snes'),
    ('super-mario-world', 'snes'),
    ('baldurs-gate-3', 'win'), ('baldurs-gate-3', 'ps5'), ('baldurs-gate-3', 'series-x-s'),
    ('pokemon-red', 'gb'),
    ('the-legend-of-zelda-breath-of-the-wild', 'switch'), ('the-legend-of-zelda-breath-of-the-wild', 'wiiu'),
    ('disco-elysium', 'win'), ('disco-elysium', 'ps4'), ('disco-elysium', 'ps5'), ('disco-elysium', 'switch'),
    ('disco-elysium', 'xboxone'), ('disco-elysium', 'series-x-s'),
    ('portal-2', 'win'), ('portal-2', 'ps3'), ('portal-2', 'xbox360'),
    ('sekiro-shadows-die-twice', 'win'), ('sekiro-shadows-die-twice', 'ps4'), ('sekiro-shadows-die-twice', 'xboxone')
) AS v (game, platform)
JOIN games g ON g.slug = v.game
JOIN platforms p ON p.slug = v.platform;
