-- Explicit one-time production reset requested on 2026-09-24.
-- Preserve schema; remove all guard business and delivery records.
DELETE FROM guard_audit_log;
DELETE FROM guard_outbox;
DELETE FROM guard_processed_event;
DELETE FROM guard_record;
