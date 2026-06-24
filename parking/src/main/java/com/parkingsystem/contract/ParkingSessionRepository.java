package com.parkingsystem.contract;

import com.parkingsystem.entity.ParkingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
    Created by anshanyan
    on 08.06.26
*/
public interface ParkingSessionRepository extends JpaRepository<ParkingSession, UUID> {

    /** All sessions ever recorded for a slot — every plate that has used it. */
    List<ParkingSession> findBySlotId(UUID slotId);

    /** All sessions ever recorded for a plate — every slot it has used. */
    List<ParkingSession> findByNumberPlate(String numberPlate);

    /** The single currently-open session for a plate, if it is parked right now. */
    Optional<ParkingSession> findByActiveTrueAndNumberPlate(String numberPlate);
}
