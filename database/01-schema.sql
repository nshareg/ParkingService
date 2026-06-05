-- Schema for the parking system, auto-run by Postgres on first container start
-- (mounted into /docker-entrypoint-initdb.d/ via compose.yaml).
-- Mirrors the columns used by ParkingRepositoryimpl: slot_id, type, booked, number_plate.

CREATE TABLE slots (
    slot_id      uuid PRIMARY KEY,
    type         varchar(50) NOT NULL,
    booked       boolean     NOT NULL,
    number_plate varchar(50)
);

CREATE INDEX idx_slots_number_plate ON slots (number_plate);
