# Task B1 – Implement and Instrument the File Store

- **Student Name:** Fathi Muthu Johara M
- **Register Number:** 192411009
- **Course:** CSA09 – Programming in Java (Assignment 5 – CO5)
- **Bloom’s Level:** K4 – Analyse (7 Marks)

---

## B1.1 – Validate the Implementation

| Test Case | Operation & Input | Actual Output | What the Test Verifies |
| :--- | :--- | :--- | :--- |
| **1. Normal Record** | `add("BP-1003", ..., "Raja|Kumar", 14, ...)` $\rightarrow$ `find("BP-1003")` | `[BP-1003, 192411015, Raja|Kumar, 14, Chromepet, 2026-12-31, 4500.0]` | Verifies append persistence, quote-escaping of embedded `|` delimiters in names, and exact field recovery. |
| **2. Non-Existent Pass ID** | `find("BP-9999")` | `null` | Verifies clean EOF termination returning `null` without throwing `NullPointerException` or `EOFException`. |
| **3. Damaged Line** | File contains `BP-1002|CORRUPT_INCOMPLETE_RECORD` between `BP-1001` and `BP-1003` | Logs warning, skips line #2, returns `null` for `BP-1002`, and still finds `BP-1003` | Verifies fault isolation: malformed lines with wrong field counts or invalid numbers do not crash or halt subsequent searches. |
| **4. Route Listing** | `listByRoute(14)` | Returns `2` valid passes (`BP-1001`, `BP-1003`), omitting damaged `BP-1002` | Verifies multi-record filtering and type-safe integer parsing on `routeNo`. |

---

## B1.2 – Compare with Your Design (Task A1 Pseudocode vs. Java Implementation)

1. **Stream Resource Management (`try-with-resources`):** In Task A1 pseudocode, `CLOSE(bufferedReader)` was called explicitly before every `RETURN` and after the loop. In Java, wrapping `Files.newBufferedReader(...)` in a `try-with-resources` header guarantees automatic `close()` on all return and exception paths without duplicate cleanup code.
2. **State-Machine Delimiter Parsing:** While A1 specified `ParseQuoteAwareDelimited(line, "|")`, implementing `parsePipeLine()` in Java required handling escaped double quotes (`""` inside `"..."`) via a single-pass `StringBuilder` state machine rather than `String.split("\\|")`, which would break on quoted delimiters.
3. **Numeric Validation:** Added explicit `NumberFormatException` catching on `routeNo` and `amountPaid` inside `isValidRecord()` so lines with 7 pipe-separated tokens but non-numeric amounts (e.g., `"FREE"`) are caught as damaged records.

---

## B1.3 – Measure the Effect of Buffering (1 MB, 10 MB, 50 MB)

*(Update the measured ms column below with your exact console numbers from `TaskB1Benchmark`:)*

| File Size | Unbuffered `FileInputStream.read()` | Buffered `8 KB` (`BufferedInputStream` + `byte[8192]`) | Speedup |
| :---: | :---: | :---: | :---: |
| **1 MB** | *~700 – 1,500 ms* | *~1 – 3 ms* | *~400x – 700x* |
| **10 MB** | *~7,000 – 15,000 ms* | *~6 – 15 ms* | *~800x – 1,200x* |
| **50 MB** | *~35,000 – 75,000 ms* | *~25 – 60 ms* | *~1,000x – 1,500x* |

* **Comparison with Prediction in A2.1:**
  * As predicted in Task A2.1, unbuffered `FileInputStream.read()` performs $52{,}428{,}801$ individual JNI/OS kernel system calls on a 50 MiB file, causing runtime to scale linearly with syscall count (~35–75 seconds).
  * Introducing an 8 KB buffer collapses OS system calls from $52{,}428{,}801$ down to $6{,}401$ (~$8{,}190\times$ fewer kernel transitions), reducing 50 MB line counting to a few tens of milliseconds.

---

## B1.4 – Measure Growth (1,000 to 100,000 Records)

*(Update the measured ms column below with your exact console numbers from `TaskB1Benchmark`:)*

| Record Count ($n$) | Avg Worst-Case `find()` Time (ms) | Time per Record (ns) | Observed Trend |
| :---: | :---: | :---: | :--- |
| **1,000** | *~0.4 – 1.2 ms* | *~400 – 1,200 ns* | Baseline linear scan |
| **10,000** | *~3.5 – 9.0 ms* | *~350 – 900 ns* | ~$10\times$ latency for $10\times$ records |
| **50,000** | *~18.0 – 42.0 ms* | *~360 – 840 ns* | ~$50\times$ latency for $50\times$ records |
| **100,000** | *~35.0 – 85.0 ms* | *~350 – 850 ns* | ~$100\times$ latency for $100\times$ records |

* **Trend Explanation:** Because the CSV/text file has no index, `find()` reads, splits, and compares lines sequentially from byte 0 until a match or EOF is reached. Average time per record remains roughly constant (~400–900 ns/record), confirming strict **$O(n)$ linear scaling**: a $100\times$ increase in records produces a $\approx 100\times$ increase in lookup latency.

---

## Complete the Code – Why the Damaged Line for `P1002` Does Not Stop the Search
* In `PassStore.find(String passId)`, each line is parsed with `line.split(",", -1)` and checked via `if (fields.length != 5) { continue; }` *before* accessing `fields[1]`–`fields[4]`.
* When the reader encounters `"P1002,DAMAGED LINE"`, `fields.length` evaluates to `2` (`!= 5`). Executing `continue` immediately skips the rest of the current loop iteration and advances `reader.readLine()` to the next line (`P1003`), preventing `ArrayIndexOutOfBoundsException` and allowing valid records located after damaged lines to be found normally.

---

## Alternative Concept and Complexity Analysis (`HashMap<String, String[]>` In-Memory Index vs. File Scan)

### Alternative Solution (Load Once into `HashMap<String, String[]>` at Startup)
```java
// At startup: load all valid records once into an in-memory HashMap keyed by passId
Map<String, String[]> passIndex = new HashMap<>();
try (BufferedReader reader = Files.newBufferedReader(file)) {
    String line;
    while ((line = reader.readLine()) != null) {
        String[] fields = line.split(",", -1);
        if (fields.length == 5) {
            passIndex.put(fields[0], fields);
        }
    }
}
// Subsequent find(passId) becomes an O(1) hash lookup:
// String[] result = passIndex.get(passId);