-- ADR-040: USER multi-pocket wallets — migration 2 of 2 (wallet columns + indexes).
-- Apply AFTER V_pocket_1. Backward compatible: existing rows backfill via DEFAULTs;
-- balances and wallet_tx are untouched. Reversible.

-- 1. Add pocket columns. Existing wallet rows become the 'default' pocket automatically.
ALTER TABLE wallet.wallet
    ADD COLUMN IF NOT EXISTS pocket_code VARCHAR(32) NOT NULL DEFAULT 'DEFAULT'
        REFERENCES wallet.wallet_pocket_def(code);
ALTER TABLE wallet.wallet
    ADD COLUMN IF NOT EXISTS label VARCHAR(64) NOT NULL DEFAULT 'default';

-- 2. Replace the old uniqueness (member,type,ccy) with label-discriminated uniqueness.
--    Existing members have a single label='default' row, so this never conflicts.
ALTER TABLE wallet.wallet DROP CONSTRAINT IF EXISTS uq_wallet_member_type_ccy;
ALTER TABLE wallet.wallet
    ADD CONSTRAINT uq_wallet_member_type_ccy_label
    UNIQUE (member_id, wallet_type, currency, label);

-- 3. Keep MERCHANT/PARTNER single-wallet (USER may hold many).
CREATE UNIQUE INDEX IF NOT EXISTS uq_wallet_single_nonuser
    ON wallet.wallet (member_id, wallet_type, currency)
    WHERE wallet_type <> 'USER';

-- 4. As-of balance index with deterministic tie-break (ADR-004 point 7).
DROP INDEX IF EXISTS wallet.idx_wallet_tx_wallet_created;
CREATE INDEX idx_wallet_tx_wallet_created
    ON wallet.wallet_tx (wallet_id, created_at DESC, id DESC);

-- Rollback:
--   DROP INDEX wallet.uq_wallet_single_nonuser;
--   ALTER TABLE wallet.wallet DROP CONSTRAINT uq_wallet_member_type_ccy_label;
--   ALTER TABLE wallet.wallet ADD CONSTRAINT uq_wallet_member_type_ccy UNIQUE (member_id, wallet_type, currency);
--   ALTER TABLE wallet.wallet DROP COLUMN label; DROP COLUMN pocket_code;
--   (recreate idx_wallet_tx_wallet_created (wallet_id, created_at DESC) if desired)
