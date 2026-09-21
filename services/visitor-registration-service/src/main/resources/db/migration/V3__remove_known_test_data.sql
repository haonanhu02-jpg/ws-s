-- One-time cleanup for the documented 2026-09-04 public-registration smoke test.
DELETE FROM registration_outbox
WHERE visit_id = 'VISIT-2D8EBF3378AD4632AF85C7C67ED0B684';

DELETE FROM visitor_registration
WHERE visit_id = 'VISIT-2D8EBF3378AD4632AF85C7C67ED0B684';
