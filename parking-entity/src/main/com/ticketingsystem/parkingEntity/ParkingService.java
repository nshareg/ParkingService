package main.com.ticketingsystem.parkingEntity;

import main.com.ticketingsystem.helpers.SlotType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
    Created by anshanyan
    on 23.05.26
*/
public interface ParkingService {

    ParkingSlot addSlot(SlotType type);

    Optional<ParkingSlot> removeSlot(UUID id);

    Optional<ParkingSlot> park(String numberPlate);

    Optional<ParkingSlot> release(String numberPlate);

    List<ParkingSlot> findAll();

    List<ParkingSlot> findAllFree();

    List<ParkingSlot> findAllBooked();

    List<ParkingSlot> findByType(SlotType type);

    Optional<ParkingSlot> findByNumberPlate(String plate);

    int count();

    int countFree();

    int countBooked();
}
