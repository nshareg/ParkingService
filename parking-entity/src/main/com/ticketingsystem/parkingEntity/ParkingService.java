package main.com.ticketingsystem.parkingEntity;

import main.com.ticketingsystem.helpers.SlotType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
    Created by anshanyan
    on 23.05.26
*/
public class ParkingService {
    /**
     * interface of the repository implementation, can be both relational database, or in-memory solution
     */
    ParkingRepository repository;

    /**
     * Constructor initializing with the given repository
     * @param repository {@link ParkingRepository} that is used
     */
    public ParkingService(ParkingRepository repository){
        this.repository = repository;
    }

    /**
     * Adding new {@link ParkingSlot} entity to the repository, with generated {@link UUID}
     *
     * @param type {@link SlotType} type of the new slot
     * @return link to the created and added {@link ParkingSlot}
     */
    public ParkingSlot addSlot(SlotType type) {
        ParkingSlot slot = new ParkingSlot(type);
        repository.add(slot);
        return slot;
    }

    /**
     * Removing slot by its id
     * @param id {@link UUID} of the parking slot
     * @return {@link Optional} of removed key, empty if no such value exists
     */
    public Optional<ParkingSlot> removeSlot(UUID id) {
        return repository.remove(id);
    }

    /**
     * User functionality for parking, we first get empty spot and book the spot with the number plate
     *
     * @param numberPlate of the car that is gonna occupy the spot
     * @return {@link Optional} of occupied {@link ParkingSlot}
     */
    public Optional<ParkingSlot> park(String numberPlate) {
        Optional<ParkingSlot> free = repository.findAllFree().stream().findFirst();
        if (free.isEmpty()) return Optional.empty();
        ParkingSlot slot = free.get();
        slot.book(numberPlate);
        repository.update(slot);
        return Optional.of(slot);
    }
    /**
     * User functionality for releasing the spot by the number plate of the car
     *
     * @param numberPlate plate of the car that occupied the slot
     * @return {@link Optional} of released {@link ParkingSlot}
     */
    public Optional<ParkingSlot> release(String numberPlate) {
        Optional<ParkingSlot> found = repository.findByNumberPlate(numberPlate);
        if (found.isEmpty()) return Optional.empty();
        ParkingSlot slot = found.get();
        slot.release();
        repository.update(slot);
        return Optional.of(slot);
    }

    /**
     * @return {@link List} of all shallow copies of {@link ParkingSlot}
     */
    public List<ParkingSlot> findAll() {
        return repository.findAll();
    }
    /**
     * @return {@link List} of all shallow copies of {@link ParkingSlot} that are not occupied
     */
    public List<ParkingSlot> findAllFree() {
        return repository.findAllFree();
    }
    /**
     * @return {@link List} of all shallow copies of {@link ParkingSlot} that are occupied
     */
    public List<ParkingSlot> findAllBooked() {
        return repository.findAllBooked();
    }
    /**
     * @return {@link List} of all shallow copies of {@link ParkingSlot} by their {@link SlotType}
     */
    public List<ParkingSlot> findByType(SlotType type) {
        return repository.findByType(type);
    }
    /**
     * @param plate number plate of the car we are searching
     * @return {@link Optional} of parking slot, where provided numberplate is parked, empty if no such car is there
     */
    public Optional<ParkingSlot> findByNumberPlate(String plate) {
        return repository.findByNumberPlate(plate);
    }

    /**
     *
     * @return number of all {@link ParkingSlot}
     */
    public int count() {
        return repository.count();
    }
    /**
     *
     * @return number of all free {@link ParkingSlot}
     */
    public int countFree(){
        return repository.countFree();
    }
    /**
     *
     * @return number of all booked {@link ParkingSlot}
     */
    public int countBooked() {
        return repository.countBooked();
    }
}
