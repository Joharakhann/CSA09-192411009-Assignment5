import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdversarialInputTest {

    private static final String SERVER_URL =
            "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/assignment5_transport_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "0210";

    private static void setupUsersTable() throws SQLException {
        try (Connection con = DriverManager.getConnection(SERVER_URL, DB_USER, DB_PASS);
             Statement st = con.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS assignment5_transport_db");
            st.executeUpdate("USE assignment5_transport_db");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "id VARCHAR(50) PRIMARY KEY, " +
                "pwd VARCHAR(100) NOT NULL, " +
                "role VARCHAR(20) NOT NULL)"
            );
            st.executeUpdate(
                "INSERT IGNORE INTO users (id, pwd, role) VALUES " +
                "('admin', 'SuperSecret#2026', 'ADMIN'), " +
                "('192411009', 'JoharaPass@99', 'STUDENT')"
            );
        }
    }

    // PART 1A: UNSAFE LOGIN CHECK (String Concatenation)
    public static boolean loginUnsafe(Connection con, String id, String pwd) throws SQLException {
        String sql = "SELECT * FROM users WHERE id='" + id + "' AND pwd='" + pwd + "'";
        System.out.println("  [Unsafe Executed SQL] -> " + sql);
        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next();
        }
    }

    // PART 1B: SAFE LOGIN CHECK (Parameterized PreparedStatement)
    public static boolean loginSafe(Connection con, String id, String pwd) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ? AND pwd = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, pwd);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // PART 2: BUS-PASS FILE STORE PARSER (Resilient Against Damaged Inputs)
    public static List<String[]> loadValidPassesFromFile(Path file) throws IOException {
        List<String[]> validRecords = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;

                if (line.trim().isEmpty()) {
                    System.out.printf("  [REJECTED Line %d] Empty line skipped.%n", lineNum);
                    continue;
                }

                String[] fields = line.split(",", -1);
                if (fields.length != 5) {
                    System.out.printf("  [REJECTED Line %d] Wrong field count (%d != 5): \"%s\"%n",
                            lineNum, fields.length, line);
                    continue;
                }

                try {
                    Integer.parseInt(fields[3].trim());
                    double amount = Double.parseDouble(fields[4].trim());
                    if (amount < 0) {
                        System.out.printf("  [REJECTED Line %d] Negative amount (%.2f): \"%s\"%n",
                                lineNum, amount, line);
                        continue;
                    }
                } catch (NumberFormatException ex) {
                    System.out.printf("  [REJECTED Line %d] Non-numeric route/amount (%s): \"%s\"%n",
                            lineNum, ex.getMessage(), line);
                    continue;
                }

                System.out.printf("  [ACCEPTED Line %d] %s%n", lineNum, Arrays.toString(fields));
                validRecords.add(fields);
            }
        }
        return validRecords;
    }

    public static void main(String[] args) throws Exception {
        setupUsersTable();

        // TEST 1: ADVERSARIAL SQL INJECTION ON LOGIN CHECK
        System.out.println("=== TEST 1: SQL INJECTION ADVERSARIAL INPUT ===");
        String validId = "192411009";
        String validPwd = "JoharaPass@99";

        String attackId = "admin' OR '1'='1' -- ";
        String attackPwd = "invalid_password";

        try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            System.out.println("\n[Case 1A: Legitimate Credentials]");
            System.out.println("  Unsafe Login Result : " + loginUnsafe(con, validId, validPwd));
            System.out.println("  Safe Login Result   : " + loginSafe(con, validId, validPwd));

            System.out.println("\n[Case 1B: Adversarial SQLi Input -> id = \"" + attackId + "\", pwd = \"" + attackPwd + "\"]");
            boolean unsafeAccepted = loginUnsafe(con, attackId, attackPwd);
            boolean safeAccepted = loginSafe(con, attackId, attackPwd);

            System.out.println("  Unsafe Version Result : " + unsafeAccepted + "  <-- VULNERABLE (Authentication Bypassed!)");
            System.out.println("  Safe Version Result   : " + safeAccepted + " <-- PROTECTED (Attack Rejected!)");
        }

        // TEST 2: DAMAGED BUS-PASS FILE ADVERSARIAL INPUTS
        System.out.println("\n=== TEST 2: DAMAGED BUS-PASS FILE ADVERSARIAL INPUTS ===");
        Path adversarialFile = Paths.get("TASKB2", "adversarial_passes.txt");
        List<String> testLines = Arrays.asList(
                "P2001,192411009,Fathi Muthu Johara M,14,4500.00",   // Line 1: Valid record
                "P2002,192411010,MissingFieldsRecord",               // Line 2: Wrong field count
                "P2003,192411011,Arun K,12,FOUR_THOUSAND",           // Line 3: Non-numeric amount
                "",                                                  // Line 4: Empty line
                "P2004,192411012,Divya S,7,3800.00"                  // Line 5: Valid record after damaged lines
        );
        Files.write(adversarialFile, testLines, StandardCharsets.UTF_8);

        List<String[]> recovered = loadValidPassesFromFile(adversarialFile);
        System.out.println("\nSummary: Loaded " + recovered.size() + " valid records out of "
                + testLines.size() + " total lines without crashing.");
    }
}