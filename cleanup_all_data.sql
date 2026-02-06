-- ============================================
-- Woorido Test Data Cleanup Script
-- WARNING: This will delete ALL test data!
-- Run this as DEV_LEE user
-- Order: Child tables FIRST, then Parent tables
-- ============================================

-- ========================================
-- WAVE 1: Deepest child tables (leaf nodes)
-- ========================================
-- Vote records
DELETE FROM woorido.expense_vote_records;
DELETE FROM woorido.general_vote_records;
DELETE FROM woorido.meeting_vote_records;

-- Payment logs
DELETE FROM woorido.payment_logs;

-- Ledger entries (references many parent tables)
DELETE FROM woorido.ledger_entries;

-- Post related leaves
DELETE FROM woorido.comment_likes;
DELETE FROM woorido.post_images;
DELETE FROM woorido.post_likes;

-- User related leaves
DELETE FROM woorido.sessions;
DELETE FROM woorido.refresh_tokens;
DELETE FROM woorido.notifications;
DELETE FROM woorido.notification_settings;
DELETE FROM woorido.user_scores;

-- Challenge related leaves
DELETE FROM woorido.challenge_members;

-- Admin related leaves
DELETE FROM woorido.admin_logs;
DELETE FROM woorido.fee_policies;
DELETE FROM woorido.reports;
DELETE FROM woorido.settlements;

-- Refunds (references accounts, account_transactions, admins, users)
DELETE FROM woorido.refunds;

-- ========================================
-- WAVE 2: Mid-level tables
-- ========================================
-- Votes (parent of vote_records)
DELETE FROM woorido.expense_votes;
DELETE FROM woorido.general_votes;
DELETE FROM woorido.meeting_votes;

-- Payment barcodes (references challenges, expense_requests)
DELETE FROM woorido.payment_barcodes;

-- Comments (self-referential: delete children first)
DELETE FROM woorido.comments WHERE parent_id IS NOT NULL;
DELETE FROM woorido.comments;

-- Account transactions
DELETE FROM woorido.account_transactions;

-- ========================================
-- WAVE 3: Higher-level tables
-- ========================================
-- Expense requests (references meetings, users)
DELETE FROM woorido.expense_requests;

-- Meetings (references challenges, users)
DELETE FROM woorido.meetings;

-- Posts (references challenges, users)
DELETE FROM woorido.posts;

-- Accounts (references users)
DELETE FROM woorido.accounts;

-- ========================================
-- WAVE 4: Top-level business tables
-- ========================================
DELETE FROM woorido.challenges;

-- ========================================
-- WAVE 5: Root tables
-- ========================================
DELETE FROM woorido.users;
DELETE FROM woorido.admins;

-- ========================================
-- COMMIT
-- ========================================
COMMIT;

-- ========================================
-- Verification (should all be 0)
-- ========================================
SELECT 'users' AS table_name, COUNT(*) AS cnt FROM woorido.users
UNION ALL SELECT 'admins', COUNT(*) FROM woorido.admins
UNION ALL SELECT 'challenges', COUNT(*) FROM woorido.challenges
UNION ALL SELECT 'accounts', COUNT(*) FROM woorido.accounts
UNION ALL SELECT 'meetings', COUNT(*) FROM woorido.meetings
UNION ALL SELECT 'posts', COUNT(*) FROM woorido.posts
UNION ALL SELECT 'expense_requests', COUNT(*) FROM woorido.expense_requests
UNION ALL SELECT 'challenge_members', COUNT(*) FROM woorido.challenge_members;
