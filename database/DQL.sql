-- 1a. CREATE
INSERT INTO slots (slot_id, type, booked, number_plate)
VALUES (:id, :type, false, NULL);

-- 1b. READ
SELECT slot_id, type, booked, number_plate
FROM   slots
WHERE  slot_id = :slotId;

-- 1c. UPDATE 
UPDATE slots
SET    booked = true,
       number_plate = :numberPlate
WHERE  slot_id = :slotId;

-- 1d. DELETE 
DELETE FROM slots
WHERE  slot_id = :slotId;


-- Search

SELECT slot_id, type, booked, number_plate
FROM   slots
WHERE  (:type IS NULL OR type =  :type)
  AND  (:booked IS NULL OR booked =  :booked)
ORDER BY slot_id
LIMIT  :limit
OFFSET :offset;

-- joined data
SELECT ps.session_id,
       ps.number_plate,
       ps.parked_at,
       s.slot_id,
       s.type        AS slot_type
FROM   parking_sessions ps
JOIN   slots s ON s.slot_id = ps.slot_id
WHERE  ps.active = true
ORDER BY ps.parked_at;


--statistics
SELECT s.type AS slot_type,
       COUNT(DISTINCT s.slot_id) AS slots,
       COUNT(ps.session_id) AS total_sessions,
       COUNT(*) FILTER (WHERE ps.active) AS active_sessions
FROM   slots s
LEFT JOIN parking_sessions ps ON ps.slot_id = s.slot_id
GROUP BY s.type
ORDER BY s.type;

-- top something
SELECT ps.number_plate,
       COUNT(*) AS visits,
       MAX(ps.parked_at) AS last_seen
FROM   parking_sessions ps
GROUP BY ps.number_plate
ORDER BY visits DESC, last_seen DESC
LIMIT 10;
