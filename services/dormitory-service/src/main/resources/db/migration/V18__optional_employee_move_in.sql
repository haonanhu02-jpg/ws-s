-- Blank planned dates are unknown, not today's date. Preserve all existing dates.
ALTER TABLE dorm_stay ALTER COLUMN planned_move_in DROP NOT NULL;
ALTER TABLE dorm_stay ALTER COLUMN cost_cut DROP NOT NULL;
ALTER TABLE dorm_stay ALTER COLUMN cleaning_required DROP NOT NULL;
