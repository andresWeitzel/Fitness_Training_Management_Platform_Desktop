-- Identity, authorization and client foundation.
-- Flyway owns the schema. Hibernate must not generate DDL.

CREATE TABLE roles (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(50)  NOT NULL UNIQUE,
    description     VARCHAR(255) NOT NULL
);

CREATE TABLE permissions (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(80)  NOT NULL UNIQUE,
    description     VARCHAR(255) NOT NULL
);

CREATE TABLE role_permissions (
    role_id         BIGINT NOT NULL REFERENCES roles (id),
    permission_id   BIGINT NOT NULL REFERENCES permissions (id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(80)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(150) NOT NULL,
    email           VARCHAR(150),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

CREATE TABLE user_roles (
    user_id         BIGINT NOT NULL REFERENCES users (id),
    role_id         BIGINT NOT NULL REFERENCES roles (id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE clients (
    id              BIGSERIAL PRIMARY KEY,
    document_number VARCHAR(20)  NOT NULL UNIQUE,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(150),
    phone           VARCHAR(50),
    address         VARCHAR(200),
    photo_path      VARCHAR(255),
    status          VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_clients_status ON clients (status) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_active ON users (active) WHERE deleted_at IS NULL;

INSERT INTO roles (name, description) VALUES
    ('ADMIN', 'Configuración, personal, clientes, membresías, pagos y reportes'),
    ('RECEPTIONIST', 'Clientes, check-in, pagos y membresías'),
    ('TRAINER', 'Clientes asignados, ejercicios, rutinas, evaluaciones y progreso'),
    ('NUTRITIONIST', 'Clientes, evaluaciones y planes nutricionales');

INSERT INTO permissions (code, description) VALUES
    ('DASHBOARD_VIEW', 'Ver el panel principal'),
    ('CLIENTS_VIEW', 'Consultar clientes'),
    ('CLIENTS_MANAGE', 'Alta, edición y baja de clientes'),
    ('MEMBERSHIPS_MANAGE', 'Planes y membresías'),
    ('PAYMENTS_MANAGE', 'Registrar y consultar pagos'),
    ('CHECKIN_MANAGE', 'Recepción y control de acceso'),
    ('STAFF_MANAGE', 'Personal y roles'),
    ('TRAINING_MANAGE', 'Ejercicios, rutinas y planes de entrenamiento'),
    ('ASSESSMENTS_MANAGE', 'Evaluaciones físicas'),
    ('NUTRITION_MANAGE', 'Turnos y planes nutricionales'),
    ('ANALYTICS_VIEW', 'Indicadores y reportes'),
    ('SETTINGS_MANAGE', 'Configuración de la aplicación');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'DASHBOARD_VIEW',
    'CLIENTS_VIEW',
    'CLIENTS_MANAGE',
    'MEMBERSHIPS_MANAGE',
    'PAYMENTS_MANAGE',
    'CHECKIN_MANAGE'
)
WHERE r.name = 'RECEPTIONIST';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'DASHBOARD_VIEW',
    'CLIENTS_VIEW',
    'TRAINING_MANAGE',
    'ASSESSMENTS_MANAGE'
)
WHERE r.name = 'TRAINER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'DASHBOARD_VIEW',
    'CLIENTS_VIEW',
    'ASSESSMENTS_MANAGE',
    'NUTRITION_MANAGE'
)
WHERE r.name = 'NUTRITIONIST';
