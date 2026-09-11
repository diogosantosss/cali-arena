DROP TABLE IF EXISTS screen_routines;
DROP TABLE IF EXISTS match_progress;
DROP TABLE IF EXISTS tournament_state;
DROP TABLE IF EXISTS matches;
DROP TABLE IF EXISTS brackets;
DROP TABLE IF EXISTS exercises;
DROP TABLE IF EXISTS endurance_routines;
DROP TABLE IF EXISTS tournaments;
DROP TABLE IF EXISTS athletes;
DROP TABLE IF EXISTS clubs;
DROP TABLE IF EXISTS tokens;
DROP TABLE IF EXISTS users;

CREATE TABLE users
(
    id         SERIAL PRIMARY KEY,
    username   VARCHAR(64) UNIQUE NOT NULL,
    password   VARCHAR(256)       NOT NULL,
    role       VARCHAR(20)          NOT NULL,

    created_at BIGINT             NOT NULL
);

CREATE TABLE tokens
(
    token_validation VARCHAR(256) PRIMARY KEY,
    user_id          INT REFERENCES users (id),

    created_at       BIGINT NOT NULL,
    last_used_at     BIGINT NOT NULL
);

CREATE TABLE clubs
(
    id         SERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    short_name VARCHAR(20),

    created_at BIGINT       NOT NULL
);

CREATE TABLE athletes
(
    id         SERIAL PRIMARY KEY,
    name       VARCHAR(100),
    gender     VARCHAR(20) NOT NULL,
    club_id    INT REFERENCES clubs (id),

    created_at BIGINT      NOT NULL
);

CREATE TABLE tournaments
(
    id         SERIAL PRIMARY KEY,
    name       VARCHAR(100)      NOT NULL,
    location   VARCHAR(100),

    start_date BIGINT,
    end_date   BIGINT,
    status     VARCHAR(20) NOT NULL,

    created_at BIGINT            NOT NULL
);

CREATE TABLE brackets
(
    id            SERIAL PRIMARY KEY,
    tournament_id INT REFERENCES tournaments (id),
    division      VARCHAR(40) NOT NULL,
    stage         VARCHAR(20) NOT NULL,
    created_at    BIGINT        NOT NULL
);


CREATE TABLE endurance_routines
(
    id               SERIAL PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    time_cap_seconds INT,
    created_at       BIGINT       NOT NULL
);


CREATE TABLE exercises
(
    id             SERIAL PRIMARY KEY,
    routine_id     INT REFERENCES endurance_routines (id),
    name           VARCHAR(100)  NOT NULL,
    target_reps    INT           NOT NULL,
    added_weight   DECIMAL(6, 2),
    exercise_order INT           NOT NULL,
    superset_order INT,
    type           VARCHAR(20) NOT NULL
);


CREATE TABLE matches
(
    id                  SERIAL PRIMARY KEY,
    bracket_id          INT REFERENCES brackets (id),
    routine_id          INT REFERENCES endurance_routines (id),
    judge_id            INT REFERENCES users (id),

    athlete_red_id      INT REFERENCES athletes (id),
    athlete_blue_id     INT REFERENCES athletes (id),

    winner_athlete_id   INT REFERENCES athletes (id),

    status              VARCHAR(20) NOT NULL,

    started_at          BIGINT,
    finished_at         BIGINT,

    created_at          BIGINT       NOT NULL
);


CREATE TABLE tournament_state
(
    id                 SERIAL PRIMARY KEY,
    tournament_id      INT REFERENCES tournaments (id),
    current_screen     VARCHAR(20) NOT NULL,
    current_match_id   INT REFERENCES matches (id),
    current_bracket_id INT REFERENCES brackets (id),
    current_division  VARCHAR(40),
    updated_at         BIGINT       NOT NULL
);


CREATE TABLE match_progress
(
    id                       SERIAL PRIMARY KEY,
    match_id                 INT UNIQUE REFERENCES matches (id),

    red_current_exercise_id  INT REFERENCES exercises (id),
    blue_current_exercise_id INT REFERENCES exercises (id),

    red_current_reps         INT    NOT NULL,
    blue_current_reps        INT    NOT NULL,

    red_finished_at          BIGINT,
    blue_finished_at         BIGINT,

    timer_started_at         BIGINT,

    timer_remaining_seconds  INT,

    updated_at               BIGINT NOT NULL
);

CREATE TABLE screen_routines
(
    id            SERIAL PRIMARY KEY,
    tournament_id INT REFERENCES tournaments (id) ON DELETE CASCADE,
    routine_id    INT REFERENCES endurance_routines (id) NOT NULL,
    display_order INT NOT NULL,
    is_visible    BOOLEAN NOT NULL DEFAULT TRUE,
    label         VARCHAR(100),
    created_at    BIGINT NOT NULL,
    updated_at    BIGINT NOT NULL
);