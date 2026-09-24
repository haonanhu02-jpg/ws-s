-- Explicit one-time production reset requested on 2026-09-24.
-- Preserve schema and adapter configuration; remove all OA workflow records.
DELETE FROM oa_outbox;
DELETE FROM oa_process;
