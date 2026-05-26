import main.com.parkingsystem.contract.ParkingRepository;
import main.com.parkingsystem.contract.ParkingService;
import main.com.parkingsystem.entity.ParkingSlot;
import main.com.parkingsystem.helpers.SlotType;
import main.com.parkingsystem.impl.ParkingRepositoryimpl;
import main.com.parkingsystem.impl.ParkingServiceImpl;
import main.com.parkingsystem.parkingPersistence.InMemoryConnection;
import main.com.parkingsystem.parkingPersistence.Index;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
    Created by anshanyan
    on 26.05.26
*/
public class Application {
    public static void main(String[] args) {
        Logger log = Logger.getLogger("parking-service-main");

        Index<UUID, ParkingSlot> mainIndex = new Index<>();
        Index<String, UUID> secondary = new Index<>(); //do i need to do the checks?

        Connection connection = new InMemoryConnection(mainIndex,secondary);
        ParkingRepository repository = new ParkingRepositoryimpl(connection);
        ParkingService parkingService = new ParkingServiceImpl(repository);

        try{
            parkingService.addSlot(SlotType.REGULAR);
            parkingService.park("ansh8880");
            parkingService.release("ansh8880");
        }catch (SQLException e){
            log.log(Level.SEVERE, e.getMessage());
        }
    }
}
