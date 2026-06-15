package main.com.parkingApplication.acid;

import main.com.parkingsystem.entity.ParkingSession;
import main.com.parkingsystem.entity.ParkingSlot;
import main.com.parkingsystem.helpers.SlotType;
import main.com.parkingsystem.impl.ParkingRepositoryimpl;
import main.com.parkingsystem.impl.ParkingSessionRepositoryimpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IsolationLevelTest {

    private static final int TIMEOUT_SECONDS = 10;

    private boolean tryPark(int isolation, String plate, CountDownLatch raceLatch, CountDownLatch doneLatch) {
        try (Connection c = AcidTestDB.openConnection()) {
            c.setTransactionIsolation(isolation);
            c.setAutoCommit(false);

            ParkingRepositoryimpl slots = new ParkingRepositoryimpl(c);
            List<ParkingSlot> free = slots.findAllFree();

            raceLatch.countDown();
            if (!raceLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                c.rollback();
                throw new IllegalStateException("race gate timed out — other thread likely crashed");
            }

            if (free.isEmpty()) { c.rollback(); return false; }

            ParkingSlot slot = free.get(0);
            slot.book(plate);
            slots.update(slot);
            new ParkingSessionRepositoryimpl(c).add(new ParkingSession(slot.getSlotID(), plate));
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
            new ParkingRepositoryimpl(c).add(new ParkingSlot(SlotType.REGULAR));
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