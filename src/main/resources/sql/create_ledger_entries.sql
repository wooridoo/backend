CREATE TABLE woorido.ledger_entries (
    id VARCHAR2(36) PRIMARY KEY,
    challenge_id VARCHAR2(36) NOT NULL,
    type VARCHAR2(20) NOT NULL, -- SUPPORT, ENTRY_FEE, EXPENSE, REFUND
    amount NUMBER(18) NOT NULL,
    balance_before NUMBER(18),
    balance_after NUMBER(18),
    description VARCHAR2(255),
    related_user_id VARCHAR2(36),
    related_meeting_id VARCHAR2(36),
    related_expense_request_id VARCHAR2(36),
    related_barcode_id VARCHAR2(36),
    merchant_name VARCHAR2(100),
    merchant_category VARCHAR2(50),
    pg_provider VARCHAR2(50),
    pg_approval_number VARCHAR2(50),
    memo VARCHAR2(1000),
    memo_updated_at TIMESTAMP,
    memo_updated_by VARCHAR2(36),
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP
);

CREATE INDEX idx_ledger_challenge_id ON woorido.ledger_entries(challenge_id);
CREATE INDEX idx_ledger_created_at ON woorido.ledger_entries(created_at);
