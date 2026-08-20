CREATE TABLE payments (
    id                    BIGSERIAL PRIMARY KEY,
    client_id             BIGINT         NOT NULL REFERENCES clients (id),
    client_membership_id  BIGINT         REFERENCES client_memberships (id),
    type                  VARCHAR(30)    NOT NULL,
    status                VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    amount                NUMERIC(12, 2) NOT NULL,
    method                VARCHAR(30),
    due_at                TIMESTAMPTZ,
    paid_at               TIMESTAMPTZ,
    notes                 VARCHAR(500),
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    cancelled_at          TIMESTAMPTZ,
    CONSTRAINT payments_amount_non_negative CHECK (amount >= 0)
);

CREATE INDEX idx_payments_client ON payments (client_id);
CREATE INDEX idx_payments_membership ON payments (client_membership_id);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_paid_at ON payments (paid_at);
CREATE INDEX idx_payments_due_at ON payments (due_at)
    WHERE status = 'PENDING';
