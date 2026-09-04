ALTER TABLE guard_record ADD COLUMN details_version BIGINT NOT NULL DEFAULT 0;
UPDATE guard_record SET details_version=1 WHERE EXISTS (SELECT 1 FROM guard_audit_log a WHERE a.visit_id=guard_record.visit_id AND a.action='DETAILS_UPDATED');
ALTER TABLE guard_record ADD COLUMN accommodation_sync_pending BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE guard_record SET accommodation_sync_pending=TRUE;
