-- Credentials de acceso y DNI único solo entre clientes no dados de baja.

ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_document_number_key;

CREATE UNIQUE INDEX IF NOT EXISTS clients_document_number_active_uidx
    ON clients (document_number)
    WHERE deleted_at IS NULL;

CREATE SEQUENCE IF NOT EXISTS client_number_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS card_number_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS qr_code_seq START WITH 1;

CREATE TABLE access_credentials (
    id          BIGSERIAL PRIMARY KEY,
    client_id   BIGINT       NOT NULL REFERENCES clients (id),
    type        VARCHAR(30)  NOT NULL,
    code        VARCHAR(80)  NOT NULL,
    issued_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMPTZ,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT access_credentials_type_code_key UNIQUE (type, code)
);

CREATE INDEX idx_access_credentials_client ON access_credentials (client_id);
CREATE INDEX idx_access_credentials_code ON access_credentials (code);
