-- ADR-007: FAILED status for voided withdrawals (release path)
ALTER TABLE accounting.coa_trans DROP CONSTRAINT IF EXISTS coa_trans_status_check;
ALTER TABLE accounting.coa_trans ADD CONSTRAINT coa_trans_status_check
    CHECK (status IN ('PENDING', 'POSTED', 'REVERSED', 'FAILED'));
