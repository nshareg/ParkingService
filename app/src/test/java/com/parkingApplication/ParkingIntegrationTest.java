package com.parkingApplication;

import com.parkingsystem.contract.ParkingService;
import com.parkingsystem.entity.ParkingSession;
import com.parkingsystem.entity.ParkingSlot;
import com.parkingsystem.helpers.SlotType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Full-stack integration tests: Spring Boot context (Application) → ParkingServiceImpl
 * → Spring Data JPA repositories → H2 in-memory database (dev profile).
 *
 * @Transactional at the class level means every test method runs inside its own
 * transaction that is automatically rolled back after the method returns, so each
 * test starts with a clean, empty database with no manual truncate / @AfterEach cleanup.
 *
 * Exception: the two concurrency tests (A10, B15) opt out of the class-level
 * transaction via Propagation.NOT_SUPPORTED, because spawned threads do not
 * inherit the Spring transaction context.  Those tests set up their data,
 * commit it (so threads can see it), and clean up with @Sql.
 */
@SpringBootTest
@ActiveProfiles("prod")
@Transactional
@TestMethodOrder(MethodOrderer.DisplayName.class)
class ParkingIntegrationTest {

    @Autowired
    private ParkingService service;

    // ════════════════════════════════════════════════════════════════
    // SECTION A — HAPPY PATH TESTS
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("A01. addSlot_regular_increasesCount")
    void a01_addSlot_regular_increasesCount() {
        service.addSlot(SlotType.REGULAR);

        assertEquals(1, service.count());
        assertEquals(1, service.countFree());
        assertEquals(0, service.countBooked());
    }

    @Test
    @DisplayName("A02. addSlot_multiple_types_countedCorrectly")
    void a02_addSlot_multiple_types_countedCorrectly() {
        service.addSlot(SlotType.REGULAR);
        service.addSlot(SlotType.REGULAR);
        service.addSlot(SlotType.ELECTRIC);

        assertEquals(3, service.count());
        assertEquals(2, service.findByType(SlotType.REGULAR).size());
    }

    @Test
    @DisplayName("A03. park_regularSlot_returnsBookedSlot")
    void a03_park_regularSlot_returnsBookedSlot() {
        service.addSlot(SlotType.REGULAR);

        Optional<ParkingSlot> result = service.park("ABC-123");

        assertTrue(result.isPresent());
        assertEquals("ABC-123", result.get().getNumberPlate());
        assertEquals(0, service.countFree());
        assertEquals(1, service.countBooked());
    }

    @Test
    @DisplayName("A04. park_withExplicitType_succeeds")
    void a04_park_withExplicitType_succeeds() {
        service.addSlot(SlotType.REGULAR);

        Optional<ParkingSlot> result = service.park("ABC-123", SlotType.REGULAR);

        assertTrue(result.isPresent());
        assertEquals(1, service.countBooked());
    }

    @Test
    @DisplayName("A05. release_bookedSlot_makesItFree")
    void a05_release_bookedSlot_makesItFree() {
        service.addSlot(SlotType.REGULAR);
        service.park("ABC-123");

        Optional<ParkingSlot> result = service.release("ABC-123");

        assertTrue(result.isPresent());
        assertEquals(1, service.countFree());
        assertEquals(0, service.countBooked());
    }

    @Test
    @DisplayName("A06. removeSlot_existingId_removesIt")
    void a06_removeSlot_existingId_removesIt() {
        ParkingSlot slot = service.addSlot(SlotType.REGULAR);

        Optional<ParkingSlot> result = service.removeSlot(slot.getSlotID());

        assertTrue(result.isPresent());
        assertEquals(0, service.count());
    }

    @Test
    @DisplayName("A07. findAll_findAllFree_findAllBooked_consistent")
    void a07_findAll_findAllFree_findAllBooked_consistent() {
        service.addSlot(SlotType.REGULAR);
        service.addSlot(SlotType.REGULAR);
        service.addSlot(SlotType.REGULAR);
        service.park("P1");
        service.park("P2");

        assertEquals(3, service.findAll().size());
        assertEquals(1, service.findAllFree().size());
        assertEquals(2, service.findAllBooked().size());
    }

    @Test
    @DisplayName("A08. findByNumberPlate_existingPlate_returnsSlot")
    void a08_findByNumberPlate_existingPlate_returnsSlot() {
        service.addSlot(SlotType.REGULAR);
        service.park("PLATE-1");

        Optional<ParkingSlot> result = service.findByNumberPlate("PLATE-1");

        assertTrue(result.isPresent());
        assertEquals("PLATE-1", result.get().getNumberPlate());
    }

    @Test
    @DisplayName("A09. rePark_afterRelease_succeeds")
    void a09_rePark_afterRelease_succeeds() {
        service.addSlot(SlotType.REGULAR);
        service.park("A");
        service.release("A");

        Optional<ParkingSlot> result = service.park("B");

        assertTrue(result.isPresent());
        assertEquals("B", result.get().getNumberPlate());
        assertEquals(1, service.countBooked());
    }

    /*
     * A10 — concurrency test.
     *
     * This test is NOT wrapped in a transaction (propagation = NOT_SUPPORTED would
     * be needed but is not annotated here; the @Transactional on the class still
     * applies, but each spawned thread creates its own transaction context,
     * independent of the parent thread's transaction).
     *
     * Data inserted by the parent transaction is NOT visible to child threads
     * until committed.  Because of this, a concurrency test in a @Transactional
     * class must flush and verify state only within the same thread.
     *
     * The assertion below validates that after all threads complete, the
     * single-slot invariant holds from the perspective of the main thread.
     */
    @Test
    @DisplayName("A10. concurrency_parallelParks_leaveConsistentState")
    void a10_concurrency_parallelParks_leaveConsistentState() throws Exception {
        service.addSlot(SlotType.REGULAR);

        int threads = 10;
        List<Future<Optional<ParkingSlot>>> futures;
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch go = new CountDownLatch(1);
            futures = new ArrayList<>();

            for (int i = 0; i < threads; i++) {
                final int index = i;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return service.park("THREAD-" + index);
                }));
            }

            ready.await();
            go.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(15, TimeUnit.SECONDS));
        }

        for (Future<Optional<ParkingSlot>> f : futures) f.get();

        assertEquals(1, service.count());
        assertEquals(1, service.countBooked());
        assertEquals(0, service.countFree());
    }

    // ════════════════════════════════════════════════════════════════
    // SECTION B — FAILURE / EDGE CASE TESTS
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("B01. park_noFreeSlot_returnsEmpty")
    void b01_park_noFreeSlot_returnsEmpty() {
        service.addSlot(SlotType.REGULAR);
        service.park("FIRST");

        Optional<ParkingSlot> result = service.park("SECOND");

        assertTrue(result.isEmpty());
        assertEquals(1, service.countBooked());
    }

    @Test
    @DisplayName("B02. park_wrongType_returnsEmpty")
    void b02_park_wrongType_returnsEmpty() {
        service.addSlot(SlotType.REGULAR);

        Optional<ParkingSlot> result = service.park("ABC-123", SlotType.ELECTRIC);

        assertTrue(result.isEmpty());
        assertEquals(1, service.countFree(), "REGULAR slot must remain free after a failed typed park");
    }

    @Test
    @DisplayName("B03. park_nullNumberPlate_throws")
    void b03_park_nullNumberPlate_throws() {
        service.addSlot(SlotType.REGULAR);
        // @NonNull on the method parameter throws NullPointerException immediately
        assertThrows(NullPointerException.class, () -> service.park(null));
    }

    @Test
    @DisplayName("B04. park_duplicatePlate_secondCallReturnsEmpty")
    void b04_park_duplicatePlate_secondCallReturnsEmpty() {
        service.addSlot(SlotType.REGULAR);
        service.addSlot(SlotType.REGULAR);
        Optional<ParkingSlot> first = service.park("DUP-1");
        assertTrue(first.isPresent(), "First park must succeed");

        // ParkingServiceImpl.park() checks findByNumberPlate() before booking;
        // a plate already present in an active session returns empty immediately.
        Optional<ParkingSlot> second = service.park("DUP-1");

        assertTrue(second.isEmpty(), "Second park with the same plate must return empty");
        assertEquals(1, service.countBooked(), "Only one slot should be booked");
    }

    @Test
    @DisplayName("B05. release_unknownPlate_returnsEmpty")
    void b05_release_unknownPlate_returnsEmpty() {
        service.addSlot(SlotType.REGULAR);

        Optional<ParkingSlot> result = service.release("GHOST-1");

        assertTrue(result.isEmpty());
        assertEquals(1, service.countFree());
    }

    @Test
    @DisplayName("B06. release_alreadyReleasedSlot_returnsEmpty")
    void b06_release_alreadyReleasedSlot_returnsEmpty() {
        service.addSlot(SlotType.REGULAR);
        service.park("ABC");
        service.release("ABC");

        Optional<ParkingSlot> second = service.release("ABC");

        assertTrue(second.isEmpty(), "Second release of the same plate must return empty (no double-free)");
        assertEquals(1, service.countFree());
    }

    @Test
    @DisplayName("B07. release_nullPlate_throws")
    void b07_release_nullPlate_throws() {
        // @NonNull on the parameter throws before any DB interaction
        assertThrows(NullPointerException.class, () -> service.release(null));
    }

    @Test
    @DisplayName("B08. removeSlot_unknownId_returnsEmpty")
    void b08_removeSlot_unknownId_returnsEmpty() {
        // JPA findById returns Optional.empty() for unknown IDs; the service
        // propagates that without throwing (contrast: old JDBC impl threw SQLException)
        Optional<ParkingSlot> result = service.removeSlot(UUID.randomUUID());

        assertTrue(result.isEmpty());
        assertEquals(0, service.count());
    }

    @Test
    @DisplayName("B09. removeSlot_bookedSlot_violatesForeignKey")
    void b09_removeSlot_bookedSlot_violatesForeignKey() {
        ParkingSlot slot = service.addSlot(SlotType.REGULAR);
        service.park("BOOKED-1");
        // park() creates a ParkingSession row referencing this slot.
        // The FK (parking_sessions.slot_id → slots.slot_id) prevents deletion.
        // Spring wraps the constraint violation as DataIntegrityViolationException.
        assertThrows(DataIntegrityViolationException.class,
                () -> service.removeSlot(slot.getSlotID()));
    }

    @Test
    @DisplayName("B10. findByType_noSlotsOfType_returnsEmptyList")
    void b10_findByType_noSlotsOfType_returnsEmptyList() {
        service.addSlot(SlotType.REGULAR);

        List<ParkingSlot> result = service.findByType(SlotType.ELECTRIC);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("B11. findByNumberPlate_unknownPlate_returnsEmpty")
    void b11_findByNumberPlate_unknownPlate_returnsEmpty() {
        Optional<ParkingSlot> result = service.findByNumberPlate("NOBODY");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("B12. count_onEmptySystem_returnsZero")
    void b12_count_onEmptySystem_returnsZero() {
        assertEquals(0, service.count());
        assertEquals(0, service.countFree());
        assertEquals(0, service.countBooked());
    }

    // ════════════════════════════════════════════════════════════════
    // SECTION M — MANY-TO-MANY (vehicle ⇄ slot over time via parking_sessions)
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("M01. oneSlot_hostsManyPlates_overTime")
    void m01_oneSlot_hostsManyPlates_overTime() {
        ParkingSlot slot = service.addSlot(SlotType.REGULAR);
        service.park("CAR-1");
        service.release("CAR-1");
        service.park("CAR-2");
        service.release("CAR-2");
        service.park("CAR-3");

        List<ParkingSession> history = service.slotHistory(slot.getSlotID());
        assertEquals(3, history.size(), "the single slot accumulated three sessions");
        assertEquals(Set.of("CAR-1", "CAR-2", "CAR-3"),
                history.stream().map(ParkingSession::getNumberPlate).collect(Collectors.toSet()));
        assertEquals(1, service.allSessions().stream().filter(ParkingSession::isActive).count());
    }

    @Test
    @DisplayName("M02. onePlate_usesManySlots_overTime")
    void m02_onePlate_usesManySlots_overTime() {
        ParkingSlot regular  = service.addSlot(SlotType.REGULAR);
        ParkingSlot electric = service.addSlot(SlotType.ELECTRIC);

        service.park("ROVER", SlotType.REGULAR);
        service.release("ROVER");
        service.park("ROVER", SlotType.ELECTRIC);

        List<ParkingSession> history = service.plateHistory("ROVER");
        assertEquals(2, history.size());
        assertEquals(Set.of(regular.getSlotID(), electric.getSlotID()),
                history.stream().map(ParkingSession::getSlotId).collect(Collectors.toSet()));
    }

    @Test
    @DisplayName("M03. release_closesSessionButKeepsHistory")
    void m03_release_closesSessionButKeepsHistory() {
        ParkingSlot slot = service.addSlot(SlotType.REGULAR);
        service.park("KEEP-1");
        service.release("KEEP-1");

        List<ParkingSession> history = service.slotHistory(slot.getSlotID());
        assertEquals(1, history.size(), "closed session is kept as history");
        assertFalse(history.getFirst().isActive());
        assertNotNull(history.getFirst().getReleasedAt());
        assertEquals(0, service.allSessions().stream().filter(ParkingSession::isActive).count());
    }

    @Test
    @DisplayName("M04. matrix_manyPlatesAndManySlots_allLinksRecorded")
    void m04_matrix_manyPlatesAndManySlots_allLinksRecorded() {
        service.addSlot(SlotType.REGULAR);
        service.addSlot(SlotType.REGULAR);

        service.park("A1");
        service.park("A2");
        service.release("A1");
        service.release("A2");
        service.park("B1");
        service.park("B2");

        assertEquals(4, service.allSessions().size(), "every park event is recorded");
        assertEquals(2, service.allSessions().stream().filter(ParkingSession::isActive).count());
    }
}
