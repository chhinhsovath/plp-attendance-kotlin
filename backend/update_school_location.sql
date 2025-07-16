-- Update school coordinates to real location
-- New coordinates: 11.551481374849613, 104.92816726562374

UPDATE schools 
SET 
    latitude = 11.551481374849613,
    longitude = 104.92816726562374,
    updated_at = CURRENT_TIMESTAMP
WHERE name = 'Royal University of Phnom Penh';

-- Verify the update
SELECT id, name, latitude, longitude 
FROM schools 
WHERE name = 'Royal University of Phnom Penh';