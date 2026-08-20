CREATE TABLE membership_plans (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(100)  NOT NULL,
    description    VARCHAR(500),
    duration_days  INTEGER       NOT NULL,
    price          NUMERIC(12, 2) NOT NULL DEFAULT 0,
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT membership_plans_duration_positive CHECK (duration_days > 0),
    CONSTRAINT membership_plans_price_non_negative CHECK (price >= 0)
);

CREATE UNIQUE INDEX membership_plans_name_uidx ON membership_plans (lower(name));

CREATE TABLE client_memberships (
    id           BIGSERIAL PRIMARY KEY,
    client_id    BIGINT       NOT NULL REFERENCES clients (id),
    plan_id      BIGINT       NOT NULL REFERENCES membership_plans (id),
    starts_at    TIMESTAMPTZ  NOT NULL,
    ends_at      TIMESTAMPTZ  NOT NULL,
    status       VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    cancelled_at TIMESTAMPTZ,
    CONSTRAINT client_memberships_dates_valid CHECK (ends_at > starts_at)
);

CREATE INDEX idx_client_memberships_client ON client_memberships (client_id);
CREATE INDEX idx_client_memberships_plan ON client_memberships (plan_id);
CREATE INDEX idx_client_memberships_ends_at ON client_memberships (ends_at);

CREATE UNIQUE INDEX client_memberships_one_active_uidx
    ON client_memberships (client_id)
    WHERE status = 'ACTIVE';

INSERT INTO membership_plans (name, description, duration_days, price)
VALUES
    ('Mensual', 'Acceso por 30 días', 30, 25000.00),
    ('Trimestral', 'Acceso por 90 días', 90, 65000.00),
    ('Anual', 'Acceso por 365 días', 365, 220000.00);
