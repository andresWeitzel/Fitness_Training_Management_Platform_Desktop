CREATE TABLE check_ins (
    id               BIGSERIAL PRIMARY KEY,
    client_id        BIGINT       NOT NULL REFERENCES clients (id),
    credential_id    BIGINT       REFERENCES access_credentials (id),
    credential_type  VARCHAR(30),
    access_mode      VARCHAR(30)  NOT NULL,
    checked_in_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    notes            VARCHAR(500)
);

CREATE INDEX idx_check_ins_client ON check_ins (client_id);
CREATE INDEX idx_check_ins_checked_in_at ON check_ins (checked_in_at);
CREATE INDEX idx_check_ins_day_client ON check_ins (client_id, checked_in_at);
