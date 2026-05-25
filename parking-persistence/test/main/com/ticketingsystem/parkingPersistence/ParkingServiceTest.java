package main.com.ticketingsystem.parkingPersistence;

import main.com.ticketingsystem.parkingEntity.ParkingService;
import main.com.ticketingsystem.parkingEntity.ParkingSlot;
import main.com.ticketingsystem.helpers.SlotType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ParkingServiceTest {

    private ParkingService service;

    @BeforeEach
    void setUp() {
        service = new ParkingService(new InMemoryPersistence());
    }

    @Test
    void addSlotCreatesNewFreeSlot() {
        ParkingSlot slot = service.addSlot(SlotType.REGULAR);

        assertNotNull(slot.getSlotID());
        assertEquals(SlotType.REGULAR, slot.getType());
        assertFalse(slot.isBooked());
    }

    @Test
    void parkBooksFreeSlot() {
        service.addSlot(SlotType.REGULAR);

        Optional<ParkingSlot> parked = service.park("AREG-1");

        assertTrue(parked.isPresent());
        assertTrue(parked.get().isBooked());
        assertEquals("AREG-1", parked.get().getNumberPlate());
    }

    @Test
    void parkReturnsEmptyWhenNoFreeSlots() {
        service.addSlot(SlotType.REGULAR);
        service.park("AREG-1");

        Optional<ParkingSlot> second = service.park("AREG-2");

        assertTrue(second.isEmpty());
    }

    @Test
    void releaseFreesCorrectSlot() {
        service.addSlot(SlotType.REGULAR);
        service.addSlot(SlotType.REGULAR);
        service.park("AREG-1");
        service.park("AREG-2");

        Optional<ParkingSlot> released = service.release("AREG-1");

        assertTrue(released.isPresent());
        assertFalse(released.get().isBooked());
        assertEquals(1, service.countFree());
        assertEquals(1, service.countBooked());
    }

    @Test
    void releaseUnknownPlateReturnsEmpty() {
        assertTrue(service.release("NOT-AREG").isEmpty());
    }

    @Test
    void findByNumberPlateFindsBookedCar() {
        service.addSlot(SlotType.REGULAR);
        service.park("AREG-1");

        Optional<ParkingSlot> found = service.findByNumberPlate("AREG-1");
        assertTrue(found.isPresent());
        assertEquals("AREG-1", found.get().getNumberPlate());
    }

    @Test
    void parkReleaseParkCycle() {
        service.addSlot(SlotType.REGULAR);

        service.park("AREG-1");
        assertEquals(0, service.countFree());

        service.release("AREG-1");
        assertEquals(1, service.countFree());

        Optional<ParkingSlot> second = service.park("AREG-2");
        assertTrue(second.isPresent());
        assertEquals("AREG-2", second.get().getNumberPlate());
    }
}
