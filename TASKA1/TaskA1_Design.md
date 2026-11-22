# Task A1 – Design a File Format (Transport Office Bus-Pass Records)

- **Student Name:** Fathi Muthu Johara M
- **Register Number:** 192411009
- **Course:** CSA09 – Programming in Java (Assignment 5 – CO5)
- **Bloom’s Level:** K4 – Analyse (7 Marks)

---

## A1.1 – Represent the Record

### (a) Delimited Text File Layout (`bus_passes.txt`)
* **Field Order (7 fields per line, terminated by `\n`):**
  `passId|regNo|name|routeNo|boardingStop|validityDate|amountPaid`
* **Delimiter:** Pipe character (`|`)
* **Sample Record:**
  `BP2026-0001|192411009|Fathi Muthu Johara M|14|Tambaram Sanatorium|2026-12-31|4500.00`
* **Handling a Name (or Stop) Containing the Delimiter:**
  * Any field containing the delimiter (`|`), double quotes (`"`), or newline characters is enclosed in double quotes (`"..."`).
  * Any literal double quote inside the field is escaped by doubling it (`""`).
  * **Example:** `BP2026-0002|192411010|"Raja|Kumar"|14|"Gate 2|Main Road"|2026-12-31|4500.00`
  * When reading, a quote-aware parser tracks an `inQuotes` boolean flag and splits fields only on `|` characters encountered when `inQuotes` is `FALSE`.

### (b) Binary File Layout (`bus_passes.dat` via `DataOutputStream`)
Each record occupies a fixed size of **120 bytes**, eliminating delimiters altogether:

| Order | Field Name | `DataOutputStream` Method | Fixed Size | Format & Padding Rule |
| :---: | :--- | :--- | :---: | :--- |
| 1 | `passId` | `writeBytes(String)` | 12 bytes | ASCII, right-padded with spaces (`"BP2026-0001 "`) |
| 2 | `regNo` | `writeBytes(String)` | 12 bytes | ASCII, right-padded with spaces (`"192411009   "`) |
| 3 | `name` | `write(byte[])` (UTF-8) | 40 bytes | Truncated or space-padded to 40 bytes |
| 4 | `routeNo` | `writeInt(int)` | 4 bytes | 32-bit signed integer |
| 5 | `boardingStop` | `write(byte[])` (UTF-8) | 32 bytes | Truncated or space-padded to 32 bytes |
| 6 | `validityDate` | `writeLong(long)` | 8 bytes | Epoch milliseconds representing `YYYY-MM-DD` |
| 7 | `amountPaid` | `writeDouble(double)` | 8 bytes | 64-bit IEEE-754 floating-point amount |
| — | `endMarker` | `writeInt(0x42504153)` | 4 bytes | Magic bytes (`"BPAS"`) verifying full record write |
| **Total** | **1 Pass Record** | — | **120 bytes** | $8{,}000 \times 120\text{ B} = 960\text{ KB/year}$ |

* **Handling Delimiters in Binary Format:** Because fields are separated by fixed byte widths rather than delimiter characters, names containing `|` or `,` are written directly as raw bytes without escaping.

---

## A1.2 – Design the Operations (SIMATS Algorithmic Pseudocode)

### Operation (i): Add a New Bus Pass
```text
ALGORITHM AddBusPass(filePath, passId, regNo, name, routeNo, boardingStop, validityDate, amountPaid)
INPUT:  filePath (STRING), passId (STRING), regNo (STRING), name (STRING),
        routeNo (INTEGER), boardingStop (STRING), validityDate (STRING), amountPaid (REAL)
OUTPUT: isSaved (BOOLEAN)

BEGIN
    DECLARE safeName AS STRING <- QuoteIfContainsDelimiter(name, "|")
    DECLARE safeStop AS STRING <- QuoteIfContainsDelimiter(boardingStop, "|")
    DECLARE recordLine AS STRING

    SET recordLine <- passId + "|" + regNo + "|" + safeName + "|" +
                      TO_STRING(routeNo) + "|" + safeStop + "|" +
                      validityDate + "|" + TO_STRING(amountPaid)

    TRY
        OPEN fileWriter ON filePath IN APPEND_CREATE_MODE
        WRAP bufferedWriter AROUND fileWriter
        WRITE_LINE(bufferedWriter, recordLine)
        FLUSH(bufferedWriter)
        CLOSE(bufferedWriter)
        RETURN TRUE
    CATCH IOFailure
        PRINT "Error: Unable to append bus pass record."
        RETURN FALSE
    ENDTRY
END