-- Manual reminder acknowledgement is independent of dates and accommodation state.
-- Existing records remain unconfirmed; do not infer acknowledgement from status.
ALTER TABLE dorm_stay ADD COLUMN arrival_confirmed BOOLEAN NOT NULL DEFAULT FALSE;
