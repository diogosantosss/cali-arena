DROP TABLE IF EXISTS match_events;
DROP TABLE IF EXISTS match_progress;
DROP TABLE IF EXISTS matches;
DROP TABLE IF EXISTS tournament_state;
DROP TABLE IF EXISTS brackets;
DROP TABLE IF EXISTS exercises;
DROP TABLE IF EXISTS endurance_routines;
DROP TABLE IF EXISTS tournaments;
DROP TABLE IF EXISTS athletes;
DROP TABLE IF EXISTS clubs;
DROP TABLE IF EXISTS tokens;
DROP TABLE IF EXISTS users;
DROP TYPE IF EXISTS gender_type;
DROP TYPE IF EXISTS user_role;
DROP TYPE IF EXISTS tournament_status;
DROP TYPE IF EXISTS bracket_stage;
DROP TYPE IF EXISTS match_status;
DROP TYPE IF EXISTS exercise_type;
DROP TYPE IF EXISTS match_event_type;
DROP TYPE IF EXISTS screen_state;

CREATE TYPE user_role AS ENUM (
    'ADMIN',
    'JUDGE'
    );

CREATE TYPE gender_type AS ENUM (
    'MALE',
    'FEMALE'
    );

CREATE TYPE tournament_status AS ENUM (
    'DRAFT',
    'READY',
    'LIVE',
    'FINISHED'
    );


CREATE TYPE bracket_stage AS ENUM (
    'QUALIFIERS',
    'QUARTERFINALS',
    'SEMIFINALS',
    'FINALS'
    );


CREATE TYPE match_status AS ENUM (
    'PENDING',
    'READY',
    'RUNNING',
    'PAUSED',
    'FINISHED'
    );


CREATE TYPE screen_state AS ENUM (
    'WAITING',
    'BRACKET',
    'BATTLE',
    'WINNER',
    'LEADERBOARD'
    );


CREATE TYPE exercise_type AS ENUM (
    'NORMAL',
    'UNBROKEN',
    'SUPERSET'
    );


CREATE TYPE match_event_type AS ENUM (
    'MATCH_STARTED',
    'REP_ADDED',
    'REP_REMOVED',
    'EXERCISE_CHANGED',
    'MATCH_PAUSED',
    'MATCH_FINISHED'
    );

CREATE TABLE users
(
    id         SERIAL PRIMARY KEY,
    username   VARCHAR(64) UNIQUE NOT NULL,
    password   VARCHAR(256)       NOT NULL,
    role       user_role          NOT NULL,

    created_at BIGINT             NOT NULL
);

CREATE TABLE tokens
(
    token_validation VARCHAR(256) PRIMARY KEY,
    user_id          INT REFERENCES Users (id),

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
    gender     gender_type NOT NULL,
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
    status     tournament_status NOT NULL,

    created_at BIGINT            NOT NULL
);

CREATE TABLE brackets
(
    id            SERIAL PRIMARY KEY,
    tournament_id INT REFERENCES tournaments (id),
    gender        gender_type   NOT NULL,
    stage         bracket_stage NOT NULL,
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
    type           exercise_type NOT NULL
);


CREATE TABLE matches
(
    id                SERIAL PRIMARY KEY,
    bracket_id        INT REFERENCES brackets (id),
    routine_id        INT REFERENCES endurance_routines (id),

    athlete_red_id    INT REFERENCES athletes (id),
    athlete_blue_id   INT REFERENCES athletes (id),

    winner_athlete_id INT REFERENCES athletes (id),

    status            match_status NOT NULL,

    started_at        BIGINT,
    finished_at       BIGINT,

    created_at        BIGINT       NOT NULL
);


CREATE TABLE tournament_state
(
    id               SERIAL PRIMARY KEY,
    tournament_id    INT REFERENCES tournaments (id),
    current_screen   screen_state NOT NULL,
    current_match_id INT REFERENCES matches (id),
    updated_at       BIGINT       NOT NULL
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


CREATE TABLE match_events
(
    id         SERIAL PRIMARY KEY,
    match_id   INT REFERENCES matches (id),
    judge_id   INT REFERENCES users (id),

    event_type match_event_type NOT NULL,

    payload    TEXT,

    created_at BIGINT           NOT NULL
);