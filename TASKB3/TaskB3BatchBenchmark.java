import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TaskB3BatchBenchmark {

    private static final String BASE_URL =
            "jdbc:mysql://localhost:3306/assignment5_transport_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String REWRITE_URL =
            BASE_URL + "&rewriteBatchedStatements=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "0210"; // <-- Enter your 4-character MySQL password

    private static void resetTable(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.executeUpdate("TRUNCATE TABLE bus_pass");
        }
    }

    // Mode 1: Individual Statement inserts with auto-commit = true
    public static double insertWithStatementAutoCommit(Connection con, int count) throws SQLException {
        resetTable(con);
        con.setAutoCommit(true);
        long start = System.nanoTime();
        try (Statement st = con.createStatement()) {
            for (int i = 1; i <= count; i++) {
                String sql = String.format(
                        "INSERT INTO bus_pass VALUES ('P%d', '192411009', 'Student_%d', %d, 4500.00)",
                        i, i, (i % 20) + 1);
                st.executeUpdate(sql);
            }
        }
        return (System.nanoTime() - start) / 1_000_000.0;
    }

    // Mode 2: Individual PreparedStatement executeUpdate() inside 1 transaction
    public static double insertWithPreparedSingleTx(Connection con, int count) throws SQLException {
        resetTable(con);
        con.setAutoCommit(false);
        String sql = "INSERT INTO bus_pass (pass_id, reg_no, name, route_no, amount) VALUES (?, ?, ?, ?, ?)";
        long start = System.nanoTime();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 1; i <= count; i++) {
                ps.setString(1, "P" + i);
                ps.setString(2, "192411009");
                ps.setString(3, "Student_" + i);
                ps.setInt(4, (i % 20) + 1);
                ps.setDouble(5, 4500.00);
                ps.executeUpdate();
            }
            con.commit();
        } finally {
            con.setAutoCommit(true);
        }
        return (System.nanoTime() - start) / 1_000_000.0;
    }

    // Mode 3 & 4: PreparedStatement addBatch() + executeBatch() (batchSize = 500)
    public static double insertWithPreparedBatch(Connection con, int count, int batchSize) throws SQLException {
        resetTable(con);
        con.setAutoCommit(false);
        String sql = "INSERT INTO bus_pass (pass_id, reg_no, name, route_no, amount) VALUES (?, ?, ?, ?, ?)";
        long start = System.nanoTime();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 1; i <= count; i++) {
                ps.setString(1, "P" + i);
                ps.setString(2, "192411009");
                ps.setString(3, "Student_" + i);
                ps.setInt(4, (i % 20) + 1);
                ps.setDouble(5, 4500.00);
                ps.addBatch();

                if (i % batchSize == 0) {
                    ps.executeBatch();
                    ps.clearBatch();
                }
            }
            if (count % batchSize != 0) {
                ps.executeBatch();
            }
            con.commit();
        } finally {
            con.setAutoCommit(true);
        }
        return (System.nanoTime() - start) / 1_000_000.0;
    }

    // Atomicity Verification: Duplicate key in batch #2 must roll back batch #1 completely
    public static void verifyAtomicRollbackOnBatchFailure(Connection con) throws SQLException {
        resetTable(con);
        con.setAutoCommit(false);
        String sql = "INSERT INTO bus_pass (pass_id, reg_no, name, route_no, amount) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            // Batch 1: 500 valid rows executed against DB
            for (int i = 1; i <= 500; i++) {
                ps.setString(1, "P" + i);
                ps.setString(2, "192411009");
                ps.setString(3, "Student_" + i);
                ps.setInt(4, 14);
                ps.setDouble(5, 4500.00);
                ps.addBatch();
            }
            ps.executeBatch();
            ps.clearBatch();

            // Batch 2: Inject duplicate primary key 'P1'
            ps.setString(1, "P1");
            ps.setString(2, "192411009");
            ps.setString(3, "Duplicate_Key_Crash");
            ps.setInt(4, 14);
            ps.setDouble(5, 4500.00);
            ps.addBatch();
            ps.executeBatch();

            con.commit();
        } catch (SQLException expected) {
            con.rollback();
            System.out.println("  [ROLLBACK TRIGGERED] Caught duplicate PK in Batch 2: " + expected.getMessage());
        } finally {
            con.setAutoCommit(true);
        }

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bus_pass")) {
            rs.next();
            System.out.println("  [ATOMICITY CHECK] Rows remaining in bus_pass after rollback: " + rs.getInt(1));
        }
    }

    public static void main(String[] args) throws SQLException {
        int[] rowCounts = {1_000, 5_000, 10_000};

        System.out.println("=== TASK B3: JDBC INSERTION & BATCHING BENCHMARK ===");
        System.out.printf("%-8s | %-18s | %-18s | %-18s | %-20s%n",
                "Rows", "Statement Auto(ms)", "PrepStmt 1-Tx(ms)", "Batch 500 (ms)", "Batch+Rewrite (ms)");
        System.out.println("--------------------------------------------------------------------------------------------");

        try (Connection baseCon = DriverManager.getConnection(BASE_URL, DB_USER, DB_PASS);
             Connection rewriteCon = DriverManager.getConnection(REWRITE_URL, DB_USER, DB_PASS)) {

            // Warm-up JVM & MySQL connection
            insertWithPreparedBatch(baseCon, 200, 100);

            for (int n : rowCounts) {
                double tStmt = insertWithStatementAutoCommit(baseCon, n);
                double tPrep = insertWithPreparedSingleTx(baseCon, n);
                double tBatch = insertWithPreparedBatch(baseCon, n, 500);
                double tRewrite = insertWithPreparedBatch(rewriteCon, n, 500);

                System.out.printf("%-8d | %-18.2f | %-18.2f | %-18.2f | %-20.2f%n",
                        n, tStmt, tPrep, tBatch, tRewrite);
            }

            System.out.println("\n=== TASK B3: TRANSACTION ROLLBACK & ATOMICITY TEST ===");
            verifyAtomicRollbackOnBatchFailure(baseCon);
        }
    }
}