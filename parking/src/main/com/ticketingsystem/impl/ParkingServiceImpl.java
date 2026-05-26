package main.com.ticketingsystem.impl;

import main.com.ticketingsystem.contract.ParkingRepository;
import main.com.ticketingsystem.contract.ParkingService;
import main.com.ticketingsystem.entity.ParkingSlot;
import main.com.ticketingsystem.helpers.SlotType;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
    Created by anshanyan
    on 23.05.26
*/
public class ParkingServiceImpl implements ParkingService {

    ParkingRepository repository;

    public ParkingServiceImpl(ParkingRepository repository) {
        this.repository = repository;
    }

    @Override
    public ParkingSlot addSlot(SlotType type) throws SQLException {
        ParkingSlot slot = new ParkingSlot(type);
        repository.add(slot);
        return slot;
    }

    @Override
    public Optional<ParkingSlot> removeSlot(UUID id) throws SQLException {
        return repository.remove(id);
    }

    @Override
    public Optional<ParkingSlot> park(String numberPlate) throws SQLException {
        Optional<ParkingSlot> free = repository.findAllFree().stream().findFirst();
        if (free.isEmpty()) return Optional.empty();
        ParkingSlot slot = free.get();
        slot.book(numberPlate);
        repository.update(slot);
        return Optional.of(slot);
    }

    @Override
    public Optional<ParkingSlot> release(String numberPlate) throws SQLException {
        Optional<ParkingSlot> found = repository.findByNumberPlate(numberPlate);
        if (found.isEmpty()) return Optional.empty();
        ParkingSlot slot = found.get();
        slot.release();
        repository.update(slot);
        return Optional.of(slot);
    }

    @Override
    public List<ParkingSlot> findAll() throws SQLException {
        return repository.findAll();
    }

    @Override
    public List<ParkingSlot> findAllFree() throws SQLException {
        return repository.findAllFree();
    }

    @Override
    public List<ParkingSlot> findAllBooked() throws SQLException {
        return repository.findAllBooked();
    }

    @Override
    public List<ParkingSlot> findByType(SlotType type) throws SQLException {
        return repository.findByType(type);
    }

    @Override
    public Optional<ParkingSlot> findByNumberPlate(String plate) throws SQLException {
        return repository.findByNumberPlate(plate);
    }

    @Override
    public int count() throws SQLException {
        return repository.count();
    }

    @Override
    public int countFree() throws SQLException {
        return repository.countFree();
    }

    @Override
    public int countBooked() throws SQLException {
        return repository.countBooked();
    }
}