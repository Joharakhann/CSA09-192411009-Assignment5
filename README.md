# CSA09 – Programming in Java: Assignment 5 (Data Persistence Engineering)

- **Student Name:** Fathi Muthu Johara M
- **Register Number:** 192411009
- **Course:** CSA09 – Programming in Java (CO5 – Files & Databases)
- **Total Marks:** 50 Marks (Part A: 25 Marks | Part B: 25 Marks)

---

## Repository Structure

### Part A – Design & Algorithmic Analysis (25 Marks)
| Task | Title | Deliverable |
| :--- | :--- | :--- |
| **Task A1** | Choose Your Storage Layout (Text & Binary Bus-Pass Schema) | [`TASKA1/TaskA1_Design.md`](TASKA1/TaskA1_Design.md) |
| **Task A2** | Trace How Data Moves (Unbuffered vs. Buffered I/O Syscalls) | [`TASKA2/TaskA2_Design.md`](TASKA2/TaskA2_Design.md) |
| **Task A3** | Discover the Data-Access Design Pattern (`Dao<T, K>`) | [`TASKA3/TaskA3_Design.md`](TASKA3/TaskA3_Design.md) |
| **Task A4** | What Should Be Serialized? (Hostel Object Graph & `serialVersionUID`) | [`TASKA4/TaskA4_Design.md`](TASKA4/TaskA4_Design.md) |
| **Task A5** | Design for Correctness and Safety (Atomic Wallet Tx & SQLi Defense) | [`TASKA5/TaskA5_Design.md`](TASKA5/TaskA5_Design.md) |

### Part B – Java Implementation, Benchmarks & Verification (25 Marks)
| Task | Title | Deliverables |
| :--- | :--- | :--- |
| **Task B1** | Implement and Instrument the File Store (`PassStore` & Buffering/Growth Benchmarks) | [`TASKB1/`](TASKB1/) |
| **Task B2** | Engineer an Adversarial Input (SQL Injection & Damaged CSV Fault Isolation) | [`TASKB2/`](TASKB2/) |
| **Task B3** | Migrate File Records to MySQL & Measure JDBC Batching (`PassLoader` & Batch Benchmarks) | [`TASKB3/`](TASKB3/) |
| **Task B4** | Generic `BusPassDao` CRUD & Atomic ₹500 Wallet Transfer + Crash Rollback | [`TASKB4/`](TASKB4/) |