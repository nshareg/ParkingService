package main.com.parkingsystem.impl;

import main.com.parkingsystem.contract.ParkingRepository;
import main.com.parkingsystem.contract.ParkingService;
import main.com.parkingsystem.entity.ParkingSlot;
import main.com.parkingsystem.helpers.SlotType;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*
    Created by anshanyan
    on 23.05.26
*/
public class ParkingServiceImpl implements ParkingService {

    private final ParkingRepository repository;

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    public ParkingServiceImpl(ParkingRepository repository) {
        this.repository = repository;
    }

    @Override
    public void init() throws SQLException {
        writeLock.lock();
        try {
            repository.init();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public ParkingSlot addSlot(SlotType type) throws SQLException {
        writeLock.lock();
        try {
            ParkingSlot slot = new ParkingSlot(type);
            repository.add(slot);
            return slot;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Optional<ParkingSlot> removeSlot(UUID id) throws SQLException {
        writeLock.lock();
        try {
            return repository.remove(id);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Optional<ParkingSlot> park(String numberPlate) throws SQLException {
        return park(numberPlate, SlotType.REGULAR);
    }

    @Override
    public Optional<ParkingSlot> park(String numberPlate, SlotType slotType) throws SQLException {
        writeLock.lock();
        try {
            Optional<ParkingSlot> free = repository.findAllFree().stream()
                    .filter(x -> x.getType().equals(slotType))
                    .findFirst();
            if (free.isEmpty()) return Optional.empty();
            ParkingSlot slot = free.get();
            slot.book(numberPlate);
            repository.update(slot);
            return Optional.of(slot);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Optional<ParkingSlot> release(String numberPlate) throws SQLException {
        writeLock.lock();
        try {
            Optional<ParkingSlot> found = repository.findByNumberPlate(numberPlate);
            if (found.isEmpty()) return Optional.empty();
            ParkingSlot slot = found.get();
            slot.release();
            repository.update(slot);
            return Optional.of(slot);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public List<ParkingSlot> findAll() throws SQLException {
        readLock.lock();
        try {
            return repository.findAll();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<ParkingSlot> findAllFree() throws SQLException {
        readLock.lock();
        try {
            return repository.findAllFree();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<ParkingSlot> findAllBooked() throws SQLException {
        readLock.lock();
        try {
            return repository.findAllBooked();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<ParkingSlot> findByType(SlotType type) throws SQLException {
        readLock.lock();
        try {
            return repository.findByType(type);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Optional<ParkingSlot> findByNumberPlate(String plate) throws SQLException {
        readLock.lock();
        try {
            return repository.findByNumberPlate(plate);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int count() throws SQLException {
        readLock.lock();
        try {
            return repository.count();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int countFree() throws SQLException {
        readLock.lock();
        try {
            return repository.countFree();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int countBooked() throws SQLException {
        readLock.lock();
        try {
            return repository.countBooked();
        } finally {
            readLock.unlock();
        }
    }
}