-- Enrichment of training catalog and routine lifecycle states.

ALTER TABLE exercises
    ADD COLUMN IF NOT EXISTS equipment VARCHAR(40) NOT NULL DEFAULT 'OTHER',
    ADD COLUMN IF NOT EXISTS difficulty VARCHAR(40) NOT NULL DEFAULT 'INTERMEDIATE',
    ADD COLUMN IF NOT EXISTS secondary_muscles VARCHAR(200),
    ADD COLUMN IF NOT EXISTS technique_notes VARCHAR(1000);

ALTER TABLE training_routines
    ADD COLUMN IF NOT EXISTS focus VARCHAR(80),
    ADD COLUMN IF NOT EXISTS starts_on DATE;

ALTER TABLE training_routines
    DROP CONSTRAINT IF EXISTS training_routines_status_valid;

ALTER TABLE training_routines
    ADD CONSTRAINT training_routines_status_valid
        CHECK (status IN ('ACTIVE', 'DRAFT', 'SCHEDULED', 'ARCHIVED'));

CREATE INDEX IF NOT EXISTS idx_exercises_equipment ON exercises (equipment);
CREATE INDEX IF NOT EXISTS idx_exercises_difficulty ON exercises (difficulty);
CREATE INDEX IF NOT EXISTS idx_training_routines_starts_on ON training_routines (starts_on);
