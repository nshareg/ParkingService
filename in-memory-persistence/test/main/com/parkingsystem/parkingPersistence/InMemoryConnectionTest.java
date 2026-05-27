package main.com.parkingsystem.parkingPersistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLFeatureNotSupportedException;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryConnectionTest {

    private InMemoryConnection connection;

    @BeforeEach
    void setUp() {
        connection = new InMemoryConnection();
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
