import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TaskB1Benchmark {

    // Full Bus-Pass Store implementing A1 text design (add, find, listByRoute + fault tolerance)
    static class FullBusPassStore {
        private final Path file;

        FullBusPassStore(Path file) {
            this.file = file;
        }

        public void add(String passId, String regNo, String name, int routeNo,
                        String boardingStop, String validityDate, double amountPaid) throws IOException {
            String safeName = escapePipe(name);
            String safeStop = escapePipe(boardingStop);
            String record = String.join("|", passId, regNo, safeName,
                    String.valueOf(routeNo), safeStop, validityDate, String.valueOf(amountPaid));

            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(record);
                writer.newLine();
            }
        }

        public String[] find(String targetPassId) throws IOException {
            if (Files.notExists(file)) {
                return null;
            }
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                int lineNo = 0;
                while ((line = reader.readLine()) != null) {
                    lineNo++;
                    String[] fields = parsePipeLine(line);
                    if (!isValidRecord(fields)) {
                        System.out.println("  [WARN] Skipping damaged line #" + lineNo + ": " + line);
                        continue;
                    }
                    if (fields[0].equals(targetPassId)) {
                        return fields;
                    }
                }
            }
            return null;
        }

        public List<String[]> listByRoute(int targetRouteNo) throws IOException {
            List<String[]> matches = new ArrayList<>();
            if (Files.notExists(file)) {
                return matches;
            }
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] fields = parsePipeLine(line);
                    if (!isValidRecord(fields)) {
                        continue;
                    }
                    if (Integer.parseInt(fields[3]) == targetRouteNo) {
                        matches.add(fields);
                    }
                }
            }
            return matches;
        }

        private static boolean isValidRecord(String[] fields) {
            if (fields.length != 7) {
                return false;
            }
            try {
                Integer.parseInt(fields[3]);
                double amt = Double.parseDouble(fields[6]);
                return amt >= 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        private static String escapePipe(String value) {
            if (value.contains("|") || value.contains("\"")) {
                return "\"" + value.replace("\"", "\"\"") + "\"";
            }
            return value;
        }

        private static String[] parsePipeLine(String line) {
            List<String> tokens = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inQuotes = false;
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '"') {
                    if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = !inQuotes;
                    }
                } else if (c == '|' && !inQuotes) {
                    tokens.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
            tokens.add(current.toString());
            return tokens.toArray(new String[0]);
        }
    }

    // B1.3: Unbuffered byte-by-byte line counter (Task A2.1)
    public static long countLinesUnbuffered(Path path) throws IOException {
        long lines = 0;
        int b;
        int lastByte = -1;
        try (InputStream in = new FileInputStream(path.toFile())) {
            while ((b = in.read()) != -1) {
                lastByte = b;
                if (b == '\n') {
                    lines++;
                }
            }
        }
        if (lastByte != -1 && lastByte != '\n') {
            lines++;
        }
        return lines;
    }

    // B1.3: Buffered 8 KB chunk line counter (Task A2.2)
    public static long countLinesBuffered(Path path) throws IOException {
        long lines = 0;
        byte[] buffer = new byte[8192];
        int bytesRead;
        byte lastByte = 0;
        boolean hasData = false;
        try (InputStream in = new BufferedInputStream(new FileInputStream(path.toFile()), 8192)) {
            while ((bytesRead = in.read(buffer)) != -1) {
                if (bytesRead > 0) {
                    hasData = true;
                }
                for (int i = 0; i < bytesRead; i++) {
                    lastByte = buffer[i];
                    if (lastByte == '\n') {
                        lines++;
                    }
                }
            }
        }
        if (hasData && lastByte != '\n') {
            lines++;
        }
        return lines;
    }

    private static Path generateLogFile(int targetMegabytes) throws IOException {
        Path tempLog = Files.createTempFile("attendance_" + targetMegabytes + "MB_", ".log");
        long targetBytes = (long) targetMegabytes * 1024 * 1024;
        String sampleLine = "2026-11-22 08:30:15 | REG:192411009 | ROUTE:14 | STOP:Tambaram | STATUS:BOARDED\n";
        byte[] lineBytes = sampleLine.getBytes(StandardCharsets.UTF_8);

        long written = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(tempLog, StandardCharsets.UTF_8)) {
            while (written < targetBytes) {
                writer.write(sampleLine);
                written += lineBytes.length;
            }
        }
        return tempLog;
    }

    public static void main(String[] args) throws IOException {
        // =========================================================================
        // B1.1 – VALIDATE THE IMPLEMENTATION (Normal, Non-Existent, Damaged Line)
        // =========================================================================
        System.out.println("=== B1.1: BUS-PASS FILE STORE VALIDATION ===");
        Path validationFile = Files.createTempFile("b1_validation_", ".txt");
        FullBusPassStore fullStore = new FullBusPassStore(validationFile);

        fullStore.add("BP-1001", "192411009", "Fathi Muthu Johara M", 14, "Tambaram", "2026-12-31", 4500.00);
        // Inject a damaged line with missing fields
        Files.writeString(validationFile, "BP-1002|CORRUPT_INCOMPLETE_RECORD\n", StandardOpenOption.APPEND);
        fullStore.add("BP-1003", "192411015", "Raja|Kumar", 14, "Chromepet", "2026-12-31", 4500.00);
        fullStore.add("BP-1004", "192411022", "Divya S", 7, "Guindy", "2026-12-31", 3800.00);

        String[] foundNormal = fullStore.find("BP-1003");
        String[] foundMissing = fullStore.find("BP-9999");
        String[] foundDamaged = fullStore.find("BP-1002");
        List<String[]> route14Passes = fullStore.listByRoute(14);

        System.out.println("Test 1 (Normal Find BP-1003)   : " + Arrays.toString(foundNormal));
        System.out.println("Test 2 (Non-Existent BP-9999)  : " + Arrays.toString(foundMissing));
        System.out.println("Test 3 (Damaged Line BP-1002)  : " + Arrays.toString(foundDamaged));
        System.out.println("Test 4 (List Route 14 Count)   : " + route14Passes.size() + " valid passes found");
        Files.deleteIfExists(validationFile);

        // =========================================================================
        // B1.3 – MEASURE THE EFFECT OF BUFFERING (1 MB, 10 MB, 50 MB)
        // =========================================================================
        System.out.println("\n=== B1.3: BUFFERED vs UNBUFFERED LINE COUNTING ===");
        System.out.println("(Running 50 MB unbuffered scan takes ~30-90s due to 52.4M OS syscalls; please wait...)");
        System.out.printf("%-10s | %-12s | %-18s | %-18s | %-10s%n",
                "File Size", "Lines", "Unbuffered (ms)", "Buffered 8KB (ms)", "Speedup");
        System.out.println("-----------------------------------------------------------------------------");

        int[] sizesMB = {1, 10, 50};
        for (int mb : sizesMB) {
            Path logFile = generateLogFile(mb);

            long t1Start = System.nanoTime();
            long linesUnbuf = countLinesUnbuffered(logFile);
            double unbufMs = (System.nanoTime() - t1Start) / 1_000_000.0;

            long t2Start = System.nanoTime();
            long linesBuf = countLinesBuffered(logFile);
            double bufMs = (System.nanoTime() - t2Start) / 1_000_000.0;

            double speedup = unbufMs / bufMs;
            System.out.printf("%-10s | %-12d | %-18.2f | %-18.2f | %-9.1fx%n",
                    mb + " MB", linesBuf, unbufMs, bufMs, speedup);

            if (linesUnbuf != linesBuf) {
                throw new IllegalStateException("Line count mismatch!");
            }
            Files.deleteIfExists(logFile);
        }

        // =========================================================================
        // B1.4 – MEASURE GROWTH (1,000 to 100,000 RECORDS)
        // =========================================================================
        System.out.println("\n=== B1.4: LINEAR SEARCH GROWTH (1,000 TO 100,000 RECORDS) ===");
        System.out.printf("%-15s | %-22s | %-22s%n",
                "Record Count", "Avg Find Time (ms)", "Time per Record (ns)");
        System.out.println("------------------------------------------------------------------");

        int[] recordCounts = {1_000, 10_000, 50_000, 100_000};
        for (int count : recordCounts) {
            Path growthFile = Files.createTempFile("passes_growth_" + count + "_", ".csv");
            try (BufferedWriter writer = Files.newBufferedWriter(growthFile, StandardCharsets.UTF_8)) {
                for (int i = 1; i <= count; i++) {
                    writer.write("P" + i + ",192411009,Student_" + i + ",14,4500.0\n");
                }
            }

            PassStore store = new PassStore(growthFile);
            String targetWorstCaseId = "P" + count; // Tail record (worst-case scan)

            // Warm up once, then average 10 lookups
            store.find(targetWorstCaseId);
            int trials = 10;
            long searchStart = System.nanoTime();
            for (int t = 0; t < trials; t++) {
                store.find(targetWorstCaseId);
            }
            double avgFindMs = ((System.nanoTime() - searchStart) / 1_000_000.0) / trials;
            double nsPerRecord = (avgFindMs * 1_000_000.0) / count;

            System.out.printf("%-15d | %-22.3f | %-22.2f%n", count, avgFindMs, nsPerRecord);
            Files.deleteIfExists(growthFile);
        }
    }
}