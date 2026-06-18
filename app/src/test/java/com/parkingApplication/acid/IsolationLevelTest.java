package com.parkingApplication.acid;

import com.parkingsystem.entity.ParkingSession;
import com.parkingsystem.entity.ParkingSlot;
import com.parkingsystem.helpers.SlotType;
import com.parkingsystem.impl.ParkingRepositoryimpl;
import com.parkingsystem.impl.ParkingSessionRepositoryimpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;
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

            // Repositories borrow their connection from a DataSource; wrap this thread's
            // connection (close-suppressed) so the manually-set isolation + commit/rollback apply.
            DataSource ds = new SingleConnectionDataSource(c, true);
            ParkingRepositoryimpl slots = new ParkingRepositoryimpl(ds);
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
            new ParkingSessionRepositoryimpl(ds).add(new ParkingSession(slot.getSlotID(), plate));
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
            new ParkingRepositoryimpl(new SingleConnectionDataSource(c, true))
                    .add(new ParkingSlot(SlotType.REGULAR));
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