package com.parkingsystem.contract;

import com.parkingsystem.helpers.SlotType;
import com.parkingsystem.entity.ParkingSlot;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
    Created by anshanyan
    on 23.05.26
*/

public interface ParkingRepository extends Repository<ParkingSlot, UUID> {

    /**
     * Retrieving all free (not booked) {@link ParkingSlot} objects.
     * @return {@link List} of {@link ParkingSlot} entries that are not booked
     * @throws SQLException if the query fails
     */
    List<ParkingSlot> findAllFree() throws SQLException;

    /**
     * Retrieving all booked {@link ParkingSlot} objects.
     * @return {@link List} of {@link ParkingSlot} entries that are booked
     * @throws SQLException if the query fails
     */
    List<ParkingSlot> findAllBooked() throws SQLException;

    /**
     * Retrieving all {@link ParkingSlot} objects filtered by {@link SlotType}.
     * @param type the {@link SlotType} to filter by
     * @return {@link List} of {@link ParkingSlot} entries matching the given type
     * @throws SQLException if the query fails
     */
    List<ParkingSlot> findByType(SlotType type) throws SQLException;

    /**
     * Finding the {@link ParkingSlot} currently holding the given number plate.
     * @param numberPlate the number plate code to search for
     * @return {@link Optional} of {@link ParkingSlot}, empty if no entry with given number plate exists
     * @throws SQLException if the lookup fails
     */
    Optional<ParkingSlot> findByNumberPlate(String numberPlate) throws SQLException;

    /**
     * Counting all free (not booked) {@link ParkingSlot} entries.
     * @return number of entries that are not booked
     * @throws SQLException if the count fails
     */
    int countFree() throws SQLException;

    /**
     * Counting all booked {@link ParkingSlot} entries.
     * @return number of entries that are booked
     * @throws SQLException if the count fails
     */
    int countBooked() throws SQLException;
}
