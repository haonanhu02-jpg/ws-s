-- Each accommodation record owns its displayed resident details.
-- Keep person_id unchanged for identity, billing and history linkage.
ALTER TABLE dorm_stay ADD COLUMN resident_name VARCHAR(100);
ALTER TABLE dorm_stay ADD COLUMN resident_center_name VARCHAR(150);
ALTER TABLE dorm_stay ADD COLUMN resident_department VARCHAR(150);
ALTER TABLE dorm_stay ADD COLUMN resident_gender VARCHAR(10);
ALTER TABLE dorm_stay ADD COLUMN resident_category VARCHAR(100);
ALTER TABLE dorm_stay ADD COLUMN resident_position_name VARCHAR(100);
ALTER TABLE dorm_stay ADD COLUMN resident_rank_name VARCHAR(100);

UPDATE dorm_stay SET
 resident_name = (SELECT name FROM dorm_person WHERE id = dorm_stay.person_id),
 resident_center_name = (SELECT center_name FROM dorm_person WHERE id = dorm_stay.person_id),
 resident_department = (SELECT department FROM dorm_person WHERE id = dorm_stay.person_id),
 resident_gender = (SELECT gender FROM dorm_person WHERE id = dorm_stay.person_id),
 resident_category = (SELECT category FROM dorm_person WHERE id = dorm_stay.person_id),
 resident_position_name = (SELECT position_name FROM dorm_person WHERE id = dorm_stay.person_id),
 resident_rank_name = (SELECT rank_name FROM dorm_person WHERE id = dorm_stay.person_id);
