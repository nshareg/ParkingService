package com.parkingsystem.impl;

import com.parkingsystem.entity.ParkingSlot;
import com.parkingsystem.helpers.SlotType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
/*
    Created by anshanyan
    on 27.05.26
*/
@ExtendWith(MockitoExtension.class)
class ParkingRepositoryimplTest {

    @Mock private Connection connection;
    @Mock private PreparedStatement ps;
    @Mock private ResultSet rs;

    private ParkingRepositoryimpl repo;

    @BeforeEach
    void setUp() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        repo = new ParkingRepositoryimpl(connection);
    }

    @Test
    void addSetsCorrectParameters() throws SQLException {
        ParkingSlot slot = new ParkingSlot(SlotType.REGULAR);

        repo.add(slot);

        verify(connection).prepareStatement(contains("INSERT INTO slots"));
        verify(ps).setObject(1, slot.getSlotID());
        verify(ps).setString(2, "REGULAR");
        verify(ps).setBoolean(3, false);
        verify(ps).setString(4, null);
        verify(ps).executeUpdate();
    }

    @Test
    void findByIdMapsRow() throws SQLException {
        UUID id = UUID.randomUUID();
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("slot_id")).thenReturn(id.toString());
        when(rs.getString("type")).thenReturn("ELECTRIC");
        when(rs.getBoolean("booked")).thenReturn(true);
        when(rs.getString("number_plate")).thenReturn("AREG-1");

        ParkingSlot found = repo.findById(id).orElseThrow();

        assertEquals(id, found.getSlotID());
        assertEquals(SlotType.ELECTRIC, found.getType());
        assertTrue(found.isBooked());
        assertEquals("AREG-1", found.getNumberPlate());
        verify(ps).setObject(1, id);
    }

    @Test
    void findByIdReturnsEmptyForUnknown() throws SQLException {
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertTrue(repo.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void updateSetsCorrectParameters() throws SQLException {
        UUID id = UUID.randomUUID();
        ParkingSlot slot = new ParkingSlot(id, SlotType.REGULAR, true, "AREG-1");

        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("slot_id")).thenReturn(id.toString());
        when(rs.getString("type")).thenReturn("REGULAR");
        when(rs.getBoolean("booked")).thenReturn(false);
        when(rs.getString("number_plate")).thenReturn(null);

        repo.update(slot);

        verify(ps).setString(1, "REGULAR");
        verify(ps).setBoolean(2, true);
        verify(ps).setString(3, "AREG-1");
        verify(ps).setObject(eq(4), eq(id));
        verify(ps).executeUpdate();
    }

    @Test
    void updateNonExistentThrows() throws SQLException {
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        ParkingSlot slot = new ParkingSlot(SlotType.REGULAR);
        assertThrows(SQLException.class, () -> repo.update(slot));
    }

    @Test
    void removeDeletesRow() throws SQLException {
        UUID id = UUID.randomUUID();
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("slot_id")).thenReturn(id.toString());
        when(rs.getString("type")).thenReturn("DISABLED");
        when(rs.getBoolean("booked")).thenReturn(false);
        when(rs.getString("number_plate")).thenReturn(null);

        assertTrue(repo.remove(id).isPresent());
        verify(ps).executeUpdate();
    }

    @Test
    void removeNonExistentThrows() throws SQLException {
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertThrows(SQLException.class, () -> repo.remove(UUID.randomUUID()));
    }

    @Test
    void findByNumberPlatePassesParam() throws SQLException {
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertTrue(repo.findByNumberPlate("AREG-1").isEmpty());
        verify(ps).setString(1, "AREG-1");
    }
}
