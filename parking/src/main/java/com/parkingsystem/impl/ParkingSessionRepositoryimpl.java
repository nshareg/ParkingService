package com.parkingsystem.impl;

import com.parkingsystem.contract.ParkingSessionRepository;
import com.parkingsystem.entity.ParkingSession;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
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
    private final DataSource dataSource;

    public ParkingSessionRepositoryimpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void init() throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement createTable = connection.prepareStatement(
                "CREATE TABLE parking_sessions (session_id, slot_id, number_plate, active, parked_at, released_at)")) {
            createTable.executeUpdate();
        }
        try (PreparedStatement indexBySessionId = connection.prepareStatement(
                "ALTER TABLE parking_sessions ADD INDEX (session_id)")) {
            indexBySessionId.executeUpdate();
        }
        try (PreparedStatement indexBySlotId = connection.prepareStatement(
                "ALTER TABLE parking_sessions ADD INDEX (slot_id)")) {
            indexBySlotId.executeUpdate();
        }
        try (PreparedStatement indexByNumberPlate = connection.prepareStatement(
                "ALTER TABLE parking_sessions ADD INDEX (number_plate)")) {
            indexByNumberPlate.executeUpdate();
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Override
    public void add(ParkingSession session) throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO parking_sessions (session_id, slot_id, number_plate, active, parked_at, released_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setObject(1, session.getSessionId());
            ps.setObject(2, session.getSlotId());
            ps.setString(3, session.getNumberPlate());
            ps.setBoolean(4, session.isActive());
            ps.setString(5, session.getParkedAt());
            ps.setString(6, session.getReleasedAt());
            ps.executeUpdate();
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Override
    public void update(ParkingSession session) throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE parking_sessions SET active = ?, released_at = ? WHERE session_id = ?")) {
            ps.setBoolean(1, session.isActive());
            ps.setString(2, session.getReleasedAt());
            ps.setObject(3, session.getSessionId());
            ps.executeUpdate();
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Override
    public Optional<ParkingSession> findById(UUID id) throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM parking_sessions WHERE session_id = ?")) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Override
    public Optional<ParkingSession> remove(UUID id) throws SQLException {
        Optional<ParkingSession> found = findById(id);
        if (found.isEmpty()) {
            throw new SQLException("no session with id: " + id);
        }
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM parking_sessions WHERE session_id = ?")) {
            ps.setObject(1, id);
            ps.executeUpdate();
            return found;
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Override
    public List<ParkingSession> findBySlot(UUID slotId) throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM parking_sessions WHERE slot_id = ?")) {
            ps.setObject(1, slotId);
            try (ResultSet rs = ps.executeQuery()) {
                return collectAll(rs);
            }
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Override
    public List<ParkingSession> findByNumberPlate(String numberPlate) throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM parking_sessions WHERE number_plate = ?")) {
            ps.setString(1, numberPlate);
            try (ResultSet rs = ps.executeQuery()) {
                return collectAll(rs);
            }
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Override
    public Optional<ParkingSession> findActiveByNumberPlate(String numberPlate) throws SQLException {
        return findByNumberPlate(numberPlate).stream()
                .filter(ParkingSession::isActive)
                .findFirst();
    }

    @Override
    public List<ParkingSession> findAll() throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM parking_sessions");
             ResultSet rs = ps.executeQuery()) {
            return collectAll(rs);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
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
