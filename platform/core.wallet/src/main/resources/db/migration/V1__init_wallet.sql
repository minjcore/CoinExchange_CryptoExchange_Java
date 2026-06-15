CREATE SCHEMA IF NOT EXISTS wallet;

CREATE TYPE wallet.wallet_status AS ENUM ('ACTIVE', 'LOCKED', 'CLOSED');
CREATE TYPE wallet.wallet_type AS ENUM ('USER', 'MERCHANT', 'PARTNER');
CREATE TYPE wallet.tx_direction AS ENUM ('CREDIT', 'DEBIT', 'FREEZE', 'UNFREEZE');

CREATE TABLE wallet.wallet (
    id              BIGSERIAL PRIMARY KEY,
    member_id       BIGINT NOT NULL,
    wallet_type     wallet.wallet_type NOT NULL,
    currency        CHAR(3) NOT NULL DEFAULT 'VND',
    status          wallet.wallet_status NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_wallet_member_type_ccy UNIQUE (member_id, wallet_type, currency)
);
CREATE INDEX idx_wallet_member ON wallet.wallet (member_id);

CREATE TABLE wallet.wallet_balance (
    wallet_id       BIGINT PRIMARY KEY REFERENCES wallet.wallet(id),
    available       NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (available >= 0),
    frozen          NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (frozen >= 0),
    version         BIGINT NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE wallet.wallet_tx (
    id              BIGSERIAL PRIMARY KEY,
    wallet_id       BIGINT NOT NULL REFERENCES wallet.wallet(id),
    tx_type         VARCHAR(32) NOT NULL,
    direction       wallet.tx_direction NOT NULL,
    amount          NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    available_after NUMERIC(19,4) NOT NULL,
    frozen_after    NUMERIC(19,4) NOT NULL,
    business_ref    VARCHAR(128) NOT NULL,
    coa_trans_id    BIGINT NULL,
    use_case        VARCHAR(32) NULL,
    remark          VARCHAR(512) NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_wallet_tx_idempotency UNIQUE (wallet_id, business_ref, tx_type)
);
CREATE INDEX idx_wallet_tx_wallet_created ON wallet.wallet_tx (wallet_id, created_at DESC);
CREATE INDEX idx_wallet_tx_business_ref ON wallet.wallet_tx (business_ref);
