package main.com.parkingsystem.contract;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/*
    Created by anshanyan
    on 08.06.26
*/

public interface Repository<T, ID> {

    /** Initialise the underlying storage (create the table and its indexes). */
    void init() throws SQLException;

    /** Persist a new entity. */
    void add(T entity) throws SQLException;

    /** Persist changes to an existing entity. */
    void update(T entity) throws SQLException;

    /** Find an entity by its identifier. */
    Optional<T> findById(ID id) throws SQLException;

    /** Remove an entity by its identifier, returning the removed entity. */
    Optional<T> remove(ID id) throws SQLException;

    /** Retrieve all entities. */
    List<T> findAll() throws SQLException;

    /** Count all entities. */
    int count() throws SQLException;
}
