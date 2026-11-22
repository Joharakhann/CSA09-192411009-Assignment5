import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BusPassDao implements Dao<BusPass, String> {
    private final Connection con;

    public BusPassDao(Connection con) {
        this.con = con;
    }

    private BusPass mapRow(ResultSet rs) throws SQLException {
        return new BusPass(
                rs.getString("pass_id"),
                rs.getString("reg_no"),
                rs.getString("name"),
                rs.getInt("route_no"),
                rs.getDouble("amount")
        );
    }

    @Override
    public boolean insert(BusPass pass) throws SQLException {
        String sql = "INSERT INTO bus_pass (pass_id, reg_no, name, route_no, amount) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pass.getPassId());
            ps.setString(2, pass.getRegNo());
            ps.setString(3, pass.getName());
            ps.setInt(4, pass.getRouteNo());
            ps.setDouble(5, pass.getAmount());
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public BusPass findById(String passId) throws SQLException {
        String sql = "SELECT pass_id, reg_no, name, route_no, amount FROM bus_pass WHERE pass_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, passId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public List<BusPass> findAll() throws SQLException {
        List<BusPass> passes = new ArrayList<>();
        String sql = "SELECT pass_id, reg_no, name, route_no, amount FROM bus_pass ORDER BY pass_id";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                passes.add(mapRow(rs));
            }
        }
        return passes;
    }

    @Override
    public boolean update(BusPass pass) throws SQLException {
        String sql = "UPDATE bus_pass SET reg_no = ?, name = ?, route_no = ?, amount = ? WHERE pass_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pass.getRegNo());
            ps.setString(2, pass.getName());
            ps.setInt(3, pass.getRouteNo());
            ps.setDouble(4, pass.getAmount());
            ps.setString(5, pass.getPassId());
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public boolean delete(String passId) throws SQLException {
        String sql = "DELETE FROM bus_pass WHERE pass_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, passId);
            return ps.executeUpdate() == 1;
        }
    }
}