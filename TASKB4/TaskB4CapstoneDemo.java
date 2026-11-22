import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TaskB4CapstoneDemo {

    private static final String SERVER_URL =
            "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/assignment5_transport_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "0210"; // <-- Enter your local MySQL password

    private static void initializeSchema() throws SQLException {
        try (Connection con = DriverManager.getConnection(SERVER_URL, DB_USER, DB_PASS);
             Statement st = con.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS assignment5_transport_db");
            st.executeUpdate("USE assignment5_transport_db");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS bus_pass (" +
                "pass_id VARCHAR(20) PRIMARY KEY, " +
                "reg_no VARCHAR(20) NOT NULL, " +
                "name VARCHAR(100) NOT NULL, " +
                "route_no INT NOT NULL, " +
                "amount DECIMAL(10, 2) NOT NULL)"
            );
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS wallets (" +
                "student_id VARCHAR(20) NOT NULL, " +
                "wallet_type VARCHAR(20) NOT NULL, " +
                "balance DECIMAL(10, 2) NOT NULL, " +
                "PRIMARY KEY (student_id, wallet_type))"
            );
        }
    }

    // Atomic ₹500 Hostel -> Canteen Wallet Transfer (Task A5.1)
    public static boolean transferWalletFunds(Connection con, String studentId,
                                              BigDecimal amount, boolean simulateCrashAfterDebit) throws SQLException {
        boolean originalAutoCommit = con.getAutoCommit();
        try {
            con.setAutoCommit(false);

            // Step 1: Debit Hostel Wallet (only if balance >= amount)
            String debitSql = "UPDATE wallets SET balance = balance - ? " +
                              "WHERE student_id = ? AND wallet_type = 'HOSTEL' AND balance >= ?";
            try (PreparedStatement psDebit = con.prepareStatement(debitSql)) {
                psDebit.setBigDecimal(1, amount);
                psDebit.setString(2, studentId);
                psDebit.setBigDecimal(3, amount);
                if (psDebit.executeUpdate() != 1) {
                    con.rollback();
                    System.out.println("  [ABORTED & ROLLED BACK] Insufficient HOSTEL balance for ₹" + amount);
                    return false;
                }
            }

            // Simulate sudden crash / network drop between debit and credit
            if (simulateCrashAfterDebit) {
                throw new SQLException("Simulated power/network crash after HOSTEL debit!");
            }

            // Step 2: Credit Canteen Wallet
            String creditSql = "UPDATE wallets SET balance = balance + ? " +
                               "WHERE student_id = ? AND wallet_type = 'CANTEEN'";
            try (PreparedStatement psCredit = con.prepareStatement(creditSql)) {
                psCredit.setBigDecimal(1, amount);
                psCredit.setString(2, studentId);
                if (psCredit.executeUpdate() != 1) {
                    con.rollback();
                    System.out.println("  [ABORTED & ROLLED BACK] CANTEEN wallet missing; debit undone.");
                    return false;
                }
            }

            con.commit();
            System.out.println("  [COMMITTED] Atomically transferred ₹" + amount + " from HOSTEL to CANTEEN.");
            return true;
        } catch (SQLException ex) {
            con.rollback();
            System.out.println("  [EXCEPTION -> ROLLED BACK] " + ex.getMessage());
            return false;
        } finally {
            con.setAutoCommit(originalAutoCommit);
        }
    }

    private static void printWalletBalances(Connection con, String studentId) throws SQLException {
        String sql = "SELECT wallet_type, balance FROM wallets WHERE student_id = ? ORDER BY wallet_type";
        BigDecimal total = BigDecimal.ZERO;
        StringBuilder sb = new StringBuilder("  Balances -> ");
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("wallet_type");
                    BigDecimal bal = rs.getBigDecimal("balance");
                    total = total.add(bal);
                    sb.append(type).append(": ₹").append(bal).append(" | ");
                }
            }
        }
        sb.append("TOTAL: ₹").append(total);
        System.out.println(sb);
    }

    public static void main(String[] args) throws SQLException {
        initializeSchema();

        try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            // =====================================================================
            // PART 1: GENERIC Dao<BusPass, String> CRUD VERIFICATION
            // =====================================================================
            System.out.println("=== PART 1: BusPassDao CRUD OPERATIONS ===");
            try (Statement st = con.createStatement()) {
                st.executeUpdate("TRUNCATE TABLE bus_pass");
            }

            Dao<BusPass, String> passDao = new BusPassDao(con);
            passDao.insert(new BusPass("P4001", "192411009", "Fathi Muthu Johara M", 14, 4500.00));
            passDao.insert(new BusPass("P4002", "192411015", "Arun K", 12, 4200.00));
            System.out.println("After Insert (findAll) : " + passDao.findAll());

            BusPass p1 = passDao.findById("P4001");
            p1.setRouteNo(18);
            p1.setAmount(4800.00);
            passDao.update(p1);
            System.out.println("After Update (findById): " + passDao.findById("P4001"));

            passDao.delete("P4002");
            System.out.println("After Delete P4002     : " + passDao.findAll());

            // =====================================================================
            // PART 2: ATOMIC WALLET TRANSFER & CRASH ROLLBACK VERIFICATION
            // =====================================================================
            System.out.println("\n=== PART 2: ATOMIC ₹500 WALLET TRANSFER & CRASH ROLLBACK ===");
            String studentId = "192411009";
            try (Statement st = con.createStatement()) {
                st.executeUpdate("TRUNCATE TABLE wallets");
                st.executeUpdate("INSERT INTO wallets VALUES ('192411009', 'HOSTEL', 1200.00)");
                st.executeUpdate("INSERT INTO wallets VALUES ('192411009', 'CANTEEN', 300.00)");
            }

            System.out.println("[Initial State]");
            printWalletBalances(con, studentId);

            System.out.println("\n[Test A: Normal ₹500 Transfer]");
            transferWalletFunds(con, studentId, new BigDecimal("500.00"), false);
            printWalletBalances(con, studentId);

            System.out.println("\n[Test B: Simulated Crash Mid-Transfer (₹500)]");
            transferWalletFunds(con, studentId, new BigDecimal("500.00"), true);
            printWalletBalances(con, studentId);

            System.out.println("\n[Test C: Insufficient Balance Guard (₹1000 > ₹700)]");
            transferWalletFunds(con, studentId, new BigDecimal("1000.00"), false);
            printWalletBalances(con, studentId);
        }
    }
}