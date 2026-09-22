-- One-time reset requested before production use.
-- Preserve the real building/room/bed layout and fee-rule configuration.
DELETE FROM dorm_stay_attachment;
DELETE FROM dorm_stay_audit;
DELETE FROM dorm_stay;
DELETE FROM dorm_person;

DELETE FROM dorm_fee_bill;
DELETE FROM dorm_meter_reading;
DELETE FROM dorm_resource_audit;

DELETE FROM bed_change_audit;
DELETE FROM dormitory_record;
DELETE FROM dormitory_processed_event;
DELETE FROM dormitory_bed;

UPDATE dorm_room SET cleaning_required = FALSE;
UPDATE dorm_bed SET cleaning_required = FALSE;
