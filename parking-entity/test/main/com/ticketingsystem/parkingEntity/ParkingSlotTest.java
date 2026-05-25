package main.com.ticketingsystem.parkingEntity;

import main.com.ticketingsystem.helpers.SlotType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ParkingSlotTest {

    @Test
    void newSlotIsFreeByDefault() {
        ParkingSlot slot = new ParkingSlot(SlotType.REGULAR);

        assertFalse(slot.isBooked());

        assertNull(slot.getNumberPlate());
    }

    @Test
    void constructorRejectsNullType() {
        assertThrows(NullPointerException.class, () -> new ParkingSlot(null));
    }

    @Test
    void bookSetsFieldsCorrectly() {
        ParkingSlot slot = new ParkingSlot(SlotType.ELECTRIC);

        slot.book("AREG-1");

        assertTrue(slot.isBooked());
        assertEquals("AREG-1", slot.getNumberPlate());
    }

    @Test
    void bookRejectsNullNumberPlate() {
        ParkingSlot slot = new ParkingSlot(SlotType.REGULAR);
        assertThrows(NullPointerException.class, () -> slot.book(null));
    }

    @Test
    void bookAlreadyBookedSlotThrows() {
        ParkingSlot slot = new ParkingSlot(SlotType.REGULAR);
        slot.book("AREG-1");

        assertThrows(IllegalStateException.class, () -> slot.book("XYZ-999"));
    }

    @Test
    void releaseClearsBooking() {
        ParkingSlot slot = new ParkingSlot(SlotType.DISABLED);
        slot.book("AREG-1");

        slot.release();

        assertFalse(slot.isBooked());

        assertNull(slot.getNumberPlate());
    }

    @Test
    void releaseFreeSlotThrows() {
        ParkingSlot slot = new ParkingSlot(SlotType.REGULAR);
        assertThrows(IllegalStateException.class, slot::release);
    }

    @Test
    void toStringShowsFreeSlot() {
        ParkingSlot slot = new ParkingSlot(SlotType.REGULAR);

        String str = slot.toString();
        assertTrue(str.contains("FREE"));
        assertTrue(str.contains(slot.getSlotID().toString()));
    }

    @Test
    void toStringShowsBookedSlot() {
        ParkingSlot slot = new ParkingSlot(SlotType.ELECTRIC);
        slot.book("AREG-1");

        String str = slot.toString();
        assertTrue(str.contains("BOOKED"));
        assertTrue(str.contains("AREG-1"));
    }

    @Test
    void gettersReturnConstructorValues() {
        ParkingSlot slot = new ParkingSlot(SlotType.DISABLED);

        assertNotNull(slot.getSlotID());
        assertEquals(SlotType.DISABLED, slot.getType());
    }
}
