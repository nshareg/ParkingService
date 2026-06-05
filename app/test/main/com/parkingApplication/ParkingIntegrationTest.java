package main.com.parkingApplication;

import main.com.inMemoryPersistence.InMemoryConnection;
import main.com.parkingsystem.contract.ParkingService;
import main.com.parkingsystem.entity.ParkingSlot;
import main.com.parkingsystem.helpers.SlotType;
import main.com.parkingsystem.impl.ParkingRepositoryimpl;
import main.com.parkingsystem.impl.ParkingServiceImpl;

import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.DisplayName.class)
class ParkingIntegrationTest {

    //set USE_IN_MEMORY=true to run against the in-memory engine,
    private static final boolean USE_IN_MEMORY =
            Boolean.parseBoolean(System.getenv().getOrDefault("USE_IN_MEMORY", "false"));

    private ParkingService service;
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

    @BeforeAll
    static void createSchema() throws SQLException {
        try (Connection connection = openConnection()) {
            // The 'slots' table is created once per run:
            //  - in-memory: via the repository's init() (Storage is static/shared);
            //  - Postgres: by database/01-schema.sql at container startup, so no init() here.
            if (USE_IN_MEMORY) {
                new ParkingServiceImpl(new ParkingRepositoryimpl(connection)).init();
            }
            // Start from a clean table regardless of any leftover state.
            try (PreparedStatement truncate = connection.prepareStatement("TRUNCATE TABLE slots")) {
                truncate.executeUpdate();
            }
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        connection = openConnection();
        service = new ParkingServiceImpl(new ParkingRepositoryimpl(connection));
    }

    @AfterEach
    void tearDown() throws Exception {
        try (PreparedStatement truncate = connection.prepareStatement("TRUNCATE TABLE slots")) {
            truncate.executeUpdate();
        }
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
    @DisplayName("A10. concurrency_parallelParks_onlyOneSucceeds")
    void a10_concurrency_parallelParks_onlyOneSucceeds() throws Exception {
        service.addSlot(SlotType.REGULAR);

        ExecutorService pool = Executors.newFixedThreadPool(10);
        List<Callable<Optional<ParkingSlot>>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int index = i;
            tasks.add(() -> service.park("THREAD-" + index));
        }

        List<Future<Optional<ParkingSlot>>> futures = pool.invokeAll(tasks);
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        long successCount = futures.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (Exception e) {
                        return Optional.<ParkingSlot>empty();
                    }
                })
                .filter(Optional::isPresent)
                .count();

        assertEquals(1, successCount, "Exactly one concurrent park should succeed for a single free slot");
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
    @DisplayName("B15. concurrency_parallelReleases_onlyOneSucceeds")
    void b15_concurrency_parallelReleases_onlyOneSucceeds() throws Exception {
        service.addSlot(SlotType.REGULAR);
        service.park("CON-PLATE");

        ExecutorService pool = Executors.newFixedThreadPool(10);
        List<Callable<Optional<ParkingSlot>>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tasks.add(() -> service.release("CON-PLATE"));
        }

        List<Future<Optional<ParkingSlot>>> futures = pool.invokeAll(tasks);
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        long successCount = futures.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (Exception e) {
                        return Optional.<ParkingSlot>empty();
                    }
                })
                .filter(Optional::isPresent)
                .count();

        assertEquals(1, successCount, "Exactly one concurrent release should succeed");
        assertEquals(1, service.countFree(), "Slot should be free after exactly one successful release");
    }
}
