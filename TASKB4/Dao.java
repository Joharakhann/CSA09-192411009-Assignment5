import java.sql.SQLException;
import java.util.List;

public interface Dao<T, K> {
    boolean insert(T entity) throws SQLException;
    T findById(K key) throws SQLException;
    List<T> findAll() throws SQLException;
    boolean update(T entity) throws SQLException;
    boolean delete(K key) throws SQLException;
}