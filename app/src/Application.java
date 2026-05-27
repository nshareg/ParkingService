import main.com.parkingsystem.contract.ParkingRepository;
import main.com.parkingsystem.contract.ParkingService;
import main.com.parkingsystem.helpers.SlotType;
import main.com.parkingsystem.impl.ParkingRepositoryimpl;
import main.com.parkingsystem.impl.ParkingServiceImpl;
import main.com.parkingsystem.parkingPersistence.InMemoryConnection;
import main.com.parkingsystem.parkingPersistence.Storage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.*;

/*
    Created by anshanyan
    on 26.05.26
*/
public class Application {
    public static void main(String[] args) {
        Logger log = Logger.getLogger("parking-service-main");
        try {
            FileHandler fh = new FileHandler("logs.txt");
            fh.setFormatter(new SimpleFormatter());
            log.addHandler(fh);
            log.setUseParentHandlers(false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Storage.<Map<String, Object>>createTable("slots");
        Connection connection = new InMemoryConnection();
        ParkingRepository repository = new ParkingRepositoryimpl(connection);
        ParkingService parkingService = new ParkingServiceImpl(repository);

        try{
            var slot1 = parkingService.addSlot(SlotType.REGULAR);
            var slot2 = parkingService.addSlot(SlotType.ELECTRIC);
            var slot3 = parkingService.addSlot(SlotType.DISABLED);
            log.info("Added: " + slot1);
            log.info("Added: " + slot2);
            log.info("Added: " + slot3);

            parkingService.removeSlot(slot1.getSlotID());
            log.info("Removed slot1, count: " + parkingService.count());

            parkingService.park("AREG-1", SlotType.ELECTRIC);
            log.info("Booked slot2 with AREG-1");

            log.info("All slots:");
            parkingService.findAll().forEach(s -> log.info("  " + s));
        }catch (SQLException e){
            log.log(Level.SEVERE, e.getMessage());
        }
    }
}
