package com.inMemoryPersistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
/*
    Created by anshanyan
    on 27.05.26
*/
class InMemoryPreparedStatementTest {

    private Connection connection;

    @BeforeEach
    void setUp() {
        Storage.createTable("slots");
        connection = new InMemoryConnection();
    }

    private void insertSlot(UUID id, String type, boolean booked, String plate) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO slots (slot_id, type, booked, number_plate) VALUES (?, ?, ?, ?)");
        ps.setObject(1, id);
        ps.setString(2, type);
        ps.setBoolean(3, booked);
        ps.setString(4, plate);
        ps.executeUpdate();
    }

    @Test
    void insertAndSelectAll() throws SQLException {
        UUID id = UUID.randomUUID();
        insertSlot(id, "REGULAR", false, null);

        PreparedStatement ps = connection.prepareStatement("SELECT * FROM slots");
        ResultSet rs = ps.executeQuery();

        assertTrue(rs.next());
        assertEquals(id.toString(), rs.getString("slot_id"));
        assertEquals("REGULAR", rs.getString("type"));
        assertFalse(rs.getBoolean("booked"));
        assertNull(rs.getString("number_plate"));
        assertFalse(rs.next());
    }

    @Test
    void selectWhereParameter() throws SQLException {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        insertSlot(id1, "REGULAR", false, null);
        insertSlot(id2, "HANDICAPPED", false, null);

        PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM slots WHERE slot_id = ?");
        ps.setObject(1, id1);
        ResultSet rs = ps.executeQuery();

        assertTrue(rs.next());
        assertEquals(id1.toString(), rs.getString("slot_id"));
        assertFalse(rs.next());
    }

    @Test
    void selectWhereBooleanLiteral() throws SQLException {
        UUID free = UUID.randomUUID();
        UUID booked = UUID.randomUUID();
        insertSlot(free, "REGULAR", false, null);
        insertSlot(booked, "REGULAR", true, "ABC123");

        PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM slots WHERE booked = false");
        ResultSet rs = ps.executeQuery();

        assertTrue(rs.next());
        assertEquals(free.toString(), rs.getString("slot_id"));
        assertFalse(rs.next());
    }

    @Test
    void updateRow() throws SQLException {
        UUID id = UUID.randomUUID();
        insertSlot(id, "REGULAR", false, null);

        PreparedStatement ps = connection.prepareStatement(
                "UPDATE slots SET type = ?, booked = ?, number_plate = ? WHERE slot_id = ?");
        ps.setString(1, "REGULAR");
        ps.setBoolean(2, true);
        ps.setString(3, "XYZ789");
        ps.setObject(4, id);
        assertEquals(1, ps.executeUpdate());

        ps = connection.prepareStatement("SELECT * FROM slots WHERE slot_id = ?");
        ps.setObject(1, id);
        ResultSet rs = ps.executeQuery();
        assertTrue(rs.next());
        assertTrue(rs.getBoolean("booked"));
        assertEquals("XYZ789", rs.getString("number_plate"));
    }

    @Test
    void deleteRow() throws SQLException {
        UUID id = UUID.randomUUID();
        insertSlot(id, "REGULAR", false, null);

        PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM slots WHERE slot_id = ?");
        ps.setObject(1, id);
        assertEquals(1, ps.executeUpdate());

        ps = connection.prepareStatement("SELECT * FROM slots");
        ResultSet rs = ps.executeQuery();
        assertFalse(rs.next());
    }

    @Test
    void selectFromMissingTableThrows() {
        assertThrows(SQLException.class, () -> {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM missing");
            ps.executeQuery();
        });
    }
}
