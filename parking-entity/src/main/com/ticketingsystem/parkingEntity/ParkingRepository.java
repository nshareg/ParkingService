package main.com.ticketingsystem.parkingEntity;

import main.com.ticketingsystem.helpers.SlotType;

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
     * Adding {@link ParkingSlot} object to the repository
     * @param parkingSlot the {@link ParkingSlot} to add
     */
    void add(ParkingSlot parkingSlot);

    /**
     * Removing {@link ParkingSlot} from the repository by ID
     * @param id the {@link UUID} of the parking slot to remove
     * @return {@link Optional} of removed {@link ParkingSlot}, empty if no entry with given ID exists
     */
    Optional<ParkingSlot> remove(UUID id);

    /**
     * Updating {@link ParkingSlot} in the repository
     * @param slot the {@link ParkingSlot} to update
     */
    void update(ParkingSlot slot);

    /**
     * Finding {@link ParkingSlot} by its ID
     * @param id the {@link UUID} of the parking slot
     * @return {@link Optional} of {@link ParkingSlot}, empty if no entry with given ID exists
     */
    Optional<ParkingSlot> findById(UUID id);

    /**
     * Retrieving all {@link ParkingSlot} objects
     * @return {@link List} of all {@link ParkingSlot} entries
     */
    List<ParkingSlot> findAll();

    /**
     * Retrieving all free {@link ParkingSlot} objects
     * @return {@link List} of {@link ParkingSlot} entries that are not booked
     */
    List<ParkingSlot> findAllFree();

    /**
     * Retrieving all booked {@link ParkingSlot} objects
     * @return {@link List} of {@link ParkingSlot} entries that are booked
     */
    List<ParkingSlot> findAllBooked();

    /**
     * Retrieving all {@link ParkingSlot} objects filtered by {@link SlotType}
     * @param type the {@link SlotType} to filter by
     * @return {@link List} of {@link ParkingSlot} entries matching the given type
     */
    List<ParkingSlot> findByType(SlotType type);

    /**
     * Finding {@link ParkingSlot} by number plate
     * @param numberPlate the number plate code to search for
     * @return {@link Optional} of {@link ParkingSlot}, empty if no entry with given number plate exists
     */
    Optional<ParkingSlot> findByNumberPlate(String numberPlate);

    /**
     * Counting all {@link ParkingSlot} entries
     * @return total number of entries
     */
    int count();

    /**
     * Counting all free {@link ParkingSlot} entries
     * @return number of entries that are not booked
     */
    int countFree();

    /**
     * Counting all booked {@link ParkingSlot} entries
     * @return number of entries that are booked
     */
    int countBooked();
}
