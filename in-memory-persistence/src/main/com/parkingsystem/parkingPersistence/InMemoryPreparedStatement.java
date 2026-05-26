package main.com.parkingsystem.parkingPersistence;

import main.com.parkingsystem.entity.ParkingSlot;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

/*
    Created by anshanyan
    on 25.05.26
*/
public class InMemoryPreparedStatement implements PreparedStatement {
    String query;
    Index<UUID, ParkingSlot> slots;
    Index<String, UUID> plateToIDIndex;
    Map<Integer, Object> parameters;

    public InMemoryPreparedStatement(
            String sql,
            Index<UUID, ParkingSlot> slots,
            Index<String, UUID> plateToIDIndex) {
        query = sql;
        this.slots = slots;
        this.plateToIDIndex = plateToIDIndex;
    }
    @Override
    public ResultSet executeQuery() throws SQLException{
        if(query.contains("CREATE TABLE")){
            Table.createTable();
        }
        //if query contains something, f.e. where slot_id, then we just parse the parameters(1,?)
    }
    @Override
    public int executeUpdate() throws SQLException{
        //if query contains something, f.e. where slot_id, then we just parse the parameters(1,?)
    }




    @Override
    public void setObject(int index, Object value) throws SQLException {
        parameters.put(index, value);
    }

    @Override
    public void setString(int index, String value) throws SQLException {
        parameters.put(index, value);
    }

    @Override
    public void setBoolean(int index, boolean value) throws SQLException {
        parameters.put(index, value);
    }
}
