-- One-time reset requested before production use. Keep schema; remove all test OA workflow data.
DELETE FROM oa_outbox;
DELETE FROM oa_process;
