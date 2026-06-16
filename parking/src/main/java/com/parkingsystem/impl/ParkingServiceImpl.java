package com.parkingsystem.impl;

import com.parkingsystem.contract.ParkingRepository;
import com.parkingsystem.contract.ParkingService;
import com.parkingsystem.contract.ParkingSessionRepository;
import com.parkingsystem.entity.ParkingSession;
import com.parkingsystem.entity.ParkingSlot;
import com.parkingsystem.helpers.SlotType;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
    Created by anshanyan
    on 23.05.26
*/
@Service
@RequiredArgsConstructor
public class ParkingServiceImpl implements ParkingService {

    private final ParkingRepository repository;
    private final ParkingSessionRepository sessionRepository;

    @Override
    @Transactional
    public ParkingSlot addSlot(@NonNull SlotType type) {
        ParkingSlot slot = new ParkingSlot(type);
        repository.save(slot);
        return slot;
    }

    @Override
    @Transactional
    public Optional<ParkingSlot> removeSlot(@NonNull UUID id) {
        Optional<ParkingSlot> found = repository.findById(id);
        found.ifPresent(repository::delete);
        return found;
    }

    @Override
    @Transactional
    public Optional<ParkingSlot> park(@NonNull String numberPlate) {
        return park(numberPlate, SlotType.REGULAR);
    }

    @Override
    @Transactional
    public Optional<ParkingSlot> park(@NonNull String numberPlate, @NonNull SlotType slotType) {
        Optional<ParkingSlot> free = repository.findByBookedFalse().stream()
                .filter(slot -> slot.getType() == slotType)
                .findFirst();
        if (free.isEmpty() || repository.findByNumberPlate(numberPlate).isPresent()) {
            return Optional.empty();
        }
        ParkingSlot slot = free.get();
        slot.book(numberPlate);
        repository.save(slot);
        sessionRepository.save(new ParkingSession(slot.getSlotID(), numberPlate));
        return Optional.of(slot);
    }

    @Override
    @Transactional
    public Optional<ParkingSlot> release(@NonNull String numberPlate) {
        Optional<ParkingSlot> found = repository.findByNumberPlate(numberPlate);
        found.ifPresent(slot -> {
            slot.release();
            repository.save(slot);
            sessionRepository.findByActiveTrueAndNumberPlate(numberPlate).ifPresent(session -> {
                session.close();
                sessionRepository.save(session);
            });
        });
        return found;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSlot> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSlot> findAllFree() {
        return repository.findByBookedFalse();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSlot> findAllBooked() {
        return repository.findByBookedTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSlot> findByType(@NonNull SlotType type) {
        return repository.findByType(type);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ParkingSlot> findByNumberPlate(@NonNull String plate) {
        return repository.findByNumberPlate(plate);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countFree() {
        return repository.countByBookedFalse();
    }

    @Override
    @Transactional(readOnly = true)
    public long countBooked() {
        return repository.countByBookedTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSession> slotHistory(@NonNull UUID slotId) {
        return sessionRepository.findBySlotId(slotId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSession> plateHistory(@NonNull String numberPlate) {
        return sessionRepository.findByNumberPlate(numberPlate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSession> allSessions() {
        return sessionRepository.findAll();
    }
}
