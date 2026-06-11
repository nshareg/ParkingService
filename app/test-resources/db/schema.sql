CREATE TABLE IF NOT EXISTS slots (
    slot_id      uuid        PRIMARY KEY,
    type         varchar(50) NOT NULL,
    booked       boolean     NOT NULL,
    number_plate varchar(50)
);

CREATE INDEX IF NOT EXISTS idx_slots_number_plate ON slots (number_plate);

CREATE TABLE IF NOT EXISTS parking_sessions (
    session_id   uuid        PRIMARY KEY,
    slot_id      uuid        NOT NULL,
    number_plate varchar(50) NOT NULL,
    active       boolean     NOT NULL,
    parked_at    varchar(64) NOT NULL,
    released_at  varchar(64),

    CONSTRAINT fk_parking_sessions_slot
        FOREIGN KEY (slot_id) REFERENCES slots (slot_id)
);

CREATE INDEX IF NOT EXISTS idx_parking_sessions_slot_id
    ON parking_sessions (slot_id);
CREATE INDEX IF NOT EXISTS idx_parking_sessions_number_plate
    ON parking_sessions (number_plate);
