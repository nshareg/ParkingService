package com.parkingsystem.impl;

import com.parkingsystem.contract.ParkingRepository;
import com.parkingsystem.entity.ParkingSlot;
import com.parkingsystem.helpers.SlotType;

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
    on 25.05.26
*/
public class ParkingRepositoryimpl implements ParkingRepository {
    private Connection connection;

    public ParkingRepositoryimpl(Connection connection){
        this.connection = connection;
    }

    @Override
    public void init() throws SQLException {
        PreparedStatement createTable = connection.prepareStatement(
                "CREATE TABLE slots (slot_id, type, booked, number_plate)");
        createTable.executeUpdate();

        PreparedStatement indexBySlotId = connection.prepareStatement(
                "ALTER TABLE slots ADD INDEX (slot_id)");
        indexBySlotId.executeUpdate();

        PreparedStatement indexByNumberPlate = connection.prepareStatement(
                "ALTER TABLE slots ADD INDEX (number_plate)");
        indexByNumberPlate.executeUpdate();
    }

    @Override
    public void add(ParkingSlot parkingSlot) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO slots (slot_id, type, booked, number_plate) VALUES (?, ?, ?, ?)");
        ps.setObject(1, parkingSlot.getSlotID());
        ps.setString(2, parkingSlot.getType().name());
        ps.setBoolean(3, parkingSlot.isBooked());
        ps.setString(4, parkingSlot.getNumberPlate());
        ps.executeUpdate();
    }

    @Override
    public Optional<ParkingSlot> remove(UUID id) throws SQLException {
        Optional<ParkingSlot> found = findById(id);
        if (found.isEmpty()) {
            throw new SQLException("no slot with id: " + id);
        }
        PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM slots WHERE slot_id = ?");
        ps.setObject(1, id);
        ps.executeUpdate();
        return found;
    }

    @Override
    public void update(ParkingSlot slot) throws SQLException {
        Optional<ParkingSlot> found = findById(slot.getSlotID());
        if (found.isEmpty()) {
            throw new SQLException("no slot with id: " + slot.getSlotID());
        }
        PreparedStatement ps = connection.prepareStatement(
                "UPDATE slots SET type = ?, booked = ?, number_plate = ? WHERE slot_id = ?");
        ps.setString(1, slot.getType().name());
        ps.setBoolean(2, slot.isBooked());
        ps.setString(3, slot.getNumberPlate());
        ps.setObject(4, slot.getSlotID());
        ps.executeUpdate();
    }

    @Override
    public Optional<ParkingSlot> findById(UUID id) throws SQLException{
        PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM slots WHERE slot_id = ?");
        ps.setObject(1, id);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
    }

    @Override
    public List<ParkingSlot> findAll() throws SQLException{
        PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM slots");
        return collectAll(ps.executeQuery());
    }

    @Override
    public List<ParkingSlot> findAllFree() throws SQLException{
        PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM slots WHERE booked = false");
        return collectAll(ps.executeQuery());
    }

    @Override
    public List<ParkingSlot> findAllBooked() throws SQLException{
        PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM slots WHERE booked = true");
        return collectAll(ps.executeQuery());
    }

    @Override
    public List<ParkingSlot> findByType(SlotType type) throws SQLException{
        PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM slots WHERE type = ?"
        );
        ps.setObject(1, type.name());
        return collectAll(ps.executeQuery());
    }

    @Override
    public Optional<ParkingSlot> findByNumberPlate(String numberPlate) throws SQLException{
        PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM slots WHERE number_plate = ?");
        ps.setString(1, numberPlate);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
    }

    @Override
    public int count() throws SQLException{
        return findAll().size();
    }

    @Override
    public int countFree() throws SQLException{
        return findAllFree().size();
    }

    @Override
    public int countBooked() throws SQLException{
        return findAllBooked().size();
    }

    private ParkingSlot mapRow(ResultSet rs) throws SQLException {
        return new ParkingSlot(
                UUID.fromString(rs.getString("slot_id")),
                SlotType.valueOf(rs.getString("type")),
                rs.getBoolean("booked"),
                rs.getString("number_plate")
        );
    }

    private List<ParkingSlot> collectAll(ResultSet rs) throws SQLException {
        List<ParkingSlot> result = new ArrayList<>();
        while (rs.next()) {
            result.add(mapRow(rs));
        }
        return result;
    }
}
