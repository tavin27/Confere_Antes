ALTER TABLE alerts DROP COLUMN content;

ALTER TABLE alerts DROP COLUMN risk_level;

ALTER TABLE alerts DROP COLUMN status;

ALTER TABLE alerts ADD COLUMN overall_risk_score INTEGER;