-- Explicit one-time production reset requested on 2026-09-24.
-- Preserve schema and configuration; remove all registration business records.
DELETE FROM registration_outbox;
DELETE FROM visitor_registration;
