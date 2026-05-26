package main.com.parkingsystem.parkingPersistence;

import main.com.parkingsystem.entity.ParkingSlot;

import java.util.HashMap;
import java.util.Map;

/*
    Created by anshanyan
    on 26.05.26
*/
public class Storage {
    private final static Map<String, Table<?>> tableList = new HashMap<>();

    public static <clazz> Table<clazz> createTable(String tableName){
        return tableList.put(tableName, new Table<clazz>(tableName));
    }
}
