CREATE SCHEMA IF NOT EXISTS accounting;

CREATE TYPE accounting.journal_status AS ENUM ('PENDING', 'POSTED', 'REVERSED');
CREATE TYPE accounting.line_side AS ENUM ('DEBIT', 'CREDIT');

CREATE TABLE accounting.coa_account (
    code            VARCHAR(16) PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    account_type    VARCHAR(32) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE accounting.coa_trans (
    id              BIGSERIAL PRIMARY KEY,
    reference_id    VARCHAR(128) NOT NULL,
    use_case        VARCHAR(32) NOT NULL,
    status          accounting.journal_status NOT NULL DEFAULT 'PENDING',
    description     VARCHAR(512) NULL,
    posting_date    DATE NOT NULL DEFAULT CURRENT_DATE,
    posted_at       TIMESTAMPTZ NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_coa_trans_reference UNIQUE (reference_id, use_case)
);

CREATE TABLE accounting.coa_trans_data (
    id              BIGSERIAL PRIMARY KEY,
    coa_trans_id    BIGINT NOT NULL REFERENCES accounting.coa_trans(id),
    account_code    VARCHAR(16) NOT NULL REFERENCES accounting.coa_account(code),
    side            accounting.line_side NOT NULL,
    amount          NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    currency        CHAR(3) NOT NULL DEFAULT 'VND',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_coa_trans_data_journal ON accounting.coa_trans_data (coa_trans_id);

-- ADR-023: accounting periods. Absence of a row = OPEN. Mark CLOSED/LOCKED to block posting.
CREATE TABLE IF NOT EXISTS accounting.coa_period (
    period_code VARCHAR(7) PRIMARY KEY,   -- yyyy-MM
    status      VARCHAR(16) NOT NULL DEFAULT 'OPEN'
);
