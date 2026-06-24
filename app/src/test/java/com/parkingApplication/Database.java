package com.parkingApplication;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;


public final class Database {

    private Database() {
    }

    private static final String HOST     = System.getenv().getOrDefault("POSTGRES_HOST", "localhost");
    private static final String PORT     = System.getenv().getOrDefault("POSTGRES_PORT", "5432");
    private static final String DB       = System.getenv().getOrDefault("POSTGRES_DB", "parking");
    private static final String USER     = System.getenv().getOrDefault("POSTGRES_USER", "postgres");
    private static final String PASSWORD = System.getenv().getOrDefault("POSTGRES_PASSWORD", "postgres");
    private static final String URL      = "jdbc:postgresql://" + HOST + ":" + PORT + "/" + DB;

    //START NON-POOLED
//     public static Connection getConnection() throws SQLException {
//         return DriverManager.getConnection(URL, USER, PASSWORD);
//     }
    //END NON-POOLED

//    //START POOLED
    private static final DataSource DATA_SOURCE = createDataSource();

    private static DataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        return new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return DATA_SOURCE.getConnection();
    }
//    //END POOLED
}
