package main.com.parkingsystem.impl;

import main.com.parkingsystem.contract.ParkingSessionRepository;
import main.com.parkingsystem.entity.ParkingSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
    Created by anshanyan
    on 08.06.26
*/
public class ParkingSessionRepositoryimpl implements ParkingSessionRepository {
    private Connection connection;

    public ParkingSessionRepositoryimpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void init() throws SQLException {
        PreparedStatement createTable = connection.prepareStatement(
                "CREATE TABLE parking_sessions (session_id, slot_id, number_plate, active, parked_at, released_at)");
        createTable.executeUpdate();

        PreparedStatement indexBySessionId = connection.prepareStatement(
                "ALTER TABLE parking_sessions ADD INDEX (session_id)");
        indexBySessionId.executeUpdate();

        PreparedStatement indexBySlotId = connection.prepareStatement(
                "ALTER TABLE parking_sessions ADD INDEX (slot_id)");
        indexBySlotId.executeUpdate();

        PreparedStatement indexByNumberPlate = connection.prepareStatement(
                "ALTER TABLE parking_sessions ADD INDEX (number_plate)");
        indexByNumberPlate.executeUpdate();
    }

    @Override
    public void add(ParkingSession session) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO parking_sessions (session_id, slot_id, number_plate, active, parked_at, released_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?)");
        ps.setObject(1, session.getSessionId());
        ps.setObject(2, session.getSlotId());
        ps.setString(3, session.getNumberPlate());
        ps.setBoolean(4, session.isActive());
        ps.setString(5, session.getParkedAt());
        ps.setString(6, session.getReleasedAt());
        ps.executeUpdate();
    }

    @Override
    public void update(ParkingSession session) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "UPDATE parking_sessions SET active = ?, released_at = ? WHERE session_id = ?");
        ps.setBoolean(1, session.isActive());
        ps.setString(2, session.getReleasedAt());
        ps.setObject(3, session.getSessionId());
        ps.executeUpdate();
    }


    @Override
    public Optional<ParkingSession> findById(UUID id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM parking_sessions WHERE session_id = ?");
        ps.setObject(1, id);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
    }

    @Override
    public Optional<ParkingSession> remove(UUID id) throws SQLException {
        Optional<ParkingSession> found = findById(id);
        if (found.isEmpty()) {
            throw new SQLException("no session with id: " + id);
        }
        PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM parking_sessions WHERE session_id = ?");
        ps.setObject(1, id);
        ps.executeUpdate();
        return found;
    }

    @Override
    public List<ParkingSession> findBySlot(UUID slotId) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM parking_sessions WHERE slot_id = ?");
        ps.setObject(1, slotId);
        return collectAll(ps.executeQuery());
    }

    @Override
    public List<ParkingSession> findByNumberPlate(String numberPlate) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM parking_sessions WHERE number_plate = ?");
        ps.setString(1, numberPlate);
        return collectAll(ps.executeQuery());
    }

    @Override
    public Optional<ParkingSession> findActiveByNumberPlate(String numberPlate) throws SQLException {
        return findByNumberPlate(numberPlate).stream()
                .filter(ParkingSession::isActive)
                .findFirst();
    }

    @Override
    public List<ParkingSession> findAll() throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM parking_sessions");
        return collectAll(ps.executeQuery());
    }

    @Override
    public int count() throws SQLException {
        return findAll().size();
    }

    private ParkingSession mapRow(ResultSet rs) throws SQLException {
        return new ParkingSession(
                UUID.fromString(rs.getString("session_id")),
                UUID.fromString(rs.getString("slot_id")),
                rs.getString("number_plate"),
                rs.getBoolean("active"),
                rs.getString("parked_at"),
                rs.getString("released_at")
        );
    }

    private List<ParkingSession> collectAll(ResultSet rs) throws SQLException {
        List<ParkingSession> result = new ArrayList<>();
        while (rs.next()) {
            result.add(mapRow(rs));
        }
        return result;
    }
}
