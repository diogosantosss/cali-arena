TRUNCATE TABLE
    match_progress, screen_routines, tournament_state, matches,
    brackets, exercises, endurance_routines, tournaments,
    athletes, clubs, tokens, users
    RESTART IDENTITY CASCADE;

-- users / tokens password Admin123! Santos123!
INSERT INTO users (username, password, role, created_at)
VALUES ('admin', '$2a$10$2kAtmal2xxnA/OS5WJ7ugOfjEO24o4KQiU3GPpMLWz4YL1CaywGzm', 'ADMIN', 1787328276),
       ('Santos', '$2a$10$nkPckXGoB84z0DtdU.J6Qu7tOReSiPNfE7CaWcho/zEI28suW.2ue', 'JUDGE', 1787328276);

-- clubs
INSERT INTO clubs (name, short_name, created_at)
VALUES ('BG BARS TEAM', 'BG BARS', 1787328276),
       ('BAR WINGS', 'BW', 1787328276);

-- athletes
INSERT INTO athletes (name, gender, club_id, created_at)
VALUES ('Athlete 1', 'MALE', 1, 1787328276),
       ('Athlete 2', 'MALE', 1, 1787328276),
       ('Athlete 3', 'MALE', 1, 1787328276),
       ('Athlete 4', 'MALE', 1, 1787328276),
       ('Athlete 5', 'MALE', 2, 1787328276),
       ('Athlete 6', 'MALE', 2, 1787328276),
       ('Athlete 7', 'MALE', 2, 1787328276),
       ('Athlete 8', 'MALE', 2, 1787328276);

-- tournament + screen state
INSERT INTO tournaments (name, location, start_date, end_date, status, created_at)
VALUES ('BAR-WINGS ENDURANCE CHAMPIONSHIP 2026',
        'Caldas da Rainha, Portugal',
        1787328276,
        null,
        'READY',
        1787328276);

INSERT INTO tournament_state (tournament_id, current_screen, current_match_id, current_bracket_id, updated_at)
VALUES (1, 'WAITING', NULL, NULL, 1787328276);

-- brackets
INSERT INTO brackets (tournament_id, division, stage, created_at)
VALUES (1, 'ELITE MALE', 'QUALIFIERS', 1787328276),
       (1, 'ELITE MALE', 'QUARTERFINALS', 1787328276),
       (1, 'ELITE MALE', 'SEMIFINALS', 1787328276),
       (1, 'ELITE MALE', 'FINALS', 1787328276);
-- routines
INSERT INTO endurance_routines (name, time_cap_seconds, created_at)
VALUES ('Qualifiers', 600, 1787328276),
       ('Quarterfinals (MEN) ELITE', 720, 1787328276),
       ('Semi-Final (MEN) ELITE', 900, 1787328276),
       ('Final (MEN) ELITE', 1080, 1787328276);

-- matches
-- qualifiers (pending) + quarterfinals -> semifinals -> final (finished, vencedor preenchido)
INSERT INTO matches (bracket_id, routine_id, judge_id, athlete_red_id, athlete_blue_id, winner_athlete_id, status,
                     started_at, finished_at, created_at)
VALUES
    -- QUALIFIERS
    (1, 1, 1, 1, 2, null, 'PENDING', null, null, 1787328276),
    -- QUARTERFINALS
    (2, 2, 1, 1, 2, 1, 'FINISHED', 1787329000, 1787329120, 1787328276),
    (2, 2, 1, 3, 4, 3, 'FINISHED', 1787329100, 1787329225, 1787328276),
    (2, 2, 1, 5, 6, 5, 'FINISHED', 1787329200, 1787329330, 1787328276),
    (2, 2, 1, 7, 8, 7, 'FINISHED', 1787329300, 1787329410, 1787328276),
    -- SEMIFINALS
    (3, 3, 1, 1, 3, 1, 'FINISHED', 1787330100, 1787330230, 1787328276),
    (3, 3, 1, 5, 7, 5, 'FINISHED', 1787330200, 1787330335, 1787328276),
    -- FINAL
    (4, 4, 1, 1, 5, 1, 'FINISHED', 1787331000, 1787331140, 1787328276);

-- exercises
INSERT INTO exercises (routine_id, name, target_reps, added_weight, exercise_order, superset_order, type)
VALUES
    -- Qualifiers
    (1, 'Pull-Ups', 15, NULL, 1, NULL, 'NORMAL'),
    (1, 'Dips', 20, NULL, 2, NULL, 'NORMAL'),
    (1, 'Squats', 30, 20.0, 3, NULL, 'NORMAL'),
    (1, 'Low-Bar Push-Ups', 40, NULL, 4, NULL, 'NORMAL'),
    (1, 'Pull-Ups', 10, NULL, 5, NULL, 'NORMAL'),
    (1, 'Muscle-Ups', 5, NULL, 6, NULL, 'NORMAL'),
    -- Quarterfinals (MEN) ELITE
    (2, 'Muscle-Ups', 1, NULL, 1, 0, 'SUPERSET'),
    (2, 'Straight-Bar-Dips', 10, NULL, 1, 1, 'SUPERSET'),
    (2, 'Pull-Ups', 10, NULL, 1, 2, 'SUPERSET'),
    (2, 'Low-Bar Push-Ups', 20, NULL, 2, NULL, 'NORMAL'),
    (2, 'Squats', 20, 20.0, 3, NULL, 'NORMAL'),
    (2, 'Muscle-Ups', 2, NULL, 4, 0, 'SUPERSET'),
    (2, 'Straight-Bar-Dips', 10, NULL, 4, 1, 'SUPERSET'),
    (2, 'Pull-Ups', 10, NULL, 4, 2, 'SUPERSET'),
    (2, 'Dips', 30, 10.0, 5, NULL, 'NORMAL'),
    (2, 'Squats', 20, 20.0, 6, NULL, 'NORMAL'),
    (2, 'Muscle-Ups', 3, NULL, 7, 0, 'SUPERSET'),
    (2, 'Straight-Bar-Dips', 10, NULL, 7, 1, 'SUPERSET'),
    (2, 'Pull-Ups', 10, NULL, 7, 2, 'SUPERSET'),
    (2, 'Low-Bar Push-Ups', 30, NULL, 8, NULL, 'NORMAL'),
    (2, 'Pull-Ups', 10, 10.0, 9, NULL, 'NORMAL'),
    -- Semi-Final (MEN) ELITE
    (3, 'Pull-Ups', 10, NULL, 1, 0, 'SUPERSET'),
    (3, 'Muscle-Ups', 1, NULL, 1, 1, 'SUPERSET'),
    (3, 'Straight-Bar-Dips', 10, NULL, 1, 2, 'SUPERSET'),
    (3, 'Squats', 20, 20.0, 2, NULL, 'NORMAL'),
    (3, 'Pull-Ups', 15, NULL, 3, 0, 'SUPERSET'),
    (3, 'Muscle-Ups', 2, NULL, 3, 1, 'SUPERSET'),
    (3, 'Straight-Bar-Dips', 15, NULL, 3, 2, 'SUPERSET'),
    (3, 'Squats', 20, 20.0, 4, NULL, 'NORMAL'),
    (3, 'Pull-Ups', 20, NULL, 5, 0, 'SUPERSET'),
    (3, 'Muscle-Ups', 3, NULL, 5, 1, 'SUPERSET'),
    (3, 'Straight-Bar-Dips', 20, NULL, 5, 2, 'SUPERSET'),
    (3, 'Dips', 30, 15.0, 6, NULL, 'NORMAL'),
    (3, 'Pull-Ups', 15, 15.0, 7, NULL, 'NORMAL'),
    (3, 'Low-Bar Push-Ups', 40, NULL, 8, NULL, 'NORMAL'),
    (3, 'Muscle-Ups', 5, NULL, 9, NULL, 'NORMAL'),
    -- Final (MEN) ELITE
    (4, 'Muscle-Ups', 10, NULL, 1, NULL, 'NORMAL'),
    (4, 'Pull-Ups', 20, 20.0, 2, NULL, 'NORMAL'),
    (4, 'Squats', 30, 20.0, 3, NULL, 'NORMAL'),
    (4, 'Low-Bar Push-Ups', 50, NULL, 4, NULL, 'NORMAL'),
    (4, 'Pull-Up to Muscle-Up', 10, NULL, 5, NULL, 'UNBROKEN'),
    (4, 'Squats', 30, 20.0, 6, NULL, 'NORMAL'),
    (4, 'Dips', 40, 20.0, 7, NULL, 'NORMAL'),
    (4, 'Pull-Ups', 50, NULL, 8, NULL, 'NORMAL'),
    (4, 'Muscle-Ups', 10, NULL, 9, NULL, 'NORMAL');
