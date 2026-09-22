-- One-time reset requested before production use. Keep schema; remove all test registrations.
DELETE FROM registration_outbox;
DELETE FROM visitor_registration;
