package main.com.parkingsystem.entity;

import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/*
    Created by anshanyan
    on 08.06.26
*/

@Getter
public class ParkingSession {
    private final UUID sessionId;
    private final UUID slotId;
    private final String numberPlate;
    private final String parkedAt;

    private boolean active;
    private String releasedAt;

    public ParkingSession(UUID slotId, String numberPlate) {
        this.sessionId   = UUID.randomUUID();
        this.slotId      = Objects.requireNonNull(slotId, "slotId must not be null");
        this.numberPlate = Objects.requireNonNull(numberPlate, "numberPlate must not be null");
        this.parkedAt    = Instant.now().toString();
        this.active      = true;
    }


    public ParkingSession(UUID sessionId, UUID slotId, String numberPlate,
                          boolean active, String parkedAt, String releasedAt) {
        this.sessionId   = Objects.requireNonNull(sessionId, "sessionId must not be null");
        this.slotId      = Objects.requireNonNull(slotId, "slotId must not be null");
        this.numberPlate = Objects.requireNonNull(numberPlate, "numberPlate must not be null");
        this.active      = active;
        this.parkedAt    = parkedAt;
        this.releasedAt  = releasedAt;
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
