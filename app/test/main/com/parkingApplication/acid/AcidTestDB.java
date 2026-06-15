package main.com.parkingApplication.acid;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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
}
