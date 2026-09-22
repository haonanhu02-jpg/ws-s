-- One-time reset requested before production use. Keep schema; remove all test guard data.
DELETE FROM guard_audit_log;
DELETE FROM guard_outbox;
DELETE FROM guard_processed_event;
DELETE FROM guard_record;
