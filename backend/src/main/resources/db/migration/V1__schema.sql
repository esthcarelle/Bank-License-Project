-- Bank Licensing Portal Schama
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    role            VARCHAR(32) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE applications (
    id                    BIGSERIAL PRIMARY KEY,
    applicant_id          BIGINT NOT NULL REFERENCES users (id),
    institution_name      VARCHAR(500) NOT NULL,
    state                 VARCHAR(64) NOT NULL,
    version               BIGINT NOT NULL DEFAULT 0,
    reviewed_by_user_id   BIGINT REFERENCES users (id),
    last_rejection_reason TEXT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_applications_applicant ON applications (applicant_id);
CREATE INDEX idx_applications_state ON applications (state);

CREATE TABLE application_documents (
    id                   BIGSERIAL PRIMARY KEY,
    application_id       BIGINT NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    revision             INT NOT NULL,
    original_filename    VARCHAR(500) NOT NULL,
    size_bytes           BIGINT NOT NULL,
    content_type         VARCHAR(200) NOT NULL,
    storage_path         VARCHAR(1000) NOT NULL,
    uploaded_by_user_id  BIGINT NOT NULL REFERENCES users (id),
    uploaded_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (application_id, revision, original_filename)
);

CREATE INDEX idx_docs_app ON application_documents (application_id);

CREATE TABLE audit_entries (
    id               BIGSERIAL PRIMARY KEY,
    application_id   BIGINT NOT NULL REFERENCES applications (id),
    actor_user_id    BIGINT NOT NULL REFERENCES users (id),
    action           VARCHAR(128) NOT NULL,
    state_before     VARCHAR(64) NOT NULL,
    state_after      VARCHAR(64) NOT NULL,
    details_json     TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_application ON audit_entries (application_id);
CREATE INDEX idx_audit_created ON audit_entries (created_at);
