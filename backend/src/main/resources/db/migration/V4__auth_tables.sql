-- Roles table (local mirror of Keycloak realm roles for display and admin UI)
CREATE TABLE roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(50)  NOT NULL UNIQUE,
    display_name    VARCHAR(100) NOT NULL,
    description     TEXT,
    permissions     JSONB        NOT NULL DEFAULT '[]',
    active          BOOLEAN      NOT NULL DEFAULT true,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_roles_name ON roles(name);
CREATE INDEX idx_roles_active ON roles(active);

-- Users table (mirrors Keycloak users; id matches Keycloak user UUID)
CREATE TABLE users (
    id              UUID PRIMARY KEY,
    username        VARCHAR(255) NOT NULL UNIQUE,
    full_name       VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    role            VARCHAR(50)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT true,
    last_login_at   TIMESTAMP WITH TIME ZONE,
    avatar_url      VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_active ON users(active);

-- User-role assignments (local tracking for admin UI and audit)
CREATE TABLE user_roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    role_id         UUID NOT NULL REFERENCES roles(id),
    assigned_by     UUID REFERENCES users(id),
    assigned_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE(user_id, role_id)
);

CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);
