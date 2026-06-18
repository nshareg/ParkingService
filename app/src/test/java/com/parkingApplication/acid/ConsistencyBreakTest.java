package com.parkingApplication.acid;

import com.parkingsystem.entity.ParkingSession;
import com.parkingsystem.entity.ParkingSlot;
import com.parkingsystem.helpers.SlotType;
import com.parkingsystem.impl.ParkingRepositoryimpl;
import com.parkingsystem.impl.ParkingSessionRepositoryimpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * In our current db model, we can break the {@code Consistency} by having parking session, without parking slot registered on it. so having an orphan record.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class ConsistencyBreakTest {

    private static final String PLATE = "TEST-OPEL";

    private Connection conn;
    private DataSource dataSource;

    @BeforeEach
    void setUp() throws SQLException {
        conn = AcidTestDB.openConnection();
        AcidTestDB.truncateParkingTables(conn);
        // The repositories borrow their connection from a DataSource; wrap the single test
        // connection (close-suppressed) so the manual commit/rollback below still governs them.
        dataSource = new SingleConnectionDataSource(conn, true);
    }

    @AfterEach
    void tearDown() throws SQLException {
        AcidTestDB.truncateParkingTables(conn);
        conn.close();
    }

    private UUID freshFreeSlot() throws SQLException {
        ParkingSlot slot = new ParkingSlot(SlotType.REGULAR);
        new ParkingRepositoryimpl(dataSource).add(slot);
        return slot.getSlotID();
    }

    private static void powerFailure() {
        throw new RuntimeException("KA-BOOM CRASH!");
    }

    private void parkWithCrashBetweenWrites(boolean transactional, UUID slotId) throws SQLException {
        ParkingRepositoryimpl slots = new ParkingRepositoryimpl(dataSource);
        ParkingSessionRepositoryimpl sessions = new ParkingSessionRepositoryimpl(dataSource);

        if (transactional) conn.setAutoCommit(false);
        try {
            ParkingSlot slot = slots.findById(slotId).orElseThrow();
            slot.book(PLATE);
            slots.update(slot);

            powerFailure();

            sessions.add(new ParkingSession(slotId, PLATE));
            if (transactional) conn.commit();
        } catch (RuntimeException crash) {
            if (transactional) conn.rollback();
        } finally {
            if (transactional) conn.setAutoCommit(true);
        }
    }

    private boolean slotIsBooked(UUID slotId) throws SQLException {
        return new ParkingRepositoryimpl(dataSource).findById(slotId).orElseThrow().isBooked();
    }

    private long activeSessionsFor(UUID slotId) throws SQLException {
        return new ParkingSessionRepositoryimpl(dataSource).findBySlot(slotId).stream()
                .filter(ParkingSession::isActive)
                .count();
    }

    private static String check(boolean booked, long active) {
        boolean holds = booked ? active == 1 : active == 0;
        return holds ? "holds" : "violated";
    }

    @Test
    @DisplayName("C1. WITHOUT a transaction the crash leaves an orphan booking (INCONSISTENT)")
    void withoutTransaction_leavesOrphanBooking() throws SQLException {
        UUID slot = freshFreeSlot();

        parkWithCrashBetweenWrites(false, slot);

        boolean booked = slotIsBooked(slot);
        long active = activeSessionsFor(slot);
        System.out.println("[NO TRANSACTION] slot.booked=" + booked + "  activeSessions=" + active
                + "  -> 'booked <=> active session' " + check(booked, active));

        assertTrue(booked, "write #1 (the booking) auto-committed before the crash");
        assertEquals(0, active, "write #2 (the session) never ran -> booked slot with no session = INCONSISTENT");
    }

    @Test
    @DisplayName("C2. WITH a transaction the crash rolls back cleanly (CONSISTENT)")
    void withTransaction_staysConsistent() throws SQLException {
        UUID slot = freshFreeSlot();

        parkWithCrashBetweenWrites(true, slot);

        boolean booked = slotIsBooked(slot);
        long active = activeSessionsFor(slot);
        System.out.println("[WITH TRANSACTION] slot.booked=" + booked + "  activeSessions=" + active
                + "  -> 'booked <=> active session' " + check(booked, active));

        assertFalse(booked, "rollback undid the booking");
        assertEquals(0, active, "no session either -> a free slot with no session = CONSISTENT");
    }
}
