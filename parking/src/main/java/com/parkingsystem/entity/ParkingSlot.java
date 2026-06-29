package com.parkingsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import com.parkingsystem.helpers.SlotType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/*
    Created by anshanyan
    on 23.05.26
*/
@Entity
@Table(name = "slots")
@Getter
public class ParkingSlot {
    @Id
    @Column(name = "slot_id")
    private UUID slotID;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private SlotType type;

    @Column(name = "booked", nullable = false)
    private boolean booked;

    @Column(name = "number_plate")
    private String numberPlate;

    @OneToMany(mappedBy = "slot", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParkingSession> sessions = new ArrayList<>();

    public ParkingSlot(){}

    /**
     * Constructor setting random {@link UUID}
     * @param type {@link SlotType} of the constructed slot, required to be non-null value
     */
    public ParkingSlot(SlotType type) {
        this.slotID = UUID.randomUUID();
        this.type   = Objects.requireNonNull(type);
        this.booked = false;
    }

    /**
     * functionality for booking the object, sets according boolean value to true
     * throws an exception if the slot is already booked
     * @param numberPlate required to be not null, the car number plate of the candidate
     */
    public void book(String numberPlate) {
        if (booked) throw new IllegalStateException("Slot " + slotID + " is already booked");
        this.numberPlate = Objects.requireNonNull(numberPlate, "numberPlate must not be null");
        this.booked = true;
    }

    /**
     * Releasing the spot, sets according boolean to false, and number plate to null,
     * if spot is not occupied, exception is thrown
     */
    public void release() {
        if (!booked) throw new IllegalStateException("Slot " + slotID + " is not booked");
        this.numberPlate = null;
        this.booked = false;
    }

    /**
     * @return formated {@link String} describing the object of this class
     */
    @Override
    public String toString() {
        if (booked) {
            return String.format("ParkingSlot{id=%s, type=%s, BOOKED, plate='%s'}",
                    slotID, type, numberPlate);
        }
        return String.format("ParkingSlot{id=%s, type=%s, FREE}", slotID, type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParkingSlot other)) return false;
        return slotID != null && slotID.equals(other.slotID);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public void addSession(ParkingSession session) {
        if(session == null || this.sessions.contains(session)){
            return;
        }
        sessions.add(session);
        if(session.getSlot() != this){
            session.assignSlot(this);
        }
    }
}
