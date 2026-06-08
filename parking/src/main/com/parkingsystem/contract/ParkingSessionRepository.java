package main.com.parkingsystem.contract;

import main.com.parkingsystem.entity.ParkingSession;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
    Created by anshanyan
    on 08.06.26
*/

public interface ParkingSessionRepository extends Repository<ParkingSession, UUID> {

    /** All sessions ever recorded for a slot — every plate that has used it. */
    List<ParkingSession> findBySlot(UUID slotId) throws SQLException;

    /** All sessions ever recorded for a plate — every slot it has used. */
    List<ParkingSession> findByNumberPlate(String numberPlate) throws SQLException;

    /** The single currently-open session for a plate, if it is parked right now. */
    Optional<ParkingSession> findActiveByNumberPlate(String numberPlate) throws SQLException;
}
