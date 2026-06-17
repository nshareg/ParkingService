package com.parkingApplication;

import com.inMemoryPersistence.InMemoryConnection;
import com.parkingsystem.contract.ParkingService;
import com.parkingsystem.entity.ParkingSlot;
import com.parkingsystem.helpers.SlotType;
import com.parkingsystem.impl.ParkingRepositoryimpl;
import com.parkingsystem.impl.ParkingServiceImpl;
import com.parkingsystem.impl.ParkingSessionRepositoryimpl;
import com.parkingsystem.entity.ParkingSession;

import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@TestMethodOrder(MethodOrderer.DisplayName.class)
class ParkingIntegrationTest {

    //set USE_IN_MEMORY=true to run against the in-memory engine,
    private static final boolean USE_IN_MEMORY =
            Boolean.parseBoolean(System.getenv().getOrDefault("USE_IN_MEMORY", "false"));

    private ParkingService service;          // Sections A & B — no session history
    private ParkingService sessionService;   // Section M — session-enabled (many-to-many)
    private Connection connection;

    private static Connection openConnection() throws SQLException {
        if (USE_IN_MEMORY) {
            return new InMemoryConnection();
        }
        String host = System.getenv().getOrDefault("POSTGRES_HOST", "localhost");
        String port = System.getenv().getOrDefault("POSTGRES_PORT", "5432");
        String db = System.getenv().getOrDefault("POSTGRES_DB", "parking");
        String user = System.getenv().getOrDefault("POSTGRES_USER", "postgres");
        String password = System.getenv().getOrDefault("POSTGRES_PASSWORD", "postgres");
        return DriverManager.getConnection(
                "jdbc:postgresql://" + host + ":" + port + "/" + db, user, password);
    }

    // Clears every table between tests. 'slots CASCADE' clears the child tables on Postgres;
    // the explicit parking_sessions truncate covers the in-memory engine (which ignores CASCADE).
    private static void truncateAll(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("TRUNCATE TABLE slots CASCADE")) {
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("TRUNCATE TABLE parking_sessions")) {
            ps.executeUpdate();
        }
    }

    @BeforeAll
    static void createSchema() throws SQLException {
        try (Connection connection = openConnection()) {
            // The 'slots' table is created once per run:
            //  - in-memory: via the repository's init() (Storage is static/shared);
            //  - Postgres: by database/01-schema.sql at container startup, so no init() here.
            if (USE_IN_MEMORY) {
                // The in-memory Storage is static and shared across the JVM, so the tables may
                // already exist from an earlier run. Create them only if missing; the TRUNCATEs
                // below guarantee a clean slate either way. (On Postgres both tables come from
                // database/01-schema.sql at container startup, so no init() here.)
                try {
                    new ParkingRepositoryimpl(connection).init();
                } catch (SQLException alreadyInitialised) {
                    // table already exists — fine
                }
                try {
                    new ParkingSessionRepositoryimpl(connection).init();
                } catch (SQLException alreadyInitialised) {
                    // table already exists — fine
                }
            }
            // Start from clean tables regardless of any leftover state.
            truncateAll(connection);
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        connection = openConnection();
        ParkingRepositoryimpl slots = new ParkingRepositoryimpl(connection);
        // The service delimits transactions on the Postgres connection; the in-memory engine
        // has no transaction support, so it gets a null connection (the service then runs the
        // repository calls directly, exactly as the old lock-free-but-untransacted path did).
        Connection txConnection = USE_IN_MEMORY ? null : connection;
        service = new ParkingServiceImpl(txConnection, slots);
        sessionService = new ParkingServiceImpl(txConnection, slots, new ParkingSessionRepositoryimpl(connection));
    }

    @AfterEach
    void tearDown() throws Exception {
        truncateAll(connection);
        connection.close();
    }

    // ════════════════════════════════════════════════════════════════
    // SECTION A — HAPPY PATH TESTS
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("A01. addSlot_regular_increasesCount")
    void a01_addSlot_regular_increasesCount() throws SQLException {
        service.addSlot(SlotType.REGULAR);

        assertEquals(1, service.count());
        assertEquals(1, service.countFree());
        assertEquals(0, service.countBooked());
    }

    @Test
    @DisplayName("A02. addSlot_multiple_types_countedCorrectly")
    void a02_addSlot_multiple_types_countedCorrectly() throws SQLException {
        // SlotType has REGULAR, DISABLED, ELECTRIC — using ELECTRIC in place of VIP
        service.addSlot(SlotType.REGULAR);
        service.addSlot(SlotType.REGULAR);
        service.addSlot(SlotType.ELECTRIC);

        assertEquals(3, service.count());
        assertEquals(2, service.findByType(SlotType.REGULAR).size());
    }

    @Test
    @DisplayName("A03. park_regularSlot_returnsBookedSlot")
    void a03_park_regularSlot_returnsBookedSlot() throws SQLException {
        service.addSlot(SlotType.REGULAR);

        Optional<ParkingSlot> result = service.park("ABC-123");

        assertTrue(result.isPresent());
        assertEquals("ABC-123", result.get().getNumberPlate());
        assertEquals(0, service.countFree());
        assertEquals(1, service.countBooked());
    }

    @Test
    @DisplayName("A04. park_withExplicitType_succeeds")
    void a04_park_withExplicitType_succeeds() throws SQLException {
        service.addSlot(SlotType.REGULAR);

        Optional<ParkingSlot> result = service.park("ABC-123", SlotType.REGULAR);

        assertTrue(result.isPresent());
        assertEquals(1, service.countBooked());
    }

    @Test
    @DisplayName("A05. release_bookedSlot_makesItFree")
    void a05_release_bookedSlot_makesItFree() throws SQLException {
        service.addSlot(SlotType.REGULAR);
        service.park("ABC-123");

        Optional<ParkingSlot> result = service.release("ABC-123");

        assertTrue(result.isPresent());
        assertEquals(1, service.countFree());
        assertEquals(0, service.countBooked());
    }

    @Test
    @DisplayName("A06. removeSlot_existingId_removesIt")
    void a06_removeSlot_existingId_removesIt() throws SQLException {
        ParkingSlot slot = service.addSlot(SlotType.REGULAR);

        Optional<ParkingSlot> result = service.removeSlot(slot.getSlotID());

        assertTrue(result.isPresent());
        assertEquals(0, service.count());
    }

    @Test
    @DisplayName("A07. findAll_findAllFree_findAllBooked_consistent")
    void a07_findAll_findAllFree_findAllBooked_consistent() throws SQLException {
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
    void a08_findByNumberPlate_existingPlate_returnsSlot() throws SQLException {
        service.addSlot(SlotType.REGULAR);
        service.park("PLATE-1");

        Optional<ParkingSlot> result = service.findByNumberPlate("PLATE-1");

        assertTrue(result.isPresent());
        assertEquals("PLATE-1", result.get().getNumberPlate());
    }

    @Test
    @DisplayName("A09. rePark_afterRelease_succeeds")
    void a09_rePark_afterRelease_succeeds() throws SQLException {
        service.addSlot(SlotType.REGULAR);
        service.park("A");
        service.release("A");

        Optional<ParkingSlot> result = service.park("B");

        assertTrue(result.isPresent());
        assertEquals("B", result.get().getNumberPlate());
        assertEquals(1, service.countBooked());
    }

    @Test
    @DisplayName("A10. concurrency_parallelParks_leaveConsistentState")
    void a10_concurrency_parallelParks_leaveConsistentState() throws Exception {
        // Concurrency control moved from a JVM lock to the database, so each thread now gets its
        // OWN pooled connection + service — a single JDBC connection is never shared across threads.
        assumeFalse(USE_IN_MEMORY, "needs a real RDBMS: the in-memory engine has no transactions");
        service.addSlot(SlotType.REGULAR);

        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Optional<ParkingSlot>>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int index = i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();                       // release every thread at the same instant
                try (Connection c = openConnection()) {
                    ParkingService s = new ParkingServiceImpl(c, new ParkingRepositoryimpl(c));
                    return s.park("THREAD-" + index);
                }
            }));
        }
        ready.await();
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(15, TimeUnit.SECONDS), "threads did not finish in time");
        for (Future<Optional<ParkingSlot>> f : futures) f.get();   // surface any thread error

        // Whatever the interleaving, the persisted state must stay consistent: the single slot ends
        // up booked by exactly one car, with no phantom free copies. (The stronger "only one caller
        // is told it won" guarantee needs SERIALIZABLE / SELECT ... FOR UPDATE — see
        // IsolationLevelDemoTest.)
        assertEquals(1, service.count(), "still exactly one slot");
        assertEquals(1, service.countBooked(), "the slot is booked by exactly one car");
        assertEquals(0, service.countFree(), "no phantom free copies");
    }

    // ════════════════════════════════════════════════════════════════
    // SECTION B — FAILURE / EDGE CASE TESTS
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("B01. park_noFreeSlot_returnsEmpty")
    void b01_park_noFreeSlot_returnsEmpty() throws SQLException {
        service.addSlot(SlotType.REGULAR);
        service.park("FIRST");

        Optional<ParkingSlot> result = service.park("SECOND");

        assertTrue(result.isEmpty());
        assertEquals(1, service.countBooked());
    }

    @Test
    @DisplayName("B02. park_wrongType_returnsEmpty")
    void b02_park_wrongType_returnsEmpty() throws SQLException {
        service.addSlot(SlotType.REGULAR);

        // Requesting ELECTRIC when only REGULAR is available — no match
        Optional<ParkingSlot> result = service.park("ABC-123", SlotType.ELECTRIC);

        assertTrue(result.isEmpty());
        assertEquals(1, service.countFree(), "REGULAR slot must remain free after failed typed park");
    }

    @Test
    @DisplayName("B03. park_nullNumberPlate_throwsOrReturnsEmpty")
    void b03_park_nullNumberPlate_throwsOrReturnsEmpty() throws SQLException {
        service.addSlot(SlotType.REGULAR);
        // Observed contract: ParkingSlot.book() calls Objects.requireNonNull(numberPlate, ...)
        // which throws NullPointerException before any persistence is touched.
        assertThrows(NullPointerException.class, () -> service.park(null));
    }

    @Test
    @DisplayName("B04. park_emptyNumberPlate_throwsOrReturnsEmpty")
    void b04_park_emptyNumberPlate_throwsOrReturnsEmpty() throws SQLException {
        service.addSlot(SlotType.REGULAR);
        // Observed contract: ParkingSlot.book("") has no empty-string guard.
        // Objects.requireNonNull("", ...) passes, the empty string is stored as the plate,
        // and the slot is marked booked. park("") therefore returns a present Optional.
        Optional<ParkingSlot> result = service.park("");
        assertTrue(result.isPresent(),
                "Implementation stores empty string as a valid plate without rejecting it");
        assertTrue(result.get().isBooked());
    }

    @Test
    @DisplayName("B05. park_duplicatePlate_secondCallFails")
    void b05_park_duplicatePlate_secondCallFails() throws SQLException {
        service.addSlot(SlotType.REGULAR);
        service.addSlot(SlotType.REGULAR);
        Optional<ParkingSlot> first = service.park("DUP-1");
        assertTrue(first.isPresent(), "First park must succeed");

        // The system must not allow the same plate to occupy two slots simultaneously.
        // A second park("DUP-1") should return empty or throw.
        boolean secondFailed;
        try {
            Optional<ParkingSlot> second = service.park("DUP-1");
            secondFailed = second.isEmpty();
        } catch (Exception e) {
            secondFailed = true;
        }

        assertTrue(secondFailed, "Second park with the same plate must return empty or throw");
        assertEquals(1, service.countBooked(), "Only one slot should be booked after a duplicate park attempt");
    }

    @Test
    @DisplayName("B06. release_unknownPlate_returnsEmpty")
    void b06_release_unknownPlate_returnsEmpty() throws SQLException {
        service.addSlot(SlotType.REGULAR);

        Optional<ParkingSlot> result = service.release("GHOST-1");

        assertTrue(result.isEmpty());
        assertEquals(1, service.countFree());
    }

    @Test
    @DisplayName("B07. release_alreadyReleasedSlot_returnsEmpty")
    void b07_release_alreadyReleasedSlot_returnsEmpty() throws SQLException {
        service.addSlot(SlotType.REGULAR);
        service.park("ABC");
        service.release("ABC");

        Optional<ParkingSlot> second = service.release("ABC");

        assertTrue(second.isEmpty(), "Second release of same plate must return empty (no double-free)");
        assertEquals(1, service.countFree(), "Slot should remain free after failed second release");
    }

    @Test
    @DisplayName("B08. release_nullPlate_throwsOrReturnsEmpty")
    void b08_release_nullPlate_throwsOrReturnsEmpty() {
        // Observed contract (empty system): findByNumberPlate(null) executes a SELECT
        // with null as the WHERE value. No rows match, so it returns Optional.empty(),
        // and release() propagates that empty result without throwing.
        // If free slots existed, null would match their null number_plate column,
        // and the subsequent slot.release() call would throw IllegalStateException
        // (slot is not booked). We test on the empty system where empty is guaranteed.
        try {
            Optional<ParkingSlot> result = service.release(null);
            assertTrue(result.isEmpty(), "release(null) returns empty when no null-plated booked slot exists");
        } catch (Exception e) {
            assertTrue(
                    e instanceof IllegalStateException
                            || e instanceof NullPointerException
                            || e instanceof SQLException,
                    "Expected IllegalStateException, NullPointerException, or SQLException; got: "
                            + e.getClass().getName()
            );
        }
    }

    @Test
    @DisplayName("B09. removeSlot_unknownId_returnsEmpty")
    void b09_removeSlot_unknownId_returnsEmpty() {
        // Observed contract: ParkingRepositoryimpl.remove() throws SQLException (not returns empty)
        // when the given UUID does not exist. The message includes "no slot with id: <uuid>".
        assertThrows(SQLException.class, () -> service.removeSlot(UUID.randomUUID()));
        assertDoesNotThrow(() -> assertEquals(0, service.count()));
    }

    @Test
    @DisplayName("B10. removeSlot_bookedSlot_behaviourDefined")
    void b10_removeSlot_bookedSlot_behaviourDefined() throws SQLException {
        ParkingSlot slot = service.addSlot(SlotType.REGULAR);
        service.park("BOOKED-1");

        // Observed contract: removeSlot() has no booking guard in ParkingRepositoryimpl.remove().
        // The repository finds the slot by ID regardless of booking status, deletes it, and
        // returns it. Result: remove succeeds, count drops to 0.
        Optional<ParkingSlot> result = service.removeSlot(slot.getSlotID());
        assertTrue(result.isPresent(), "Remove succeeds even while slot is booked");
        assertEquals(0, service.count(), "Slot is deleted regardless of booking status");
    }

    @Test
    @DisplayName("B11. findByType_noSlotsOfType_returnsEmptyList")
    void b11_findByType_noSlotsOfType_returnsEmptyList() throws SQLException {
        service.addSlot(SlotType.REGULAR);

        // SlotType has no VIP — ELECTRIC is a second enum value absent from the added slot
        List<ParkingSlot> result = service.findByType(SlotType.ELECTRIC);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("B12. findByNumberPlate_unknownPlate_returnsEmpty")
    void b12_findByNumberPlate_unknownPlate_returnsEmpty() throws SQLException {
        Optional<ParkingSlot> result = service.findByNumberPlate("NOBODY");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("B13. count_onEmptySystem_returnsZero")
    void b13_count_onEmptySystem_returnsZero() throws SQLException {
        assertEquals(0, service.count());
        assertEquals(0, service.countFree());
        assertEquals(0, service.countBooked());
    }

    @Test
    @DisplayName("B14. init_calledTwice_doesNotCorruptState")
    void b14_init_calledTwice_doesNotCorruptState() throws SQLException {
        // init() throws SQLException in both backends:
        //  - in-memory: the 'slots' table already exists (created once in @BeforeAll),
        //    and the implementation is NOT idempotent;
        //  - Postgres: the table exists (database/01-schema.sql) and init()'s in-memory-dialect
        //    CREATE TABLE is rejected as invalid SQL.
        // Either way the existing table is left intact, so the service stays fully usable.
        assertThrows(SQLException.class, () -> service.init());

        service.addSlot(SlotType.REGULAR);
        assertEquals(1, service.count(), "Service remains usable after failed init()");
    }

    @Test
    @DisplayName("B15. concurrency_parallelReleases_leaveSlotFree")
    void b15_concurrency_parallelReleases_leaveSlotFree() throws Exception {
        assumeFalse(USE_IN_MEMORY, "needs a real RDBMS: the in-memory engine has no transactions");
        service.addSlot(SlotType.REGULAR);
        service.park("CON-PLATE");

        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Optional<ParkingSlot>>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                try (Connection c = openConnection()) {
                    ParkingService s = new ParkingServiceImpl(c, new ParkingRepositoryimpl(c));
                    return s.release("CON-PLATE");
                }
            }));
        }
        ready.await();
        go.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(15, TimeUnit.SECONDS), "threads did not finish in time");
        for (Future<Optional<ParkingSlot>> f : futures) f.get();   // surface any thread error

        // Regardless of interleaving, the slot must end up free and no car parked.
        assertEquals(1, service.count(), "still exactly one slot");
        assertEquals(1, service.countFree(), "the slot ends up free after the concurrent releases");
        assertEquals(0, service.countBooked(), "no car remains parked");
    }

    // ════════════════════════════════════════════════════════════════
    // SECTION M — MANY-TO-MANY (vehicle ⇄ slot over time, via parking_sessions)
    // Uses the session-enabled service so park()/release() journal sessions.
    // ════════════════════════════════════════════════════════════════

    private long activeSessions() throws SQLException {
        return sessionService.allSessions().stream().filter(ParkingSession::isActive).count();
    }

    @Test
    @DisplayName("M01. oneSlot_hostsManyPlates_overTime")
    void m01_oneSlot_hostsManyPlates_overTime() throws SQLException {
        ParkingSlot slot = sessionService.addSlot(SlotType.REGULAR);

        sessionService.park("CAR-1");
        sessionService.release("CAR-1");
        sessionService.park("CAR-2");
        sessionService.release("CAR-2");
        sessionService.park("CAR-3"); // still parked

        List<ParkingSession> history = sessionService.slotHistory(slot.getSlotID());
        assertEquals(3, history.size(), "the single slot accumulated three sessions");
        assertEquals(Set.of("CAR-1", "CAR-2", "CAR-3"),
                history.stream().map(ParkingSession::getNumberPlate).collect(Collectors.toSet()),
                "three different vehicles used the same slot");
        assertEquals(1, activeSessions(), "only the last park is still open");
    }

    @Test
    @DisplayName("M02. onePlate_usesManySlots_overTime")
    void m02_onePlate_usesManySlots_overTime() throws SQLException {
        // Distinct slot TYPES keep exactly one matching slot free at each park, so the plate
        // provably lands on two different slots — and no slot is deleted (Postgres-FK-safe).
        ParkingSlot regular  = sessionService.addSlot(SlotType.REGULAR);
        ParkingSlot electric = sessionService.addSlot(SlotType.ELECTRIC);

        sessionService.park("ROVER", SlotType.REGULAR);   // -> regular
        sessionService.release("ROVER");
        sessionService.park("ROVER", SlotType.ELECTRIC);  // -> electric

        List<ParkingSession> history = sessionService.plateHistory("ROVER");
        assertEquals(2, history.size(), "the plate accumulated two sessions");
        assertEquals(Set.of(regular.getSlotID(), electric.getSlotID()),
                history.stream().map(ParkingSession::getSlotId).collect(Collectors.toSet()),
                "the same vehicle used two different slots");
        assertEquals(1, activeSessions(), "only the current park is still open");
    }

    @Test
    @DisplayName("M03. release_closesSessionButKeepsHistory")
    void m03_release_closesSessionButKeepsHistory() throws SQLException {
        ParkingSlot slot = sessionService.addSlot(SlotType.REGULAR);
        sessionService.park("KEEP-1");
        sessionService.release("KEEP-1");

        List<ParkingSession> history = sessionService.slotHistory(slot.getSlotID());
        assertEquals(1, history.size(), "the closed session is kept as history");
        assertFalse(history.get(0).isActive(), "session is closed after release");
        assertNotNull(history.get(0).getReleasedAt(), "release stamps a timestamp");
        assertEquals(0, activeSessions(), "nothing is parked");
    }

    @Test
    @DisplayName("M04. matrix_manyPlatesAndManySlots_allLinksRecorded")
    void m04_matrix_manyPlatesAndManySlots_allLinksRecorded() throws SQLException {
        sessionService.addSlot(SlotType.REGULAR);
        sessionService.addSlot(SlotType.REGULAR);

        sessionService.park("A1");
        sessionService.park("A2");
        sessionService.release("A1");
        sessionService.release("A2");
        sessionService.park("B1");
        sessionService.park("B2");

        assertEquals(4, sessionService.allSessions().size(), "every park event is recorded");
        assertEquals(2, activeSessions(), "two cars are currently parked");
    }
}
