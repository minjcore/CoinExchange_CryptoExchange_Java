INSERT INTO accounting.coa_account (code, name, account_type, active) VALUES
    ('1111', 'Vietinbank — dedicated', 'ASSET', true),
    ('2110', 'Wallet balance — User', 'LIABILITY', true),
    ('2120', 'Wallet balance — Merchant', 'LIABILITY', true),
    ('3100', 'Transit — deposit', 'TRANSIT', true),
    ('3300', 'Transit — internal transfer', 'TRANSIT', true),
    ('3500', 'Transit — payment', 'TRANSIT', true),
    ('4110', 'Fee revenue — deposit', 'REVENUE', true),
    ('4130', 'Fee revenue — transfer', 'REVENUE', true);
