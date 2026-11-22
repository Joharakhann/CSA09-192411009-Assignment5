# Task A2 – Don’t Read What You Don’t Need (Buffered I/O Design)

- **Student Name:** Fathi Muthu Johara M
- **Register Number:** 192411009
- **Course:** CSA09 – Programming in Java (Assignment 5 – CO5)
- **Bloom’s Level:** K4 – Analyse (5 Marks)

---

## A2.1 – Predict the Cost

### 1. Predicted Number of `read()` Calls
* **For a 50 MiB file ($50 \times 1{,}024 \times 1{,}024 = 52{,}428{,}800\text{ bytes}$):**
  * `FileInputStream.read()` reads **1 byte per call** and requires **1 final call** that returns `-1` to signal End-of-File (EOF).
  * **Total `read()` calls:** $52{,}428{,}800 + 1 = \mathbf{52{,}428{,}801\text{ calls}}$ (or $\mathbf{50{,}000{,}001\text{ calls}}$ if measured in decimal $50 \times 10^6$ bytes).

### 2. Why It Is Slow
* **JNI & Operating System System-Call Overhead:** `FileInputStream.read()` is a native method; every single call crosses the Java Native Interface (JNI) boundary and invokes an OS kernel `read()` system call.
* **Context Switching & Cache Invalidation:** Executing over 52.4 million user-mode $\leftrightarrow$ kernel-mode context switches triggers repeated CPU pipeline flushes, Translation Lookaside Buffer (TLB) / L1 cache misses, and kernel file-descriptor lock acquisition for every individual byte, making syscall overhead dominate actual disk transfer time.

---

## A2.2 – Redesign Using Buffering

### SIMATS Algorithmic Pseudocode (`CountLinesBuffered`)
```text
ALGORITHM CountLinesBuffered(filePath)
INPUT:  filePath (STRING)
OUTPUT: lineCount (LONG)

BEGIN
    DECLARE lineCount AS LONG <- 0
    DECLARE bufferSize AS INTEGER <- 8192      // 8 KB user-space byte buffer
    DECLARE buffer AS ARRAY[bufferSize] OF BYTE
    DECLARE bytesRead AS INTEGER
    DECLARE lastByte AS BYTE <- 0
    DECLARE hasData AS BOOLEAN <- FALSE

    OPEN fileStream ON filePath IN BINARY_READ_MODE
    TRY
        WHILE (bytesRead <- READ_BYTES(fileStream, buffer, 0, bufferSize)) != -1 DO
            IF bytesRead > 0 THEN
                SET hasData <- TRUE
            ENDIF

            FOR i <- 0 TO bytesRead - 1 DO
                SET lastByte <- buffer[i]
                IF lastByte == NEWLINE_ASCII (0x0A) THEN
                    SET lineCount <- lineCount + 1
                ENDIF
            ENDFOR
        ENDWHILE

        // Count the final line if the file is non-empty and lacks a trailing '\n'
        IF hasData == TRUE AND lastByte != NEWLINE_ASCII (0x0A) THEN
            SET lineCount <- lineCount + 1
        ENDIF
    FINALLY
        CLOSE fileStream
    ENDTRY

    RETURN lineCount
END