-- uq_coa_trans_reference already creates an index on (reference_id, use_case)
DROP INDEX IF EXISTS accounting.idx_coa_trans_ref_usecase;

-- idx_coa_trans_data_journal covers coa_trans_id; drop the Hibernate-generated duplicate
DROP INDEX IF EXISTS accounting.idx_coa_trans_data_trans_id;
