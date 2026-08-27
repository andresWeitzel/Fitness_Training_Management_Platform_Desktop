CREATE TABLE nutrition_appointments (
    id                     BIGSERIAL PRIMARY KEY,
    client_id              BIGINT        NOT NULL REFERENCES clients (id),
    nutritionist_user_id   BIGINT        NOT NULL REFERENCES users (id),
    scheduled_at           TIMESTAMPTZ   NOT NULL,
    status                 VARCHAR(30)   NOT NULL,
    notes                  VARCHAR(2000),
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT nutrition_appointments_status_check CHECK (
        status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')
    )
);

CREATE TABLE nutrition_plans (
    id                   BIGSERIAL PRIMARY KEY,
    client_id            BIGINT        NOT NULL REFERENCES clients (id),
    created_by_user_id   BIGINT        NOT NULL REFERENCES users (id),
    title                VARCHAR(150)  NOT NULL,
    objectives           VARCHAR(1000),
    meal_guidance        VARCHAR(4000),
    status               VARCHAR(30)   NOT NULL,
    valid_from           DATE,
    valid_until          DATE,
    notes                VARCHAR(2000),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT nutrition_plans_status_check CHECK (
        status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')
    )
);

CREATE TABLE health_record_entries (
    id                   BIGSERIAL PRIMARY KEY,
    client_id            BIGINT        NOT NULL REFERENCES clients (id),
    recorded_by_user_id  BIGINT        NOT NULL REFERENCES users (id),
    recorded_at          TIMESTAMPTZ   NOT NULL,
    allergies            VARCHAR(1000),
    restrictions         VARCHAR(1000),
    conditions           VARCHAR(1000),
    medications          VARCHAR(1000),
    notes                VARCHAR(2000),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_nutrition_appointments_client ON nutrition_appointments (client_id);
CREATE INDEX idx_nutrition_appointments_scheduled ON nutrition_appointments (scheduled_at DESC);
CREATE INDEX idx_nutrition_appointments_status ON nutrition_appointments (status);
CREATE INDEX idx_nutrition_plans_client ON nutrition_plans (client_id);
CREATE INDEX idx_nutrition_plans_status ON nutrition_plans (status);
CREATE INDEX idx_health_record_entries_client ON health_record_entries (client_id, recorded_at DESC);
