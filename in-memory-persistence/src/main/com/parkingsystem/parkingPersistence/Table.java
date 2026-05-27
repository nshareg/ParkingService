package main.com.parkingsystem.parkingPersistence;

import java.util.*;

/*
    Created by anshanyan
    on 26.05.26
*/
public class Table<V> {

    private final String name;
    private final Set<V> data = new LinkedHashSet<>();
    private List<Index<?, V>> indexList = new ArrayList<>();

    public Table(String tableName) {
        name = tableName;
    }

    public Set<V> getData() {
        return data;
    }

    public List<Index<?, V>> getIndexList() {
        return indexList;
    }

    public void setIndexList(List<Index<?, V>> indexList) {
        this.indexList = indexList;
    }

    public String getName() {
        return name;
    }
}
