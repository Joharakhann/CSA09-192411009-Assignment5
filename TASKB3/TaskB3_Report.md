# Task B3 – Migrate File Records to MySQL & Measure JDBC Batching

- **Student Name:** Fathi Muthu Johara M
- **Register Number:** 192411009
- **Course:** CSA09 – Programming in Java (Assignment 5 – CO5)
- **Bloom’s Level:** K4 – Analyse (6 Marks)

---

## 1. Benchmark Results (`1,000`, `5,000`, and `10,000` Records)

| Record Count ($n$) | `Statement` (Auto-Commit) | `PreparedStatement` (1 Tx, Row-by-Row) | `PreparedStatement` Batch (`size = 500`) | Batch + `rewriteBatchedStatements=true` | Total Speedup (Col 2 vs Col 5) |
| :---: | :---: | :---: | :---: | :---: | :---: |
| **1,000** | *<fill ms>* | *<fill ms>* | *<fill ms>* | *<fill ms>* | *<fill x>* |
| **5,000** | *<fill ms>* | *<fill ms>* | *<fill ms>* | *<fill ms>* | *<fill x>* |
| **10,000** | *<fill ms>* | *<fill ms>* | *<fill ms>* | *<fill ms>* | *<fill x>* |

---

## 2. Engineering Explanation of Observed Differences

1. **Auto-Commit Redo-Log Flushing (`Statement` Auto-Commit vs. Single Transaction):**
   * With `autoCommit = true`, MySQL InnoDB flushes its transaction redo log to disk (`fsync`) after **every single `INSERT`** ($10{,}000$ disk syncs for $10{,}000$ rows), dominating execution time. Wrapping all inserts inside `con.setAutoCommit(false)` reduces disk flushes from $n$ down to **$1$ `COMMIT`**.
2. **SQL Parsing & Query-Plan Reuse (`Statement` vs. `PreparedStatement`):**
   * Dynamic `Statement` strings force MySQL to lex, parse, and optimize $10{,}000$ distinct SQL strings. `PreparedStatement` compiles the parameterized execution plan once and reuses it across all $n$ rows.
3. **Network Round-Trip Reduction (`executeUpdate()` vs. `executeBatch()` + `rewriteBatchedStatements=true`):**
   * Calling `executeUpdate()` per row still incurs $n$ client-server TCP round-trip times (RTTs).
   * Grouping rows into batches of $B = 500$ via `addBatch()` / `executeBatch()` reduces network round-trips from $n$ to $\lceil n / B \rceil$ ($20$ round-trips for $10{,}000$ rows).
   * Enabling `rewriteBatchedStatements=true` in MySQL Connector/J rewrites each 500-statement batch into a single multi-row SQL packet (`INSERT INTO bus_pass VALUES (...),(...),...`), slashing server-side execution overhead even further.

---

## 3. Atomicity & Fault-Tolerance Verification

* **File-Level Fault Tolerance:** When `PassLoader.loadFromFile()` encounters malformed CSV lines (`P3002,CORRUPT_ROW_ONLY_TWO_FIELDS`), it logs and skips the damaged line without aborting valid rows (`P3001`, `P3003`, `P3004`).
* **Database-Level Atomicity (`con.rollback()`):** In `verifyAtomicRollbackOnBatchFailure()`, Batch 1 executes `500` valid inserts and Batch 2 triggers a duplicate primary key `SQLException` on `P1`. Because `autoCommit` is disabled, `catch (SQLException)` invokes `con.rollback()`, rolling back Batch 1 as well and leaving **`0` rows** in `bus_pass` (no half-loaded state).

---

## 4. Alternative Concept and Complexity Analysis (MySQL `LOAD DATA LOCAL INFILE` vs. JDBC `executeBatch()`)

### Alternative Solution (`LOAD DATA LOCAL INFILE` Bulk Streaming)
```java
String bulkSql = "LOAD DATA LOCAL INFILE 'passes.csv' INTO TABLE bus_pass " +
                 "FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n' " +
                 "(pass_id, reg_no, name, route_no, amount)";
try (Statement st = con.createStatement()) {
    st.execute(bulkSql);
}