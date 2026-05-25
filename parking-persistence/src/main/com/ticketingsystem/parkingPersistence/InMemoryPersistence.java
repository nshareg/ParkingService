package main.com.ticketingsystem.parkingPersistence;

import main.com.ticketingsystem.parkingEntity.ParkingRepository;
import main.com.ticketingsystem.parkingEntity.ParkingSlot;
import main.com.ticketingsystem.helpers.SlotType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
    Created by anshanyan
    on 23.05.26
*/

/**
 * implements {@link ParkingRepository} as ordinary in-memory solution, storing the information in two hashmaps
 */
public class InMemoryPersistence implements ParkingRepository {
    /**
     * custom map
     * {@link UUID} for the parking slot -> {@link ParkingSlot} object
     */
    private final Index<UUID, ParkingSlot> slots;
    /**
     * Mapping number plate code -> {@link UUID} for the parking slot
     * <p>for the number plate fast lookup </p>
     *
     */
    private final Index<String, UUID> plateToIDIndex;   // numberPlate -> slotId

    /**
     * Constructor initializing both tables
     */
    public InMemoryPersistence() {
        slots = Index.createIndex();
        plateToIDIndex = Index.createIndex();
    }

    /**
     * Adding {@link ParkingSlot} object to main table, and if it is booked, to the complimentary also
     * @param parkingSlot
     */
    @Override
    public void add(ParkingSlot parkingSlot) {
        if(parkingSlot == null){
            throw new IllegalArgumentException("null parking slot provided");
        }
        slots.put(parkingSlot.getSlotID(), parkingSlot);
        if (parkingSlot.isBooked()) {
            plateToIDIndex.put(parkingSlot.getNumberPlate(), parkingSlot.getSlotID());
        }
    }

    /**
     * Removing {@link ParkingSlot} from both tables by ID
     *
     * @param id of {@link ParkingSlot} we are trying to delete
     * @return {@link Optional} of {@link ParkingSlot}, empty if no {@link ParkingSlot} with given ID exists
     */
    @Override
    public Optional<ParkingSlot> remove(UUID id) {
        Optional<ParkingSlot> found = slots.lookup(id);
        found.ifPresent(
                slot -> {
            if (slot.getNumberPlate() != null) {
                plateToIDIndex.remove(slot.getNumberPlate());
            }
            slots.delete(id);
        });
        return found;
    }

    /**
     * Updating {@link ParkingSlot} in main table, and syncing the complimentary based on booking state
     * @param slot the {@link ParkingSlot} to update
     */
    @Override
    public void update(ParkingSlot slot) {
        if (slot.isBooked()) {
            plateToIDIndex.put(slot.getNumberPlate(), slot.getSlotID());
        } else {
            plateToIDIndex.entrySet().removeIf(e -> e.getValue().equals(slot.getSlotID()));
        }
        slots.update(slot.getSlotID(), slot);
    }

    /**
     * Finding {@link ParkingSlot} in main table by its id
     *
     * @param id of {@link ParkingSlot}
     * @return {@link Optional} of {@link ParkingSlot}, empty if no entry with given ID exists
     */
    @Override
    public Optional<ParkingSlot> findById(UUID id) {
        return Optional.of(slots.get(id));
    }

    /**
     * Retrieving all {@link ParkingSlot} objects from main table
     * @return {@link List} of all {@link ParkingSlot} entries
     */
    public List<ParkingSlot> findAll() {
        return slots.selectAll();
    }

    /**
     * Retrieving all free {@link ParkingSlot} objects from main table
     * @return {@link List} of {@link ParkingSlot} entries that are not booked
     */
    @Override
    public List<ParkingSlot> findAllFree() {
        return slots.scanValueSet(
                x -> !x.isBooked()
        );
    }

    /**
     * Retrieving all booked {@link ParkingSlot} objects from main table
     * @return {@link List} of {@link ParkingSlot} entries that are booked
     */
    @Override
    public List<ParkingSlot> findAllBooked() {
        return slots.scanValueSet(ParkingSlot::isBooked);
    }

    /**
     * Retrieving all {@link ParkingSlot} objects from main table filtered by {@link SlotType}
     * @param type the {@link SlotType} to filter by
     * @return {@link List} of {@link ParkingSlot} entries matching the given type
     */
    @Override
    public List<ParkingSlot> findByType(SlotType type) {
        return slots.scanValueSet(
                x -> x.getType().equals(type)
        );
    }

    /**
     * Finding {@link ParkingSlot} in complimentary table by number plate, then looking up in main table
     * @param numberPlate the number plate code to search for
     * @return {@link Optional} of {@link ParkingSlot}, empty if no entry with given number plate exists
     */
    @Override
    public Optional<ParkingSlot> findByNumberPlate(String numberPlate) {
        return plateToIDIndex.lookup(numberPlate).flatMap(slots::lookup);
    }

    /**
     * Counting all {@link ParkingSlot} entries in main table
     * @return total number of entries
     */
    @Override
    public int count() {
        return slots.size();
    }

    /**
     * Counting all free {@link ParkingSlot} entries in main table
     * @return number of entries that are not booked
     */
    @Override
    public int countFree() {
        return findAllFree().size();
    }

    /**
     * Counting all booked {@link ParkingSlot} entries in main table
     * @return number of entries that are booked
     */
    @Override
    public int countBooked() {
        return findAllBooked().size();
    }
}
