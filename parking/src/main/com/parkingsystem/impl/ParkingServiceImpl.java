package main.com.parkingsystem.impl;

import lombok.NonNull;
import main.com.parkingsystem.contract.ParkingRepository;
import main.com.parkingsystem.contract.ParkingService;
import main.com.parkingsystem.contract.ParkingSessionRepository;
import main.com.parkingsystem.entity.ParkingSession;
import main.com.parkingsystem.entity.ParkingSlot;
import main.com.parkingsystem.helpers.SlotType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
    Created by anshanyan
    on 23.05.26
*/
public class ParkingServiceImpl implements ParkingService {

    private final ParkingRepository repository;
    //junction table
    private final ParkingSessionRepository sessionRepository;

    private final Connection connection;

    /** Action over the repositories that may fail with a {@link SQLException}. */
    @FunctionalInterface
    private interface SqlAction<T> {
        T run() throws SQLException;
    }

    public ParkingServiceImpl(ParkingRepository repository) {
        this(null, repository, null);
    }

    public ParkingServiceImpl(ParkingRepository repository, ParkingSessionRepository sessionRepository) {
        this(null, repository, sessionRepository);
    }

    public ParkingServiceImpl(Connection connection, ParkingRepository repository) {
        this(connection, repository, null);
    }

    public ParkingServiceImpl(Connection connection,
                              ParkingRepository repository,
                              ParkingSessionRepository sessionRepository) {
        this.connection = connection;
        this.repository = repository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Runs {@code action} as a single atomic transaction: everything commits together or, on any
     * failure, nothing does (rollback). When there is no usable connection the action just runs
     * as-is (no-op transaction), so mock and in-memory callers behave exactly as before.
     */
    private <T> T inTransaction(SqlAction<T> action) throws SQLException {
        if (connection == null) {
            return action.run();
        }
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            T result = action.run();
            connection.commit();
            return result;
        } catch (SQLException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    @Override
    public void init() throws SQLException {
        inTransaction(() -> {
            repository.init();
            if (sessionRepository != null) {
                sessionRepository.init();
            }
            return null;
        });
    }

    @Override
    public ParkingSlot addSlot(@NonNull SlotType type) throws SQLException {
        return inTransaction(() -> {
            ParkingSlot slot = new ParkingSlot(type);
            repository.add(slot);
            return slot;
        });
    }

    @Override
    public Optional<ParkingSlot> removeSlot(@NonNull UUID id) throws SQLException {
        return inTransaction(() -> repository.remove(id));
    }

    @Override
    public Optional<ParkingSlot> park(@NonNull String numberPlate) throws SQLException {
        return park(numberPlate, SlotType.REGULAR);
    }

    @Override
    public Optional<ParkingSlot> park(@NonNull String numberPlate, @NonNull SlotType slotType) throws SQLException {
        return inTransaction(() -> {
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
        });
    }

    @Override
    public Optional<ParkingSlot> release(@NonNull String numberPlate) throws SQLException {
        return inTransaction(() -> {
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
        });
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
