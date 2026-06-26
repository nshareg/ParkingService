package com.parkingsystem.contract;

import com.parkingsystem.entity.ParkingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
    Created by anshanyan
    on 08.06.26
*/
@Repository
public interface ParkingSessionRepository extends JpaRepository<ParkingSession, UUID> {

    /** All sessions ever recorded for a slot — every plate that has used it. */
    @Query("SELECT s FROM ParkingSession s WHERE s.slot.slotID = :slotId")
    List<ParkingSession> findBySlotId(@Param("slotId") UUID slotId);

    /** All sessions ever recorded for a plate — every slot it has used. */
    List<ParkingSession> findByNumberPlate(String numberPlate);

    /** The single currently-open session for a plate, if it is parked right now. */
    Optional<ParkingSession> findByActiveTrueAndNumberPlate(String numberPlate);
}
