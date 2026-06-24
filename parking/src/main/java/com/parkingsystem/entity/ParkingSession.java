package com.parkingsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/*
    Created by anshanyan
    on 08.06.26
*/
@Entity
@Table(name = "parking_sessions")
@Getter
public class ParkingSession {

    @Id
    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "slot_id", nullable = false)
    private UUID slotId;

    @Column(name = "number_plate", nullable = false)
    private String numberPlate;

    @Column(name = "parked_at", nullable = false)
    private String parkedAt;

    private boolean active;

    @Column(name = "released_at")
    private String releasedAt;

    protected ParkingSession() {}

    public ParkingSession(UUID slotId, String numberPlate) {
        this.sessionId   = UUID.randomUUID();
        this.slotId      = Objects.requireNonNull(slotId);
        this.numberPlate = Objects.requireNonNull(numberPlate);
        this.parkedAt    = Instant.now().toString();
        this.active      = true;
    }

    public void close() {
        if (!active) throw new IllegalStateException("Session " + sessionId + " is already closed");
        this.releasedAt = Instant.now().toString();
        this.active = false;
    }


    @Override
    public String toString() {
        if (active) {
            return String.format("ParkingSession{id=%s, slot=%s, plate='%s', ACTIVE, since=%s}",
                    sessionId, slotId, numberPlate, parkedAt);
        }
        return String.format("ParkingSession{id=%s, slot=%s, plate='%s', CLOSED, %s -> %s}",
                sessionId, slotId, numberPlate, parkedAt, releasedAt);
    }
}
