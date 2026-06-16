package com.parkingApplication.acid;

import com.parkingsystem.helpers.SlotType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IsolationLevelTest {

    private static final int TIMEOUT_SECONDS = 10;

    private boolean tryPark(int isolation, String plate, CountDownLatch raceLatch, CountDownLatch doneLatch) {
        try (Connection c = AcidTestDB.openConnection()) {
            c.setTransactionIsolation(isolation);
            c.setAutoCommit(false);

            Optional<UUID> free = AcidTestDB.firstFreeSlot(c);

            raceLatch.countDown();
            if (!raceLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                c.rollback();
                throw new IllegalStateException("race gate timed out — other thread likely crashed");
            }

            if (free.isEmpty()) { c.rollback(); return false; }

            UUID slotId = free.get();
            AcidTestDB.bookSlot(c, slotId, plate);
            AcidTestDB.insertSession(c, slotId, plate);
            c.commit();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            doneLatch.countDown();
        }
    }

    private long race(int isolation) throws Exception {
        try (Connection c = AcidTestDB.openConnection()) {
            AcidTestDB.truncateParkingTables(c);
            AcidTestDB.insertFreeSlot(c, SlotType.REGULAR);
        }

        CountDownLatch raceLatch = new CountDownLatch(2);
        CountDownLatch doneLatch = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            pool.submit(() -> tryPark(isolation, "CAR-A", raceLatch, doneLatch));
            pool.submit(() -> tryPark(isolation, "CAR-B", raceLatch, doneLatch));

            if (!doneLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("[failed] timeout for: " + TIMEOUT_SECONDS);
            }
            return activeSessions();
        } finally {
            pool.shutdownNow();
        }
    }

    private long activeSessions() throws Exception {
        try (Connection c = AcidTestDB.openConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT count(*) FROM parking_sessions WHERE active = true");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    @Test
    @DisplayName("READ COMMITTED double-books the last slot")
    void readCommitted_doubleBooks() throws Exception {
        assertEquals(2, race(Connection.TRANSACTION_READ_COMMITTED));
    }

    @Test
    @DisplayName("SERIALIZABLE allows only one car")
    void serializable_noDoubleBooking() throws Exception {
        assertEquals(1, race(Connection.TRANSACTION_SERIALIZABLE));
    }
}
