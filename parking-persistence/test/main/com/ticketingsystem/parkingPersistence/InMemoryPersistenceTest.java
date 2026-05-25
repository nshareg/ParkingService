package main.com.ticketingsystem.parkingPersistence;

import main.com.ticketingsystem.parkingEntity.ParkingSlot;
import main.com.ticketingsystem.helpers.SlotType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryPersistenceTest {

    private InMemoryPersistence repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryPersistence();
    }

    @Test
    void addAndFindById() {
        ParkingSlot slot = new ParkingSlot(SlotType.REGULAR);
        repo.add(slot);

        Optional<ParkingSlot> found = repo.findById(slot.getSlotID());
        assertTrue(found.isPresent());
        assertSame(slot, found.get());
    }

    @Test
    void addNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> repo.add(null));
    }

    @Test
    void addAlreadyBookedSlotSyncsPlateIndex() {
        ParkingSlot slot = new ParkingSlot(SlotType.REGULAR);
        slot.book("AREG-1");
        repo.add(slot);

        Optional<ParkingSlot> found = repo.findByNumberPlate("AREG-1");
        assertTrue(found.isPresent());
        assertSame(slot, found.get());
    }

    @Test
    void removeExistingSlot() {
        ParkingSlot slot = new ParkingSlot(SlotType.REGULAR);
        repo.add(slot);

        Optional<ParkingSlot> removed = repo.remove(slot.getSlotID());
        assertTrue(removed.isPresent());
        assertEquals(0, repo.count());
    }

    @Test
    void removeNonExistentSlotReturnsEmpty() {
        Optional<ParkingSlot> removed = repo.remove(UUID.randomUUID());
        assertTrue(removed.isEmpty());
    }

    @Test
    void removeBookedSlotSync() {
        ParkingSlot slot = new ParkingSlot(SlotType.REGULAR);
        repo.add(slot);
        slot.book("AREG-1");
        repo.update(slot);

        repo.remove(slot.getSlotID());

        assertTrue(repo.findByNumberPlate("AREG-1").isEmpty());
    }

    @Test
    void updateBookedSlotIndexesPlate() {
        ParkingSlot slot = new ParkingSlot(SlotType.REGULAR);
        repo.add(slot);
        slot.book("AREG-1");
        repo.update(slot);

        Optional<ParkingSlot> found = repo.findByNumberPlate("AREG-1");
        assertTrue(found.isPresent());
        assertSame(slot, found.get());
    }

    @Test
    void findAllReturnsAllSlots() {
        repo.add(new ParkingSlot(SlotType.REGULAR));
        repo.add(new ParkingSlot(SlotType.ELECTRIC));

        assertEquals(2, repo.findAll().size());
    }

    @Test
    void findAllFreeReturnsFreeSlots() {
        ParkingSlot free = new ParkingSlot(SlotType.REGULAR);
        ParkingSlot booked = new ParkingSlot(SlotType.REGULAR);
        repo.add(free);
        repo.add(booked);
        booked.book("AREG-1");
        repo.update(booked);

        List<ParkingSlot> freeSlots = repo.findAllFree();
        assertEquals(1, freeSlots.size());
        assertSame(free, freeSlots.get(0));
    }

    @Test
    void findAllBookedReturnsBookedSlots() {
        ParkingSlot free = new ParkingSlot(SlotType.REGULAR);
        ParkingSlot booked = new ParkingSlot(SlotType.REGULAR);
        repo.add(free);
        repo.add(booked);
        booked.book("AREG-1");
        repo.update(booked);

        List<ParkingSlot> bookedSlots = repo.findAllBooked();
        assertEquals(1, bookedSlots.size());
        assertSame(booked, bookedSlots.get(0));
    }

    @Test
    void findByTypeFiltersCorrectly() {
        repo.add(new ParkingSlot(SlotType.REGULAR));
        repo.add(new ParkingSlot(SlotType.ELECTRIC));
        repo.add(new ParkingSlot(SlotType.REGULAR));

        List<ParkingSlot> regulars = repo.findByType(SlotType.REGULAR);
        assertEquals(2, regulars.size());
        regulars.forEach(s -> assertEquals(SlotType.REGULAR, s.getType()));
    }

    @Test
    void countAfterAddAndRemove() {
        assertEquals(0, repo.count());

        ParkingSlot slot = new ParkingSlot(SlotType.REGULAR);
        repo.add(slot);
        assertEquals(1, repo.count());

        repo.remove(slot.getSlotID());
        assertEquals(0, repo.count());
    }

    @Test
    void countFreeAndCountBooked() {
        ParkingSlot s1 = new ParkingSlot(SlotType.REGULAR);
        ParkingSlot s2 = new ParkingSlot(SlotType.REGULAR);
        repo.add(s1);
        repo.add(s2);

        assertEquals(2, repo.countFree());
        assertEquals(0, repo.countBooked());

        s1.book("AREG-1");
        repo.update(s1);

        assertEquals(1, repo.countFree());
        assertEquals(1, repo.countBooked());
    }
}
