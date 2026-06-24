package com.parkingsystem.contract;

import com.parkingsystem.helpers.SlotType;
import com.parkingsystem.entity.ParkingSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
    Created by anshanyan
    on 23.05.26
*/
public interface ParkingRepository extends JpaRepository<ParkingSlot, UUID> {

    /** All free (not booked) slots. */
    List<ParkingSlot> findByBookedFalse();

    /** All booked slots. */
    List<ParkingSlot> findByBookedTrue();

    /** All slots of the given {@link SlotType}. */
    List<ParkingSlot> findByType(SlotType type);

    /** The slot currently holding the given number plate, if any. */
    Optional<ParkingSlot> findByNumberPlate(String numberPlate);

    /** Count of free (not booked) slots. */
    long countByBookedFalse();

    /** Count of booked slots. */
    long countByBookedTrue();
}
