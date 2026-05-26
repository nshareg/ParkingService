package main.com.ticketingsystem.parkingPersistence;

import main.com.ticketingsystem.entity.ParkingSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLFeatureNotSupportedException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryConnectionTest {

    private InMemoryConnection connection;

    @BeforeEach
    void setUp() {
        Index<UUID, ParkingSlot> slots = Index.createIndex();
        Index<String, UUID> plateIndex = Index.createIndex();
        connection = new InMemoryConnection(slots, plateIndex);
    }

    @Test
    void isNotClosedInitially() {
        assertFalse(connection.isClosed());
    }

    @Test
    void closeMarksConnectionAsClosed() {
        connection.close();
        assertTrue(connection.isClosed());
    }

    @Test
    void prepareStatementReturnsNonNull() throws Exception {
        assertNotNull(connection.prepareStatement("SELECT * FROM slots"));
    }

    @Test
    void unsupportedMethodThrows() {
        assertThrows(SQLFeatureNotSupportedException.class, () -> connection.createStatement());
    }
}
