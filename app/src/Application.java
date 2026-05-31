import lombok.extern.slf4j.Slf4j;
import main.com.parkingsystem.contract.ParkingRepository;
import main.com.parkingsystem.contract.ParkingService;
import main.com.parkingsystem.helpers.SlotType;
import main.com.parkingsystem.impl.ParkingRepositoryimpl;
import main.com.parkingsystem.impl.ParkingServiceImpl;
import main.com.inMemoryPersistence.InMemoryConnection;


import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/*
    Created by anshanyan
    on 26.05.26
*/
@Slf4j
public class Application {
    public static void main(String[] args) throws InterruptedException {
        Connection connection = new InMemoryConnection();
        ParkingRepository repository = new ParkingRepositoryimpl(connection);
        ParkingService parkingService = new ParkingServiceImpl(repository);

        try {
            parkingService.init();

            var regular1 = parkingService.addSlot(SlotType.REGULAR);
            var regular2 = parkingService.addSlot(SlotType.REGULAR);
            var electric1 = parkingService.addSlot(SlotType.ELECTRIC);
            var disabled1 = parkingService.addSlot(SlotType.DISABLED);
            log.info("Added slots: " + regular1.getSlotID() + ", " + regular2.getSlotID()
                    + ", " + electric1.getSlotID() + ", " + disabled1.getSlotID());
            log.info("Total slots: " + parkingService.count()
                    + " (free=" + parkingService.countFree()
                    + ", booked=" + parkingService.countBooked() + ")");

            Optional<?> parked = parkingService.park("AREG-1", SlotType.ELECTRIC);
            log.info("Parked AREG-1 in ELECTRIC: " + parked);
            log.info("Lookup by plate AREG-1: " + parkingService.findByNumberPlate("AREG-1"));

            parkingService.release("AREG-1");
            log.info("Released AREG-1, booked count: " + parkingService.countBooked());

            parkingService.park("OLD-PLATE", SlotType.REGULAR);
            log.info("Parked OLD-PLATE.");
            parkingService.release("OLD-PLATE");
            parkingService.park("NEW-PLATE", SlotType.REGULAR);
            log.info("Released OLD-PLATE, re-parked as NEW-PLATE.");

            log.info("Lookup OLD-PLATE (expected empty): " + parkingService.findByNumberPlate("OLD-PLATE"));
            log.info("Lookup NEW-PLATE (expected present): " + parkingService.findByNumberPlate("NEW-PLATE"));
            if (parkingService.findByNumberPlate("OLD-PLATE").isPresent()
                    || parkingService.findByNumberPlate("NEW-PLATE").isEmpty()) {
                log.error("INDEX RE-KEY FAILED: stale plate still resolves, or new plate is missing.");
            } else {
                log.info("Index re-key OK: plate lookups reflect the latest update.");
            }
            parkingService.release("NEW-PLATE");
            log.info("Released NEW-PLATE, booked count: " + parkingService.countBooked());

            parkingService.removeSlot(regular2.getSlotID());
            log.info("Removed regular2, total: " + parkingService.count());

            final AtomicInteger successes = new AtomicInteger();

            Runnable racer = () -> {
                String plate = Thread.currentThread().getName();
                try {
                    Optional<?> result = parkingService.park(plate, SlotType.ELECTRIC);
                    if (result.isPresent()) {
                        successes.incrementAndGet();
                        log.info(plate + " WON the slot: " + result.get());
                    } else {
                        log.info(plate + " lost the race (no free ELECTRIC slot).");
                    }
                } catch (Exception e) {
                    log.error(plate + " failed: " + e.getMessage());
                }
            };
            Thread t1 = new Thread(racer, "PLATE-A");
            Thread t2 = new Thread(racer, "PLATE-B");
            t1.start();
            t2.start();
            t1.join();
            t2.join();

            log.info("Concurrent bookings that succeeded: " + successes.get() + " (expected 1)");
            if (successes.get() != 1) {
                log.error("expected exactly 1 booking, got " + successes.get());
            } else {
                log.info("exactly one plate booked the single ELECTRIC slot.");
            }

            log.info("Final state (booked=" + parkingService.countBooked() + ", free=" + parkingService.countFree() + "):");
            parkingService.findAll().forEach(s -> log.info("  " + s));
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }
}
