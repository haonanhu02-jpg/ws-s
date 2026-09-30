ALTER TABLE dorm_meter_reading ADD COLUMN water_start DECIMAL(14,2);
ALTER TABLE dorm_meter_reading ADD COLUMN electric_start DECIMAL(14,2);
ALTER TABLE dorm_meter_reading ADD COLUMN reading_date DATE;

UPDATE dorm_meter_reading
SET reading_date = CAST(updated_at AS DATE)
WHERE reading_date IS NULL;
