import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PassLoader {

    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/assignment5_transport_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "0210";
    private static final int BATCH_SIZE = 500;

    /**
     * Loads valid CSV bus-pass records into MySQL inside a single atomic transaction.
     * Damaged lines in the CSV are skipped; any database error rolls back the entire load.
     */
    public int loadFromFile(Connection con, Path csvFile) throws IOException, SQLException {
        String insertSql = "INSERT INTO bus_pass (pass_id, reg_no, name, route_no, amount) VALUES (?, ?, ?, ?, ?)";
        boolean originalAutoCommit = con.getAutoCommit();
        int loadedCount = 0;
        int batchCount = 0;

        try {
            con.setAutoCommit(false);

            try (BufferedReader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8);
                 PreparedStatement ps = con.prepareStatement(insertSql)) {

                String line;
                int lineNo = 0;
                while ((line = reader.readLine()) != null) {
                    lineNo++;
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    String[] fields = line.split(",", -1);
                    if (fields.length != 5) {
                        System.out.printf("  [SKIP Line %d] Invalid field count (%d != 5): %s%n",
                                lineNo, fields.length, line);
                        continue;
                    }

                    try {
                        String passId = fields[0].trim();
                        String regNo = fields[1].trim();
                        String name = fields[2].trim();
                        int routeNo = Integer.parseInt(fields[3].trim());
                        double amount = Double.parseDouble(fields[4].trim());

                        ps.setString(1, passId);
                        ps.setString(2, regNo);
                        ps.setString(3, name);
                        ps.setInt(4, routeNo);
                        ps.setDouble(5, amount);
                        ps.addBatch();

                        batchCount++;
                        loadedCount++;

                        if (batchCount % BATCH_SIZE == 0) {
                            ps.executeBatch();
                            ps.clearBatch();
                        }
                    } catch (NumberFormatException ex) {
                        System.out.printf("  [SKIP Line %d] Malformed numeric field: %s%n", lineNo, line);
                    }
                }

                // Flush remaining statements in the final partial batch
                if (batchCount % BATCH_SIZE != 0) {
                    ps.executeBatch();
                }

                con.commit();
                return loadedCount;
            } catch (SQLException | IOException ex) {
                con.rollback();
                throw ex;
            }
        } finally {
            con.setAutoCommit(originalAutoCommit);
        }
    }

    public static int countTableRows(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bus_pass")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public static void main(String[] args) throws Exception {
        Path sampleCsv = Files.createTempFile("passes_b3_", ".csv");
        try (BufferedWriter writer = Files.newBufferedWriter(sampleCsv, StandardCharsets.UTF_8)) {
            writer.write("P3001,192411009,Fathi Muthu Johara M,14,4500.00\n");
            writer.write("P3002,CORRUPT_ROW_ONLY_TWO_FIELDS\n");
            writer.write("P3003,192411015,Arun K,12,4200.00\n");
            writer.write("P3004,192411022,Divya S,7,3800.00\n");
        }

        PassLoader loader = new PassLoader();
        try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            try (Statement st = con.createStatement()) {
                st.executeUpdate("TRUNCATE TABLE bus_pass");
            }

            int inserted = loader.loadFromFile(con, sampleCsv);
            int dbRows = countTableRows(con);

            System.out.println("Valid CSV records loaded : " + inserted);
            System.out.println("Rows confirmed in MySQL  : " + dbRows);
        } finally {
            Files.deleteIfExists(sampleCsv);
        }
    }
}