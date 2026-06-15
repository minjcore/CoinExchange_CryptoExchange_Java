-- ADR-040: USER multi-pocket wallets — migration 1 of 2 (catalog + seed).
-- Apply BEFORE V_pocket_2 (the wallet.pocket_code FK references this table).
-- Design/spec repo: this is the rollout path for a system already running single-wallet.
-- For a fresh install, the target shape is inlined in spec/implementation.md §3.

CREATE TABLE IF NOT EXISTS wallet.wallet_pocket_def (
    code            VARCHAR(32) PRIMARY KEY,
    name            VARCHAR(64) NOT NULL,
    description     VARCHAR(255) NULL,
    wallet_type     wallet.wallet_type NOT NULL DEFAULT 'USER',
    is_default      BOOLEAN NOT NULL DEFAULT false,
    multi_allowed   BOOLEAN NOT NULL DEFAULT false,
    active          BOOLEAN NOT NULL DEFAULT true,
    sort_order      INT NOT NULL DEFAULT 0
);

-- Exactly one is_default = true (the auto-provisioned pocket).
INSERT INTO wallet.wallet_pocket_def (code, name, wallet_type, is_default, multi_allowed, sort_order) VALUES
    ('DEFAULT',  'Ví chính',   'USER', true,  false, 0),
    ('SPENDING', 'Chi tiêu',   'USER', false, false, 1),
    ('SAVINGS',  'Tiết kiệm',  'USER', false, false, 2),
    ('GOAL',     'Mục tiêu',   'USER', false, true,  3)
ON CONFLICT (code) DO NOTHING;

-- Rollback: DROP TABLE wallet.wallet_pocket_def;  (safe: only referenced after V_pocket_2)
