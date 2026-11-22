# Task A4 – What Should Be Serialized? (Hostel Object-Graph Persistence)

- **Student Name:** Fathi Muthu Johara M
- **Register Number:** 192411009
- **Course:** CSA09 – Programming in Java (Assignment 5 – CO5)
- **Bloom’s Level:** K4 – Analyse (4 Marks)

---

## A4.1 – Decide What to Persist

### 1. Fields Marked `transient` and Justification
In the `Resident` entity (`name`, `registerNumber`, `password`, `dbConnection`), two fields must be declared `transient` before serializing the `Hostel` state:

| Field in `Resident` | Persist or `transient`? | Engineering & Security Justification |
| :--- | :---: | :--- |
| `name` | **Persist** | Core domain attribute needed to restore resident identity. |
| `registerNumber` | **Persist** | Unique student identifier needed to restore room allocation. |
| `password` | **`transient`** | **Security & Confidentiality:** Writing raw passwords into a binary snapshot file exposes user credentials on disk to anyone with file-system read access. Marking `password` as `transient` omits it from the byte stream (deserializing as `null`) so authentication is verified securely against salted hashes in the database. |
| `dbConnection` | **`transient`** | **Runtime Resource / Non-Serializable State:** A live JDBC `Connection` wraps active OS TCP sockets, database session state, and driver-specific native handles that do not implement `Serializable`. Serializing it without `transient` immediately throws `java.io.NotSerializableException` and aborts the entire snapshot. On deserialization, `dbConnection` restores as `null` and must be re-acquired from a connection pool. |

---

### 2. Object-Graph Traversal Behavior During Serialization
For `Hostel` serialization to succeed, `Hostel`, `Room`, and `Resident` must all implement the `java.io.Serializable` marker interface.

```text
[Hostel] (Serializable)
   │
   └── rooms : List<Room> (Serializable)
         ├── [Room #101] (Serializable)
         │     └── residents : List<Resident>
         │           ├── [Resident A] ──► name (persisted), registerNumber (persisted)
         │           │                  ──► password (transient -> omitted), dbConnection (transient -> omitted)
         │           └── [Resident B] ──► name (persisted), registerNumber (persisted)
         │                              ──► password (transient -> omitted), dbConnection (transient -> omitted)
         └── [Room #102] (Serializable)
               └── ...