package com.parkingApplication.acid;

import com.parkingsystem.helpers.SlotType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

final class AcidTestDB {

    private AcidTestDB() {
    }

    static Connection openConnection() throws SQLException {
        String host = System.getenv().getOrDefault("POSTGRES_HOST", "localhost");
        String port = System.getenv().getOrDefault("POSTGRES_PORT", "5432");
        String db   = System.getenv().getOrDefault("POSTGRES_DB", "parking");
        String user = System.getenv().getOrDefault("POSTGRES_USER", "postgres");
        String pass = System.getenv().getOrDefault("POSTGRES_PASSWORD", "postgres");
        return DriverManager.getConnection(
                "jdbc:postgresql://" + host + ":" + port + "/" + db, user, pass);
    }

    static void truncateParkingTables(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("TRUNCATE TABLE slots CASCADE");
            st.executeUpdate("TRUNCATE TABLE parking_sessions");
        }
    }

    /* Raw-JDBC helpers for the ACID demos. These talk plain SQL on a caller-managed
       Connection on purpose — the tests drive transaction/isolation themselves, which
       a JPA repository (managing its own connection) cannot express. */

    static UUID insertFreeSlot(Connection c, SlotType type) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO slots (slot_id, type, booked, number_plate) VALUES (?, ?, false, NULL)")) {
            ps.setObject(1, id);
            ps.setString(2, type.name());
            ps.executeUpdate();
        }
        return id;
    }

    static Optional<UUID> firstFreeSlot(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT slot_id FROM slots WHERE booked = false LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(rs.getObject(1, UUID.class)) : Optional.empty();
        }
    }

    static void bookSlot(Connection c, UUID slotId, String plate) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE slots SET booked = true, number_plate = ? WHERE slot_id = ?")) {
            ps.setString(1, plate);
            ps.setObject(2, slotId);
            ps.executeUpdate();
        }
    }

    static boolean isBooked(Connection c, UUID slotId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT booked FROM slots WHERE slot_id = ?")) {
            ps.setObject(1, slotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("no slot " + slotId);
                return rs.getBoolean(1);
            }
        }
    }

    static void insertSession(Connection c, UUID slotId, String plate) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO parking_sessions " +
                        "(session_id, slot_id, number_plate, active, parked_at, released_at) " +
                        "VALUES (?, ?, ?, true, ?, NULL)")) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, slotId);
            ps.setString(3, plate);
            ps.setString(4, Instant.now().toString());
            ps.executeUpdate();
        }
    }

    static long activeSessions(Connection c, UUID slotId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT count(*) FROM parking_sessions WHERE slot_id = ? AND active = true")) {
            ps.setObject(1, slotId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }
}
