package main.com.inMemoryPersistence;

import java.util.HashMap;
import java.util.Map;

/*
    Created by anshanyan
    on 26.05.26
*/
public class Storage {
    private final static Map<String, Table<?>> tableList = new HashMap<>();

    public static <T> Table<T> createTable(String tableName) {
        Table<T> table = new Table<>(tableName);
        tableList.put(tableName, table);
        return table;
    }

    public static <T> Table<T> getTable(String tableName) {
        return (Table<T>) tableList.get(tableName);
    }
}
