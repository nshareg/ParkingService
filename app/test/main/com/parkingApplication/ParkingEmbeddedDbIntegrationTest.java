package main.com.parkingApplication;

import main.com.parkingsystem.contract.ParkingService;
import main.com.parkingsystem.entity.ParkingSession;
import main.com.parkingsystem.entity.ParkingSlot;
import main.com.parkingsystem.helpers.SlotType;
import main.com.parkingsystem.impl.ParkingRepositoryimpl;
import main.com.parkingsystem.impl.ParkingServiceImpl;
import main.com.parkingsystem.impl.ParkingSessionRepositoryimpl;

import org.h2.tools.RunScript;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class ParkingEmbeddedDbIntegrationTest {

    private static final String URL = "jdbc:h2:mem:parking_embedded_it;DB_CLOSE_DELAY=-1";

    // Stable ids from seed.sql.
    private static final UUID S1_REGULAR_FREE    = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID S2_REGULAR_BOOKED  = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID S3_ELECTRIC_BOOKED = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID S4_ELECTRIC_FREE   = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID S5_DISABLED_FREE   = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID SESSION_S1_CAR100_CLOSED = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");

    private Connection connection;
    private ParkingRepositoryimpl slotRepo;
    private ParkingSessionRepositoryimpl sessionRepo;
    private ParkingService service;

    // ── lifecycle ───────────────────────────────────────────────────────────

    @BeforeAll
    void initSchemaAndSeed() throws Exception {
        connection = DriverManager.getConnection(URL, "sa", "");
        runScript("/db/schema.sql");
        runScript("/db/seed.sql");
        connection.setAutoCommit(false);

        slotRepo = new ParkingRepositoryimpl(connection);
        sessionRepo = new ParkingSessionRepositoryimpl(connection);
        service = new ParkingServiceImpl(slotRepo, sessionRepo);
    }

    @AfterEach
    void rollbackPerTest() throws SQLException {
        connection.rollback();
    }

    @AfterAll
    void closeConnection() throws SQLException {
        if (connection != null) connection.close();
    }

    private void runScript(String resource) throws Exception {
        try (Reader r = new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream(resource),
                        "missing test resource " + resource), StandardCharsets.UTF_8)) {
            RunScript.execute(connection, r);
        }
    }

    private long activeSessions() throws SQLException {
        return sessionRepo.findAll().stream().filter(ParkingSession::isActive).count();
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION A — PRELOADED DATA (the init script seeded the expected rows)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("A01. seededSlots_countsAreCorrect")
    void a01_seededSlots_countsAreCorrect() throws SQLException {
        assertEquals(5, service.count());
        assertEquals(3, service.countFree());
        assertEquals(2, service.countBooked());
    }

    @Test
    @DisplayName("A02. seededSlots_findByType")
    void a02_seededSlots_findByType() throws SQLException {
        assertEquals(2, service.findByType(SlotType.REGULAR).size());
        assertEquals(2, service.findByType(SlotType.ELECTRIC).size());
        assertEquals(1, service.findByType(SlotType.DISABLED).size());
    }

    @Test
    @DisplayName("A03. seededSlots_freeAndBookedLists")
    void a03_seededSlots_freeAndBookedLists() throws SQLException {
        assertEquals(3, service.findAllFree().size());
        assertEquals(2, service.findAllBooked().size());
        assertEquals(5, service.findAll().size());
    }

    @Test
    @DisplayName("A04. seededSlots_findByNumberPlate")
    void a04_seededSlots_findByNumberPlate() throws SQLException {
        Optional<ParkingSlot> car100 = service.findByNumberPlate("CAR-100");
        assertTrue(car100.isPresent());
        assertEquals(S2_REGULAR_BOOKED, car100.get().getSlotID());
        assertTrue(car100.get().isBooked());
        assertEquals(SlotType.REGULAR, car100.get().getType());

        assertEquals(S3_ELECTRIC_BOOKED, service.findByNumberPlate("CAR-200").orElseThrow().getSlotID());
    }

    @Test
    @DisplayName("A05. seededSessions_totalsAndActiveCount")
    void a05_seededSessions_totalsAndActiveCount() throws SQLException {
        assertEquals(6, service.allSessions().size());
        assertEquals(2, activeSessions());
    }

    @Test
    @DisplayName("A06. seededSessions_slotHistory")
    void a06_seededSessions_slotHistory() throws SQLException {
        List<ParkingSession> history = service.slotHistory(S1_REGULAR_FREE);
        assertEquals(2, history.size());
        assertEquals(Set.of("CAR-100", "CAR-300"),
                history.stream().map(ParkingSession::getNumberPlate).collect(Collectors.toSet()));
    }

    @Test
    @DisplayName("A07. seededSessions_plateHistory")
    void a07_seededSessions_plateHistory() throws SQLException {
        List<ParkingSession> history = service.plateHistory("CAR-100");
        assertEquals(3, history.size());
        assertEquals(Set.of(S1_REGULAR_FREE, S2_REGULAR_BOOKED),
                history.stream().map(ParkingSession::getSlotId).collect(Collectors.toSet()));
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION B — SLOT REPOSITORY CRUD (positive + negative)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("B01. findById_existingSlot_mapsRow")
    void b01_findById_existingSlot_mapsRow() throws SQLException {
        ParkingSlot s3 = slotRepo.findById(S3_ELECTRIC_BOOKED).orElseThrow();
        assertEquals(SlotType.ELECTRIC, s3.getType());
        assertTrue(s3.isBooked());
        assertEquals("CAR-200", s3.getNumberPlate());
    }

    @Test
    @DisplayName("B02. findById_unknownId_returnsEmpty")
    void b02_findById_unknownId_returnsEmpty() throws SQLException {
        assertTrue(slotRepo.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    @DisplayName("B03. add_newSlot_isPersisted")
    void b03_add_newSlot_isPersisted() throws SQLException {
        ParkingSlot fresh = new ParkingSlot(SlotType.ELECTRIC);
        slotRepo.add(fresh);

        assertEquals(6, slotRepo.count());
        assertTrue(slotRepo.findById(fresh.getSlotID()).isPresent());
    }

    @Test
    @DisplayName("B04. add_duplicatePrimaryKey_throws")
    void b04_add_duplicatePrimaryKey_throws() {
        ParkingSlot clash = new ParkingSlot(S1_REGULAR_FREE, SlotType.REGULAR, false, null);
        assertThrows(SQLException.class, () -> slotRepo.add(clash));
    }

    @Test
    @DisplayName("B05. update_existingSlot_changesPersist")
    void b05_update_existingSlot_changesPersist() throws SQLException {
        slotRepo.update(new ParkingSlot(S1_REGULAR_FREE, SlotType.REGULAR, true, "UPD-1"));

        ParkingSlot reloaded = slotRepo.findById(S1_REGULAR_FREE).orElseThrow();
        assertTrue(reloaded.isBooked());
        assertEquals("UPD-1", reloaded.getNumberPlate());
        assertEquals(3, slotRepo.countBooked());
    }

    @Test
    @DisplayName("B06. update_unknownSlot_throws")
    void b06_update_unknownSlot_throws() {
        ParkingSlot ghost = new ParkingSlot(UUID.randomUUID(), SlotType.REGULAR, false, null);
        assertThrows(SQLException.class, () -> slotRepo.update(ghost));
    }

    @Test
    @DisplayName("B07. remove_unreferencedSlot_succeeds")
    void b07_remove_unreferencedSlot_succeeds() throws SQLException {
        // S4 has no parking_sessions referencing it -> FK-safe to delete
        assertTrue(slotRepo.remove(S4_ELECTRIC_FREE).isPresent());
        assertEquals(4, slotRepo.count());
        assertTrue(slotRepo.findById(S4_ELECTRIC_FREE).isEmpty());
    }

    @Test
    @DisplayName("B08. remove_unknownId_throws")
    void b08_remove_unknownId_throws() {
        assertThrows(SQLException.class, () -> slotRepo.remove(UUID.randomUUID()));
    }

    @Test
    @DisplayName("B09. remove_slotReferencedBySession_violatesForeignKey")
    void b09_remove_slotReferencedBySession_violatesForeignKey() {
        // S2 is referenced by parking_sessions -> the FK must block the delete
        assertThrows(SQLException.class, () -> slotRepo.remove(S2_REGULAR_BOOKED));
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION C — SESSION REPOSITORY / junction table (positive + negative)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("C01. findBySlot_returnsAllSessionsOfSlot")
    void c01_findBySlot_returnsAllSessionsOfSlot() throws SQLException {
        assertEquals(2, sessionRepo.findBySlot(S1_REGULAR_FREE).size());
    }

    @Test
    @DisplayName("C02. findByNumberPlate_returnsWholePlateHistory")
    void c02_findByNumberPlate_returnsWholePlateHistory() throws SQLException {
        assertEquals(3, sessionRepo.findByNumberPlate("CAR-100").size());
    }

    @Test
    @DisplayName("C03. findActiveByNumberPlate_openSession_present")
    void c03_findActiveByNumberPlate_openSession_present() throws SQLException {
        Optional<ParkingSession> active = sessionRepo.findActiveByNumberPlate("CAR-100");
        assertTrue(active.isPresent());
        assertTrue(active.get().isActive());
        assertEquals(S2_REGULAR_BOOKED, active.get().getSlotId());
    }

    @Test
    @DisplayName("C04. findActiveByNumberPlate_onlyClosedSessions_empty")
    void c04_findActiveByNumberPlate_onlyClosedSessions_empty() throws SQLException {
        // CAR-300 has a single, already-closed session
        assertTrue(sessionRepo.findActiveByNumberPlate("CAR-300").isEmpty());
    }

    @Test
    @DisplayName("C05. findById_knownAndUnknown")
    void c05_findById_knownAndUnknown() throws SQLException {
        ParkingSession s = sessionRepo.findById(SESSION_S1_CAR100_CLOSED).orElseThrow();
        assertEquals("CAR-100", s.getNumberPlate());
        assertEquals(S1_REGULAR_FREE, s.getSlotId());
        assertFalse(s.isActive());
        assertNotNull(s.getReleasedAt());

        assertTrue(sessionRepo.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    @DisplayName("C06. add_sessionForExistingSlot_isPersisted")
    void c06_add_sessionForExistingSlot_isPersisted() throws SQLException {
        sessionRepo.add(new ParkingSession(S4_ELECTRIC_FREE, "NEW-CAR"));
        assertEquals(7, sessionRepo.count());
        assertEquals(1, sessionRepo.findByNumberPlate("NEW-CAR").size());
    }

    @Test
    @DisplayName("C07. add_sessionForUnknownSlot_violatesForeignKey")
    void c07_add_sessionForUnknownSlot_violatesForeignKey() {
        ParkingSession orphan = new ParkingSession(UUID.randomUUID(), "ORPHAN");
        assertThrows(SQLException.class, () -> sessionRepo.add(orphan));
    }

    @Test
    @DisplayName("C08. update_closesActiveSession")
    void c08_update_closesActiveSession() throws SQLException {
        ParkingSession active = sessionRepo.findActiveByNumberPlate("CAR-100").orElseThrow();
        active.close();
        sessionRepo.update(active);

        assertTrue(sessionRepo.findActiveByNumberPlate("CAR-100").isEmpty());
        assertEquals(1, activeSessions());
    }

    @Test
    @DisplayName("C09. remove_knownSession_succeeds_unknown_throws")
    void c09_remove_knownSession_succeeds_unknown_throws() throws SQLException {
        assertTrue(sessionRepo.remove(SESSION_S1_CAR100_CLOSED).isPresent());
        assertEquals(5, sessionRepo.count());
        assertThrows(SQLException.class, () -> sessionRepo.remove(UUID.randomUUID()));
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION D — SERVICE park / release / addSlot / removeSlot (positive + negative)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("D01. park_freeRegularSlot_booksItAndJournalsSession")
    void d01_park_freeRegularSlot_booksItAndJournalsSession() throws SQLException {
        Optional<ParkingSlot> parked = service.park("NEW-CAR");

        assertTrue(parked.isPresent());
        assertEquals(S1_REGULAR_FREE, parked.get().getSlotID());
        assertEquals(3, service.countBooked());
        assertEquals(7, service.allSessions().size());
        assertEquals(3, activeSessions());
    }

    @Test
    @DisplayName("D02. park_explicitElectricType_booksElectricSlot")
    void d02_park_explicitElectricType_booksElectricSlot() throws SQLException {
        Optional<ParkingSlot> parked = service.park("EV-1", SlotType.ELECTRIC);
        assertTrue(parked.isPresent());
        assertEquals(S4_ELECTRIC_FREE, parked.get().getSlotID());
    }

    @Test
    @DisplayName("D03. park_alreadyParkedPlate_returnsEmpty")
    void d03_park_alreadyParkedPlate_returnsEmpty() throws SQLException {
        // CAR-100 already occupies S2 -> must not be parked twice
        assertTrue(service.park("CAR-100").isEmpty());
        assertEquals(2, service.countBooked());
    }

    @Test
    @DisplayName("D04. park_noFreeSlotOfType_returnsEmpty")
    void d04_park_noFreeSlotOfType_returnsEmpty() throws SQLException {
        assertTrue(service.park("R1").isPresent());      // takes the only free REGULAR (S1)
        assertTrue(service.park("R2").isEmpty());         // none left
    }

    @Test
    @DisplayName("D05. park_nullPlate_throws")
    void d05_park_nullPlate_throws() {
        assertThrows(NullPointerException.class, () -> service.park(null));
    }

    @Test
    @DisplayName("D06. release_bookedPlate_freesSlotAndClosesSession")
    void d06_release_bookedPlate_freesSlotAndClosesSession() throws SQLException {
        Optional<ParkingSlot> released = service.release("CAR-100");

        assertTrue(released.isPresent());
        assertEquals(S2_REGULAR_BOOKED, released.get().getSlotID());
        assertFalse(released.get().isBooked());
        assertEquals(1, service.countBooked());
        assertEquals(1, activeSessions(), "the CAR-100 session is now closed");
    }

    @Test
    @DisplayName("D07. release_unknownPlate_returnsEmpty")
    void d07_release_unknownPlate_returnsEmpty() throws SQLException {
        assertTrue(service.release("GHOST").isEmpty());
        assertEquals(2, service.countBooked());
    }

    @Test
    @DisplayName("D08. release_twice_secondReturnsEmpty")
    void d08_release_twice_secondReturnsEmpty() throws SQLException {
        assertTrue(service.release("CAR-200").isPresent());
        assertTrue(service.release("CAR-200").isEmpty(), "no double-free");
    }

    @Test
    @DisplayName("D09. release_nullPlate_throws")
    void d09_release_nullPlate_throws() {
        assertThrows(NullPointerException.class, () -> service.release(null));
    }

    @Test
    @DisplayName("D10. addSlot_throughService_increasesCount")
    void d10_addSlot_throughService_increasesCount() throws SQLException {
        service.addSlot(SlotType.DISABLED);
        assertEquals(6, service.count());
        assertEquals(4, service.countFree());
    }

    @Test
    @DisplayName("D11. removeSlot_throughService_knownAndUnknown")
    void d11_removeSlot_throughService_knownAndUnknown() throws SQLException {
        assertTrue(service.removeSlot(S5_DISABLED_FREE).isPresent());
        assertEquals(4, service.count());
        assertThrows(SQLException.class, () -> service.removeSlot(UUID.randomUUID()));
    }

    @Test
    @DisplayName("D12. addSlot_nullType_throws")
    void d12_addSlot_nullType_throws() {
        assertThrows(NullPointerException.class, () -> service.addSlot(null));
    }

    // ════════════════════════════════════════════════════════════════════════
    // SECTION M — MANY-TO-MANY over time (vehicle ⇄ slot via parking_sessions)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("M01. oneSlot_accumulatesAnotherPlate_overTime")
    void m01_oneSlot_accumulatesAnotherPlate_overTime() throws SQLException {
        // S1 already hosted CAR-100 and CAR-300; park a third plate onto it.
        service.park("M-CAR");   // S1 is the only free REGULAR -> lands on S1

        List<ParkingSession> history = service.slotHistory(S1_REGULAR_FREE);
        assertEquals(3, history.size());
        assertEquals(Set.of("CAR-100", "CAR-300", "M-CAR"),
                history.stream().map(ParkingSession::getNumberPlate).collect(Collectors.toSet()));
    }

    @Test
    @DisplayName("M02. onePlate_spansAnotherSlot_overTime")
    void m02_onePlate_spansAnotherSlot_overTime() throws SQLException {
        // CAR-100 currently on S2; release then re-park -> lands on the free REGULAR S1.
        service.release("CAR-100");
        Optional<ParkingSlot> reparked = service.park("CAR-100");

        assertTrue(reparked.isPresent());
        assertEquals(S1_REGULAR_FREE, reparked.get().getSlotID());

        List<ParkingSession> history = service.plateHistory("CAR-100");
        assertEquals(4, history.size(), "the re-park adds a 4th session for the plate");
        assertEquals(Set.of(S1_REGULAR_FREE, S2_REGULAR_BOOKED),
                history.stream().map(ParkingSession::getSlotId).collect(Collectors.toSet()));
    }

    @Test
    @DisplayName("M03. release_closesSessionButKeepsHistory")
    void m03_release_closesSessionButKeepsHistory() throws SQLException {
        service.release("CAR-200");

        List<ParkingSession> history = service.slotHistory(S3_ELECTRIC_BOOKED);
        assertEquals(2, history.size(), "both S3 sessions remain as history");
        assertEquals(1, activeSessions(), "only the still-open CAR-100 session remains active");
    }
}
