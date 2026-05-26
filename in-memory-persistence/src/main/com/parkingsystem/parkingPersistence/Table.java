package main.com.parkingsystem.parkingPersistence;

import main.com.parkingsystem.entity.ParkingSlot;

import java.util.*;

/*
    Created by anshanyan
    on 26.05.26
*/
public class Table <V> {

    private final String name;
    private final Set<V> data = new LinkedHashSet<>();
    private List<Index<?, V>> indexList = new ArrayList<>();

    public Table(String tableName){
        name = tableName;
        indexList.add(new Index<>());
    }

    public Set<ParkingSlot> getData() {
        return data;
    }

    public List<Index> getIndexList() {
        return indexList;
    }

    public void setData(Set<ParkingSlot> data) {
        this.data = data;
    }

    public void setIndexList(List<Index> indexList) {
        this.indexList = indexList;
    }

    public String getName() {
        return name;
    }

    public void setIndexList(List<Index<?, V>> indexList) {
        this.indexList = indexList;
    }
}
