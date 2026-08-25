CREATE TABLE exercises (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(120) NOT NULL,
    muscle_group VARCHAR(40)  NOT NULL,
    description  VARCHAR(500),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX exercises_name_uidx ON exercises (lower(name));
CREATE INDEX idx_exercises_muscle_group ON exercises (muscle_group);
CREATE INDEX idx_exercises_active ON exercises (active);

CREATE TABLE training_routines (
    id              BIGSERIAL PRIMARY KEY,
    client_id       BIGINT       NOT NULL REFERENCES clients (id),
    trainer_user_id BIGINT       NOT NULL REFERENCES users (id),
    title           VARCHAR(150) NOT NULL,
    notes           VARCHAR(1000),
    status          VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT training_routines_status_valid CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_training_routines_client ON training_routines (client_id);
CREATE INDEX idx_training_routines_trainer ON training_routines (trainer_user_id);
CREATE INDEX idx_training_routines_status ON training_routines (status);

CREATE TABLE training_routine_items (
    id           BIGSERIAL PRIMARY KEY,
    routine_id   BIGINT      NOT NULL REFERENCES training_routines (id) ON DELETE CASCADE,
    exercise_id  BIGINT      NOT NULL REFERENCES exercises (id),
    sort_order   INTEGER     NOT NULL DEFAULT 0,
    sets         INTEGER,
    reps         VARCHAR(40),
    rest_seconds INTEGER,
    load_note    VARCHAR(80),
    notes        VARCHAR(300),
    CONSTRAINT training_routine_items_sets_positive CHECK (sets IS NULL OR sets > 0),
    CONSTRAINT training_routine_items_rest_non_negative CHECK (rest_seconds IS NULL OR rest_seconds >= 0)
);

CREATE INDEX idx_training_routine_items_routine ON training_routine_items (routine_id);
CREATE INDEX idx_training_routine_items_exercise ON training_routine_items (exercise_id);
