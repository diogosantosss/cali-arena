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
       ('Athlete 5', 'MALE', 1, 1787328276),
       ('Athlete 6', 'MALE', 1, 1787328276),
       ('Athlete 7', 'MALE', 1, 1787328276),
       ('Athlete 8', 'MALE', 1, 1787328276),
       ('Athlete 9', 'MALE', 1, 1787328276),
       ('Athlete 10', 'MALE', 1, 1787328276),
       ('Athlete 11', 'MALE', 1, 1787328276),
       ('Athlete 12', 'MALE', 1, 1787328276),
       ('Athlete 13', 'MALE', 2, 1787328276),
       ('Athlete 14', 'MALE', 2, 1787328276),
       ('Athlete 15', 'MALE', 2, 1787328276),
       ('Athlete 16', 'MALE', 2, 1787328276),
       ('Athlete 17', 'MALE', 2, 1787328276),
       ('Athlete 18', 'MALE', 2, 1787328276),
       ('Athlete 19', 'MALE', 2, 1787328276),
       ('Athlete 20', 'MALE', 2, 1787328276),
       ('Athlete 21', 'MALE', 2, 1787328276),
       ('Athlete 22', 'MALE', 2, 1787328276),
       ('Athlete 23', 'MALE', 2, 1787328276),
       ('Athlete 24', 'MALE', 2, 1787328276);

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
-- 12 qualifiers (finished, all 24 athletes have times) -> quarterfinals -> semifinals -> final
INSERT INTO matches (bracket_id, routine_id, judge_id, athlete_red_id, athlete_blue_id, winner_athlete_id, status,
                     started_at, finished_at, created_at)
VALUES
    -- QUALIFIERS (24 athletes, 12 finished matches)
    (1, 1, 1, 1, 2, 1, 'FINISHED', 1787329000000, 1787329110000, 1787328276000),
    (1, 1, 1, 3, 4, 3, 'FINISHED', 1787329300000, 1787329420000, 1787328276000),
    (1, 1, 1, 5, 6, 5, 'FINISHED', 1787329600000, 1787329731000, 1787328276000),
    (1, 1, 1, 7, 8, 8, 'FINISHED', 1787329900000, 1787330015000, 1787328276000),
    (1, 1, 1, 9, 10, 10, 'FINISHED', 1787330200000, 1787330322000, 1787328276000),
    (1, 1, 1, 11, 12, 11, 'FINISHED', 1787330500000, 1787330640000, 1787328276000),
    (1, 1, 1, 13, 14, 14, 'FINISHED', 1787330800000, 1787330918000, 1787328276000),
    (1, 1, 1, 15, 16, 16, 'FINISHED', 1787331100000, 1787331226000, 1787328276000),
    (1, 1, 1, 17, 18, 17, 'FINISHED', 1787331400000, 1787331549000, 1787328276000),
    (1, 1, 1, 19, 20, 20, 'FINISHED', 1787331700000, 1787331833000, 1787328276000),
    (1, 1, 1, 21, 22, 22, 'FINISHED', 1787332000000, 1787332116000, 1787328276000),
    (1, 1, 1, 23, 24, 24, 'FINISHED', 1787332300000, 1787332444000, 1787328276000),
    -- QUARTERFINALS
    (2, 2, 1, 1, 2, 1, 'FINISHED', 1787333000000, 1787333220000, 1787328276000),
    (2, 2, 1, 3, 4, 3, 'FINISHED', 1787333100000, 1787333325000, 1787328276000),
    (2, 2, 1, 5, 6, 5, 'FINISHED', 1787333200000, 1787333430000, 1787328276000),
    (2, 2, 1, 7, 8, 7, 'FINISHED', 1787333300000, 1787333510000, 1787328276000),
    -- SEMIFINALS
    (3, 3, 1, 1, 3, 1, 'FINISHED', 1787334100000, 1787334330000, 1787328276000),
    (3, 3, 1, 5, 7, 5, 'FINISHED', 1787334200000, 1787334435000, 1787328276000),
    -- FINAL
    (4, 4, 1, 1, 5, 1, 'FINISHED', 1787335000000, 1787335240000, 1787328276000);

-- match progress (finished times per athlete, ms relative to match start)
INSERT INTO match_progress (match_id, red_current_exercise_id, blue_current_exercise_id, red_current_reps,
                            blue_current_reps, red_finished_at, blue_finished_at, timer_started_at,
                            timer_remaining_seconds, updated_at)
VALUES
    -- QUALIFIERS
    (1, NULL, NULL, 0, 0, 1787329096000, 1787329110000, 1787329000000, NULL, 1787329110000),
    (2, NULL, NULL, 0, 0, 1787329405000, 1787329420000, 1787329300000, NULL, 1787329420000),
    (3, NULL, NULL, 0, 0, 1787329699000, 1787329731000, 1787329600000, NULL, 1787329731000),
    (4, NULL, NULL, 0, 0, 1787330015000, 1787329998000, 1787329900000, NULL, 1787330015000),
    (5, NULL, NULL, 0, 0, 1787330322000, 1787330308000, 1787330200000, NULL, 1787330322000),
    (6, NULL, NULL, 0, 0, 1787330595000, 1787330640000, 1787330500000, NULL, 1787330640000),
    (7, NULL, NULL, 0, 0, 1787330918000, 1787330904000, 1787330800000, NULL, 1787330918000),
    (8, NULL, NULL, 0, 0, 1787331226000, 1787331212000, 1787331100000, NULL, 1787331226000),
    (9, NULL, NULL, 0, 0, 1787331501000, 1787331549000, 1787331400000, NULL, 1787331549000),
    (10, NULL, NULL, 0, 0, 1787331833000, 1787331827000, 1787331700000, NULL, 1787331833000),
    (11, NULL, NULL, 0, 0, 1787332116000, 1787332107000, 1787332000000, NULL, 1787332116000),
    (12, NULL, NULL, 0, 0, 1787332444000, 1787332393000, 1787332300000, NULL, 1787332444000),
    -- QUARTERFINALS
    (13, NULL, NULL, 0, 0, 1787333120000, 1787333220000, 1787333000000, NULL, 1787333220000),
    (14, NULL, NULL, 0, 0, 1787333215000, 1787333325000, 1787333100000, NULL, 1787333325000),
    (15, NULL, NULL, 0, 0, 1787333320000, 1787333430000, 1787333200000, NULL, 1787333430000),
    (16, NULL, NULL, 0, 0, 1787333410000, 1787333510000, 1787333300000, NULL, 1787333510000),
    -- SEMIFINALS
    (17, NULL, NULL, 0, 0, 1787334220000, 1787334330000, 1787334100000, NULL, 1787334330000),
    (18, NULL, NULL, 0, 0, 1787334335000, 1787334435000, 1787334200000, NULL, 1787334435000),
    -- FINAL
    (19, NULL, NULL, 0, 0, 1787335120000, 1787335240000, 1787335000000, NULL, 1787335240000);

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
