package main.com.parkingsystem.contract;

import main.com.parkingsystem.helpers.SlotType;
import main.com.parkingsystem.entity.ParkingSlot;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
    Created by anshanyan
    on 23.05.26
*/

/**
 * Repository interface for managing {@link ParkingSlot} persistence
 */
public interface ParkingRepository {

    /**
     * Initializing the underlying storage: creating the {@code slots} table
     * and registering an index on the {@code slot_id} column for O(1) lookups
     * @throws SQLException if the table or index cannot be created
     */
    void init() throws SQLException;

    /**
     * Adding {@link ParkingSlot} object to the repository
     * @param parkingSlot the {@link ParkingSlot} to add
     * @throws SQLException if the slot cannot be added
     */
    void add(ParkingSlot parkingSlot) throws SQLException;

    /**
     * Removing {@link ParkingSlot} from the repository by ID
     * @param id the {@link UUID} of the parking slot to remove
     * @return {@link Optional} of removed {@link ParkingSlot}, empty if no entry with given ID exists
     * @throws SQLException if the removal fails
     */
    Optional<ParkingSlot> remove(UUID id) throws SQLException;

    /**
     * Updating {@link ParkingSlot} in the repository
     * @param slot the {@link ParkingSlot} to update
     * @throws SQLException if the update fails
     */
    void update(ParkingSlot slot) throws SQLException;

    /**
     * Finding {@link ParkingSlot} by its ID
     * @param id the {@link UUID} of the parking slot
     * @return {@link Optional} of {@link ParkingSlot}, empty if no entry with given ID exists
     * @throws SQLException if the lookup fails
     */
    Optional<ParkingSlot> findById(UUID id) throws SQLException;

    /**
     * Retrieving all {@link ParkingSlot} objects
     * @return {@link List} of all {@link ParkingSlot} entries
     * @throws SQLException if the query fails
     */
    List<ParkingSlot> findAll() throws SQLException;

    /**
     * Retrieving all free {@link ParkingSlot} objects
     * @return {@link List} of {@link ParkingSlot} entries that are not booked
     * @throws SQLException if the query fails
     */
    List<ParkingSlot> findAllFree() throws SQLException;

    /**
     * Retrieving all booked {@link ParkingSlot} objects
     * @return {@link List} of {@link ParkingSlot} entries that are booked
     * @throws SQLException if the query fails
     */
    List<ParkingSlot> findAllBooked() throws SQLException;

    /**
     * Retrieving all {@link ParkingSlot} objects filtered by {@link SlotType}
     * @param type the {@link SlotType} to filter by
     * @return {@link List} of {@link ParkingSlot} entries matching the given type
     * @throws SQLException if the query fails
     */
    List<ParkingSlot> findByType(SlotType type) throws SQLException;

    /**
     * Finding {@link ParkingSlot} by number plate
     * @param numberPlate the number plate code to search for
     * @return {@link Optional} of {@link ParkingSlot}, empty if no entry with given number plate exists
     * @throws SQLException if the lookup fails
     */
    Optional<ParkingSlot> findByNumberPlate(String numberPlate) throws SQLException;

    /**
     * Counting all {@link ParkingSlot} entries
     * @return total number of entries
     * @throws SQLException if the count fails
     */
    int count() throws SQLException;

    /**
     * Counting all free {@link ParkingSlot} entries
     * @return number of entries that are not booked
     * @throws SQLException if the count fails
     */
    int countFree() throws SQLException;

    /**
     * Counting all booked {@link ParkingSlot} entries
     * @return number of entries that are booked
     * @throws SQLException if the count fails
     */
    int countBooked() throws SQLException;
}