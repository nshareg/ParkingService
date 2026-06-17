package com.parkingsystem.contract;

import com.parkingsystem.helpers.SlotType;
import com.parkingsystem.entity.ParkingSlot;
import com.parkingsystem.entity.ParkingSession;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
    Created by anshanyan
    on 23.05.26
*/
public interface ParkingService {

    void init() throws SQLException;

    ParkingSlot addSlot(SlotType type) throws SQLException;

    Optional<ParkingSlot> removeSlot(UUID id) throws SQLException;

    Optional<ParkingSlot> park(String numberPlate) throws SQLException;

    Optional<ParkingSlot> park(String numberPlate, SlotType slotType) throws SQLException;

    Optional<ParkingSlot> release(String numberPlate) throws SQLException;

    List<ParkingSlot> findAll() throws SQLException;

    List<ParkingSlot> findAllFree() throws SQLException;

    List<ParkingSlot> findAllBooked() throws SQLException;

    List<ParkingSlot> findByType(SlotType type) throws SQLException;

    Optional<ParkingSlot> findByNumberPlate(String plate) throws SQLException;

    int count() throws SQLException;

    int countFree() throws SQLException;

    int countBooked() throws SQLException;

    List<ParkingSession> slotHistory(UUID slotId) throws SQLException;

    List<ParkingSession> plateHistory(String numberPlate) throws SQLException;

    List<ParkingSession> allSessions() throws SQLException;
}