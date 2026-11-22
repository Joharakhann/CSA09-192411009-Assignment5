# Task A3 – Discover the Data-Access Design Pattern

- **Student Name:** Fathi Muthu Johara M
- **Register Number:** 192411009
- **Course:** CSA09 – Programming in Java (Assignment 5 – CO5)
- **Bloom’s Level:** K4 – Analyse (5 Marks)

---

## A3.1 – Identify the Common Pattern

Across `StudentDB`, `CourseDB`, and `MarksDB`, the persistence workflow separates into invariant JDBC infrastructure steps and entity-specific mapping logic:

### 1. Repeated (Common) Steps Across All Three Classes
1. **Connection Acquisition:** Requesting a JDBC `Connection` from `DriverManager` or a `DataSource` pool.
2. **Statement Preparation & Parameter Binding:** Creating a `PreparedStatement` and setting `?` placeholders.
3. **Query Execution:** Calling `executeUpdate()` (for `INSERT`, `UPDATE`, `DELETE`) or `executeQuery()` (for `SELECT`).
4. **Result Iteration & Resource Cleanup:** Looping through `ResultSet.next()` and closing `ResultSet`, `PreparedStatement`, and `Connection` inside `try-with-resources` / `finally` blocks so database cursors never leak.

### 2. Varying (Entity-Specific) Parts
1. **Table & Column Identifiers:** Each class targets a different table (`student`, `course`, `marks`) and column list.
2. **SQL Statement Strings:** Custom `INSERT`, `SELECT`, `UPDATE`, and `DELETE` templates per entity.
3. **Parameter Types & Order:** Binding entity fields with matching JDBC setters (`setString`, `setInt`, `setDouble`).
4. **Object Hydration (Row-to-Object Mapping):** Constructing a `Student`, `Course`, or `Marks` object from the columns of each `ResultSet` row.

---

## A3.2 – Design a Generic Data-Access Interface (SIMATS Algorithmic Pseudocode)

### Generic Interface `Dao<T, K>`
```text
INTERFACE Dao<T, K>
    OPERATION insert(entity : T) : BOOLEAN
    OPERATION findById(key : K) : T OR NULL
    OPERATION findAll() : LIST OF T
    OPERATION update(entity : T) : BOOLEAN
    OPERATION delete(key : K) : BOOLEAN
END INTERFACE