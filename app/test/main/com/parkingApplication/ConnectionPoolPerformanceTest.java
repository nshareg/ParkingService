package main.com.parkingApplication;

import main.com.parkingsystem.contract.ParkingService;
import main.com.parkingsystem.entity.ParkingSlot;
import main.com.parkingsystem.helpers.SlotType;
import main.com.parkingsystem.impl.ParkingRepositoryimpl;
import main.com.parkingsystem.impl.ParkingServiceImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectionPoolPerformanceTest {
    private static final int  REQUESTS = 2000;
    private static final long MAX_WALL_CLOCK_MS = 15_000;

    @Test
    @DisplayName("2000 concurrent DB requests all succeed within 15s")
    void concurrent2000Requests_allSucceed_withinThreshold() throws Exception {
        final ExecutorService pool = Executors.newFixedThreadPool(REQUESTS);
        final CountDownLatch overallDone = new CountDownLatch(REQUESTS);
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done  = new CountDownLatch(REQUESTS);

        final AtomicInteger success = new AtomicInteger();
        final AtomicInteger failure = new AtomicInteger();
        final ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < REQUESTS; i++) {
            pool.submit(() -> {
                overallDone.countDown();
                try {
                    start.await();
                    try (Connection conn = Database.getConnection()) {
                        ParkingService service =
                                new ParkingServiceImpl(conn, new ParkingRepositoryimpl(conn));
                        ParkingSlot slot = service.addSlot(SlotType.REGULAR);
                        service.removeSlot(slot.getSlotID());
                    }
                    success.incrementAndGet();
                } catch (Throwable t) {
                    failure.incrementAndGet();
                    errors.add(t);
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(overallDone.await(30, TimeUnit.SECONDS), "worker threads failed to start in time");
        final long t0 = System.nanoTime();
        start.countDown();
        final boolean finished = done.await(60, TimeUnit.SECONDS);
        final long wallMs = (System.nanoTime() - t0) / 1_000_000;
        pool.shutdownNow();

        final Throwable sample = errors.peek();

        System.out.println("================ CONNECTION POOL PERF ================");
        System.out.println("requests       : " + REQUESTS);
        System.out.println("succeeded      : " + success.get());
        System.out.println("failed         : " + failure.get());
        System.out.println("wall-clock     : " + wallMs + " ms");
        System.out.println("all finished   : " + finished);
        System.out.println("errors         : " + errors.size());
        if (sample != null) {
            System.out.println("sample error   : " + sample.getClass().getName() + ": " + sample.getMessage());
        }
        System.out.println("=====================================================");

        assertTrue(finished, "Not all requests completed within 60s (latch did not reach zero)");
        assertEquals(0, failure.get(),
                "Expected zero failed requests, but " + failure.get() + " failed (sample: "
                        + (sample == null ? "none" : sample) + ")");
        assertEquals(REQUESTS, success.get(), "All " + REQUESTS + " requests must succeed");
        assertTrue(wallMs < MAX_WALL_CLOCK_MS,
                "Expected the burst to finish in under " + MAX_WALL_CLOCK_MS + " ms, but it took " + wallMs + " ms");
    }

}
