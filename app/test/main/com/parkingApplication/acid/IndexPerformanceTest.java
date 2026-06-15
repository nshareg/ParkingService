package main.com.parkingApplication.acid;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexPerformanceTest {

    private static final int ROWS = 1_000_000;
    private static final Pattern EXEC_TIME = Pattern.compile("Execution Time: ([0-9.]+) ms");

    @Test
    @DisplayName("single-column index on slot_id")
    void task3() throws Exception {
        try (Connection c = AcidTestDB.openConnection()) {
            c.setAutoCommit(true);
            try {
                loadData(c);

                String slotId;
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT slot_id FROM slots LIMIT 1")) {
                    rs.next();
                    slotId = rs.getString(1);
                }

                dropSecondaryIndexes(c);
                String noIndex = explain(c,
                        "SELECT * FROM parking_sessions WHERE slot_id = '" + slotId + "'");
                exec(c, "CREATE INDEX idx_ps_slot_id ON parking_sessions (slot_id)");
                exec(c, "ANALYZE parking_sessions");
                String withIndex = explain(c,
                        "SELECT * FROM parking_sessions WHERE slot_id = '" + slotId + "'");

                double t0 = execMs(noIndex);
                double t1 = execMs(withIndex);
                System.out.println("\n================ TASK 3  WHERE slot_id = ? ================");
                System.out.println("--- WITHOUT index ---");
                System.out.println(noIndex);
                System.out.println("--- WITH index ---");
                System.out.println(withIndex);
                System.out.printf("execution time: %.3f ms (seq scan)  ->  %.3f ms (index)  =  %.1fx faster%n",
                        t0, t1, t0 / t1);

                assertTrue(noIndex.contains("Seq Scan"),
                        "without an index Postgres must scan the whole table");
                assertTrue(withIndex.contains("Index Scan")
                           || withIndex.contains("Index Only Scan")
                           || withIndex.contains("Bitmap Index Scan"),
                        "with (slot_id) Postgres switches to an index scan");
                assertTrue(t1 < t0,
                        "the index scan must be faster than the sequential scan");
            } finally {
                restore(c);
            }
        }
    }

    @Test
    @DisplayName(" compound index (number_plate, active)")
    void task4() throws Exception {
        try (Connection c = AcidTestDB.openConnection()) {
            c.setAutoCommit(true);
            try {
                loadData(c);

                dropSecondaryIndexes(c);
                exec(c, "CREATE INDEX idx_ps_plate_active ON parking_sessions (number_plate, active)");
                exec(c, "ANALYZE parking_sessions");

                String wholeKey    = explain(c,
                        "SELECT * FROM parking_sessions WHERE number_plate = 'PLATE-42' AND active = true");
                String leadingCol  = explain(c,
                        "SELECT * FROM parking_sessions WHERE number_plate = 'PLATE-42'");
                String trailingCol = explain(c,
                        "SELECT * FROM parking_sessions WHERE active = true");

                System.out.println("\n================ TASK 4  compound index (number_plate, active) ================");
                System.out.println("--- whole key:  number_plate = ? AND active = true  (expect: uses index) ---");
                System.out.println(wholeKey);
                System.out.println("--- leading prefix:  number_plate = ?  (expect: uses index) ---");
                System.out.println(leadingCol);
                System.out.println("--- trailing only:  active = true  (expect: Seq Scan, NOT a left prefix) ---");
                System.out.println(trailingCol);

                assertTrue(wholeKey.contains("idx_ps_plate_active"),
                        "full key (number_plate + active) uses the compound index");
                assertTrue(leadingCol.contains("idx_ps_plate_active"),
                        "leading prefix (number_plate) uses the compound index");
                assertTrue(trailingCol.contains("Seq Scan"),
                        "trailing column alone (active) is not a left prefix -> sequential scan");
            } finally {
                restore(c);
            }
        }
    }

    private void loadData(Connection c) throws SQLException {
        dropSecondaryIndexes(c);
        AcidTestDB.truncateParkingTables(c);
        System.out.println("Loading " + ROWS + " parking_sessions rows (this takes a few seconds)...");
        exec(c, """
                DO $$
                DECLARE ids uuid[];
                BEGIN
                    INSERT INTO slots (slot_id, type, booked, number_plate)
                    SELECT gen_random_uuid(), 'REGULAR', false, NULL FROM generate_series(1, 1000);

                    SELECT array_agg(slot_id) INTO ids FROM slots;

                    INSERT INTO parking_sessions
                        (session_id, slot_id, number_plate, active, parked_at, released_at)
                    SELECT gen_random_uuid(),
                           ids[1 + (g %% array_length(ids, 1))],
                           'PLATE-' || (g %% 100000),
                           (g %% 5 = 0),
                           now()::text,
                           NULL
                    FROM generate_series(1, %d) g;
                END $$;
                """.formatted(ROWS));
        exec(c, "ANALYZE parking_sessions");
    }

    private void restore(Connection c) throws SQLException {//we use this to get everything back in the database as it initially was
        dropSecondaryIndexes(c);
        AcidTestDB.truncateParkingTables(c);
        exec(c, "CREATE INDEX IF NOT EXISTS idx_parking_sessions_slot_id ON parking_sessions (slot_id)");
        exec(c, "CREATE INDEX IF NOT EXISTS idx_parking_sessions_number_plate ON parking_sessions (number_plate)");
    }

    private void dropSecondaryIndexes(Connection c) throws SQLException {
        exec(c, "DROP INDEX IF EXISTS idx_parking_sessions_slot_id");
        exec(c, "DROP INDEX IF EXISTS idx_parking_sessions_number_plate");
        exec(c, "DROP INDEX IF EXISTS idx_ps_slot_id");
        exec(c, "DROP INDEX IF EXISTS idx_ps_plate_active");
    }

    private static void exec(@NonNull Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }

    private static @NonNull String explain(Connection c, String query) throws SQLException {
        StringBuilder sb = new StringBuilder();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("EXPLAIN (ANALYZE, BUFFERS) " + query)) {
            while (rs.next()) {
                sb.append(rs.getString(1)).append('\n');
            }
        }
        return sb.toString();
    }

    private static double execMs(String plan) {
        Matcher m = EXEC_TIME.matcher(plan);
        return m.find() ? Double.parseDouble(m.group(1)) : Double.NaN;
    }
}