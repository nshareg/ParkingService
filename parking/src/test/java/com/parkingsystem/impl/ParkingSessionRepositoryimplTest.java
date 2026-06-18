package com.parkingsystem.impl;

import com.parkingsystem.entity.ParkingSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
/*
    Created by anshanyan
    on 08.06.26
*/
@ExtendWith(MockitoExtension.class)
class ParkingSessionRepositoryimplTest {

    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private PreparedStatement ps;
    @Mock private ResultSet rs;

    private ParkingSessionRepositoryimpl repo;

    @BeforeEach
    void setUp() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        repo = new ParkingSessionRepositoryimpl(dataSource);
    }

    @Test
    void addSetsCorrectParameters() throws SQLException {
        UUID sessionId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        ParkingSession session = new ParkingSession(
                sessionId, slotId, "AREG-1", true, "2026-01-01T00:00:00Z", null);

        repo.add(session);

        verify(connection).prepareStatement(contains("INSERT INTO parking_sessions"));
        verify(ps).setObject(1, sessionId);
        verify(ps).setObject(2, slotId);
        verify(ps).setString(3, "AREG-1");
        verify(ps).setBoolean(4, true);
        verify(ps).setString(5, "2026-01-01T00:00:00Z");
        verify(ps).setString(6, null);
        verify(ps).executeUpdate();
    }

    @Test
    void updateSetsCorrectParameters() throws SQLException {
        UUID sessionId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        ParkingSession closed = new ParkingSession(
                sessionId, slotId, "AREG-1", false, "2026-01-01T00:00:00Z", "2026-01-02T00:00:00Z");

        repo.update(closed);

        verify(connection).prepareStatement(contains("UPDATE parking_sessions"));
        verify(ps).setBoolean(1, false);
        verify(ps).setString(2, "2026-01-02T00:00:00Z");
        verify(ps).setObject(eq(3), eq(sessionId));
        verify(ps).executeUpdate();
    }

    @Test
    void findBySlotMapsRowsAndPassesParam() throws SQLException {
        UUID sessionId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("session_id")).thenReturn(sessionId.toString());
        when(rs.getString("slot_id")).thenReturn(slotId.toString());
        when(rs.getString("number_plate")).thenReturn("AREG-1");
        when(rs.getBoolean("active")).thenReturn(true);
        when(rs.getString("parked_at")).thenReturn("2026-01-01T00:00:00Z");
        when(rs.getString("released_at")).thenReturn(null);

        List<ParkingSession> result = repo.findBySlot(slotId);

        assertEquals(1, result.size());
        ParkingSession session = result.get(0);
        assertEquals(sessionId, session.getSessionId());
        assertEquals(slotId, session.getSlotId());
        assertEquals("AREG-1", session.getNumberPlate());
        assertTrue(session.isActive());
        assertNull(session.getReleasedAt());
        verify(connection).prepareStatement(contains("WHERE slot_id = ?"));
        verify(ps).setObject(1, slotId);
    }

    @Test
    void findByNumberPlatePassesParam() throws SQLException {
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertTrue(repo.findByNumberPlate("AREG-1").isEmpty());
        verify(connection).prepareStatement(contains("WHERE number_plate = ?"));
        verify(ps).setString(1, "AREG-1");
    }

    @Test
    void findActiveByNumberPlateReturnsOpenSession() throws SQLException {
        UUID sessionId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("session_id")).thenReturn(sessionId.toString());
        when(rs.getString("slot_id")).thenReturn(slotId.toString());
        when(rs.getString("number_plate")).thenReturn("AREG-1");
        when(rs.getBoolean("active")).thenReturn(true);
        when(rs.getString("parked_at")).thenReturn("2026-01-01T00:00:00Z");
        when(rs.getString("released_at")).thenReturn(null);

        Optional<ParkingSession> active = repo.findActiveByNumberPlate("AREG-1");

        assertTrue(active.isPresent());
        assertEquals(sessionId, active.get().getSessionId());
    }

    @Test
    void findActiveByNumberPlateIgnoresClosedSession() throws SQLException {
        UUID sessionId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("session_id")).thenReturn(sessionId.toString());
        when(rs.getString("slot_id")).thenReturn(slotId.toString());
        when(rs.getString("number_plate")).thenReturn("AREG-1");
        when(rs.getBoolean("active")).thenReturn(false);
        when(rs.getString("parked_at")).thenReturn("2026-01-01T00:00:00Z");
        when(rs.getString("released_at")).thenReturn("2026-01-02T00:00:00Z");

        assertTrue(repo.findActiveByNumberPlate("AREG-1").isEmpty());
    }
}
