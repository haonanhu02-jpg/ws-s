-- One-time cleanup for records created by the documented 2026-09-04 smoke test
-- and the explicitly confirmed employee test records shown in the acceptance UI.
DELETE FROM dorm_stay_attachment
WHERE stay_id IN (
  SELECT s.id FROM dorm_stay s JOIN dorm_person p ON p.id = s.person_id
  WHERE p.name IN ('高管', '全权')
);

DELETE FROM dorm_stay_audit
WHERE stay_id IN (
  SELECT s.id FROM dorm_stay s JOIN dorm_person p ON p.id = s.person_id
  WHERE p.name IN ('高管', '全权')
);

DELETE FROM dorm_stay
WHERE person_id IN (SELECT id FROM dorm_person WHERE name IN ('高管', '全权'));

DELETE FROM dorm_person WHERE name IN ('高管', '全权');

DELETE FROM bed_change_audit
WHERE visit_id IN (
  'GUARD-05BFD9ABCCA7468889C02B14D0F5317E',
  'VISIT-2D8EBF3378AD4632AF85C7C67ED0B684'
);

DELETE FROM dormitory_record
WHERE visit_id IN (
  'GUARD-05BFD9ABCCA7468889C02B14D0F5317E',
  'VISIT-2D8EBF3378AD4632AF85C7C67ED0B684'
);

DELETE FROM dormitory_bed
WHERE bed_code LIKE 'SMOKE-%'
  AND id NOT IN (SELECT bed_id FROM dormitory_record WHERE bed_id IS NOT NULL);
