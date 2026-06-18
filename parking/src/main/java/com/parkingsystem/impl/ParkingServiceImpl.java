package com.parkingsystem.impl;

import lombok.NonNull;
import com.parkingsystem.contract.ParkingRepository;
import com.parkingsystem.contract.ParkingService;
import com.parkingsystem.contract.ParkingSessionRepository;
import com.parkingsystem.entity.ParkingSession;
import com.parkingsystem.entity.ParkingSlot;
import com.parkingsystem.helpers.SlotType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
    Created by anshanyan
    on 23.05.26
*/
@RequiredArgsConstructor
public class ParkingServiceImpl implements ParkingService {

    private final ParkingRepository repository;
    private final ParkingSessionRepository sessionRepository;

    @Override
    @Transactional(rollbackFor = SQLException.class)
    public void init() throws SQLException {
        repository.init();
        if (sessionRepository != null) {
            sessionRepository.init();
        }
    }

    @Override
    @Transactional(rollbackFor = SQLException.class)
    public ParkingSlot addSlot(@NonNull SlotType type) throws SQLException {
        ParkingSlot slot = new ParkingSlot(type);
        repository.add(slot);
        return slot;
    }

    @Override
    @Transactional(rollbackFor = SQLException.class)
    public Optional<ParkingSlot> removeSlot(@NonNull UUID id) throws SQLException {
        return repository.remove(id);
    }

    @Override
    @Transactional(rollbackFor = SQLException.class)
    public Optional<ParkingSlot> park(@NonNull String numberPlate) throws SQLException {
        return park(numberPlate, SlotType.REGULAR);
    }

    @Override
    @Transactional(rollbackFor = SQLException.class)
    public Optional<ParkingSlot> park(@NonNull String numberPlate, @NonNull SlotType slotType) throws SQLException {
        Optional<ParkingSlot> free = repository.findAllFree().stream()
                .filter(x -> x.getType().equals(slotType))
                .findFirst();
        if (free.isEmpty() || repository.findByNumberPlate(numberPlate).isPresent()) return Optional.empty();
        ParkingSlot slot = free.get();
        slot.book(numberPlate);
        repository.update(slot);
        if (sessionRepository != null) {
            sessionRepository.add(new ParkingSession(slot.getSlotID(), numberPlate));
        }
        return Optional.of(slot);
    }

    @Override
    @Transactional(rollbackFor = SQLException.class)
    public Optional<ParkingSlot> release(@NonNull String numberPlate) throws SQLException {
        Optional<ParkingSlot> found = repository.findByNumberPlate(numberPlate);
        if (found.isEmpty()) return Optional.empty();
        ParkingSlot slot = found.get();
        slot.release();
        repository.update(slot);
        if (sessionRepository != null) {
            Optional<ParkingSession> open = sessionRepository.findActiveByNumberPlate(numberPlate);
            if (open.isPresent()) {
                ParkingSession session = open.get();
                session.close();
                sessionRepository.update(session);
            }
        }
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
    public List<ParkingSlot> findByType(@NonNull SlotType type) throws SQLException {
        return repository.findByType(type);
    }

    @Override
    public Optional<ParkingSlot> findByNumberPlate(@NonNull String plate) throws SQLException {
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

    @Override
    public List<ParkingSession> slotHistory(@NonNull UUID slotId) throws SQLException {
        return sessionRepository == null ? List.of() : sessionRepository.findBySlot(slotId);
    }

    @Override
    public List<ParkingSession> plateHistory(@NonNull String numberPlate) throws SQLException {
        return sessionRepository == null ? List.of() : sessionRepository.findByNumberPlate(numberPlate);
    }

    @Override
    public List<ParkingSession> allSessions() throws SQLException {
        return sessionRepository == null ? List.of() : sessionRepository.findAll();
    }
}
