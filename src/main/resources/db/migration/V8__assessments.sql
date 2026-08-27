CREATE TABLE physical_assessments (
    id                   BIGSERIAL PRIMARY KEY,
    client_id            BIGINT        NOT NULL REFERENCES clients (id),
    assessed_by_user_id  BIGINT        NOT NULL REFERENCES users (id),
    assessed_at          TIMESTAMPTZ   NOT NULL,
    weight_kg            NUMERIC(5, 2),
    height_cm            NUMERIC(5, 2),
    body_fat_pct         NUMERIC(4, 1),
    waist_cm             NUMERIC(5, 1),
    hip_cm               NUMERIC(5, 1),
    chest_cm             NUMERIC(5, 1),
    notes                VARCHAR(2000),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT physical_assessments_weight_positive CHECK (weight_kg IS NULL OR weight_kg > 0),
    CONSTRAINT physical_assessments_height_positive CHECK (height_cm IS NULL OR height_cm > 0),
    CONSTRAINT physical_assessments_body_fat_range CHECK (body_fat_pct IS NULL OR (body_fat_pct >= 0 AND body_fat_pct <= 100)),
    CONSTRAINT physical_assessments_waist_non_negative CHECK (waist_cm IS NULL OR waist_cm >= 0),
    CONSTRAINT physical_assessments_hip_non_negative CHECK (hip_cm IS NULL OR hip_cm >= 0),
    CONSTRAINT physical_assessments_chest_non_negative CHECK (chest_cm IS NULL OR chest_cm >= 0)
);

CREATE INDEX idx_physical_assessments_client ON physical_assessments (client_id);
CREATE INDEX idx_physical_assessments_assessed_at ON physical_assessments (assessed_at DESC);
CREATE INDEX idx_physical_assessments_assessor ON physical_assessments (assessed_by_user_id);
