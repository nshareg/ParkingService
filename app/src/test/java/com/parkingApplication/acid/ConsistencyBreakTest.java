package com.parkingApplication.acid;

import com.parkingsystem.helpers.SlotType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * In our current db model, we can break the {@code Consistency} by having a parking session
 * without a parking slot registered on it — i.e. an orphan record.
 */
@TestMethodOrder(MethodOrderer.DisplayName.class)
class ConsistencyBreakTest {

    private static final String PLATE = "TEST-OPEL";

    private Connection conn;

    @BeforeEach
    void setUp() throws SQLException {
        conn = AcidTestDB.openConnection();
        AcidTestDB.truncateParkingTables(conn);
    }

    @AfterEach
    void tearDown() throws SQLException {
        AcidTestDB.truncateParkingTables(conn);
        conn.close();
    }

    private UUID freshFreeSlot() throws SQLException {
        return AcidTestDB.insertFreeSlot(conn, SlotType.REGULAR);
    }

    private static void powerFailure() {
        throw new RuntimeException("KA-BOOM CRASH!");
    }

    private void parkWithCrashBetweenWrites(boolean transactional, UUID slotId) throws SQLException {
        if (transactional) conn.setAutoCommit(false);
        try {
            AcidTestDB.bookSlot(conn, slotId, PLATE);

            powerFailure();

            AcidTestDB.insertSession(conn, slotId, PLATE);
            if (transactional) conn.commit();
        } catch (RuntimeException crash) {
            if (transactional) conn.rollback();
        } finally {
            if (transactional) conn.setAutoCommit(true);
        }
    }

    private boolean slotIsBooked(UUID slotId) throws SQLException {
        return AcidTestDB.isBooked(conn, slotId);
    }

    private long activeSessionsFor(UUID slotId) throws SQLException {
        return AcidTestDB.activeSessions(conn, slotId);
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
