package com.parkingsystem.contract;

import com.parkingsystem.helpers.SlotType;
import com.parkingsystem.entity.ParkingSlot;
import com.parkingsystem.entity.ParkingSession;

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

    Optional<ParkingSlot> park(String numberPlate, SlotType slotType);

    Optional<ParkingSlot> release(String numberPlate);

    List<ParkingSlot> findAll();

    List<ParkingSlot> findAllFree();

    List<ParkingSlot> findAllBooked();

    List<ParkingSlot> findByType(SlotType type);

    Optional<ParkingSlot> findByNumberPlate(String plate);

    long count();

    long countFree();

    long countBooked();

    List<ParkingSession> slotHistory(UUID slotId);

    List<ParkingSession> plateHistory(String numberPlate);

    List<ParkingSession> allSessions();
}