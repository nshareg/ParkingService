package com.parkingsystem.contract;

import com.parkingsystem.helpers.SlotType;
import com.parkingsystem.entity.ParkingSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
    Created by anshanyan
    on 23.05.26
*/
@Repository
public interface ParkingRepository extends JpaRepository<ParkingSlot, UUID> {

    /** All free (not booked) slots. */
    @Query("SELECT DISTINCT p FROM ParkingSlot p LEFT JOIN FETCH p.sessions WHERE p.booked = false")
    List<ParkingSlot> findByBookedFalse();

    /** All booked slots. */
    @Query("SELECT DISTINCT p FROM ParkingSlot p LEFT JOIN FETCH p.sessions WHERE p.booked = true")
    List<ParkingSlot> findByBookedTrue();

    /** All slots of the given {@link SlotType}. */
    @Query("SELECT DISTINCT p FROM ParkingSlot p LEFT JOIN FETCH p.sessions WHERE p.type = :type")
    List<ParkingSlot> findByType(SlotType type);

    /** The slot currently holding the given number plate, if any. */
    Optional<ParkingSlot> findByNumberPlate(String numberPlate);

    /** Count of free (not booked) slots. */
    long countByBookedFalse();

    /** Count of booked slots. */
    long countByBookedTrue();

    /**
     * Method for getting first free spot with pessimistic locking, so the system will hold the invariant during the race conditions
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ParkingSlot> findFirstByTypeAndBookedFalse(SlotType type);
}
