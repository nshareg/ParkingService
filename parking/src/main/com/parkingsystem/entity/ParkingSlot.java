package main.com.parkingsystem.entity;

import lombok.Getter;
import main.com.parkingsystem.helpers.SlotType;

import java.util.Objects;
import java.util.UUID;

/*
    Created by anshanyan
    on 23.05.26
*/

@Getter
public class ParkingSlot {
    //variable names are self-descriptive
    private final UUID slotID;
    private final SlotType type;

    private boolean booked;
    private String numberPlate;

    /**
     * Constructor setting random {@link UUID}
     * @param type {@link SlotType} of the constructed slot, required to be non-null value
     */
    public ParkingSlot(SlotType type) {
        this.slotID = UUID.randomUUID();
        this.type   = Objects.requireNonNull(type, "type must not be null");
        this.booked = false;
    }

    public ParkingSlot(UUID id, SlotType type, boolean booked, String numberPlate) {
        this.slotID = id;
        this.type   = Objects.requireNonNull(type, "type must not be null");
        this.booked = booked;
        this.numberPlate = numberPlate;
    }

    /**
     * Reconstruction constructor — used when rehydrating a slot from persistent storage.
     * @param slotID existing {@link UUID} from the data store
     * @param type   {@link SlotType} of the slot
     */
    public ParkingSlot(UUID slotID, SlotType type) {
        this.slotID = Objects.requireNonNull(slotID, "slotID must not be null");
        this.type   = Objects.requireNonNull(type, "type must not be null");
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
}
