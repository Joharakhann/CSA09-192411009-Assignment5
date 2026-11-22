# Task B2 – Engineer an Adversarial Input

- **Student Name:** Fathi Muthu Johara M
- **Register Number:** 192411009
- **Course:** CSA09 – Programming in Java (Assignment 5 – CO5)
- **Bloom’s Level:** K4 – Analyse (5 Marks)

---

## 1. Adversarial SQL Injection Input & Observed Behaviour

### Crafted Input
* **`id`:** `admin' OR '1'='1' -- `
* **`pwd`:** `invalid_password`

### Observed Behaviour Comparison

| Login Implementation | Query Sent to MySQL | Actual Result | Security Outcome |
| :--- | :--- | :---: | :--- |
| **Unsafe (`Statement` + String Concatenation)** | `SELECT * FROM users WHERE id='admin' OR '1'='1' -- ' AND pwd='invalid_password'` | **`true` (Accepted)** | **Authentication Bypassed:** Logged in as the first table row (`admin`) without knowing any password. |
| **Safe (`PreparedStatement` with `?` Placeholders)** | `SELECT * FROM users WHERE id = ? AND pwd = ?` (parameter 1 bound strictly as literal `"admin' OR '1'='1' -- "`) | **`false` (Rejected)** | **Attack Blocked:** No user exists whose literal `id` equals `admin' OR '1'='1' -- `, so login is cleanly denied. |

### What This Reveals About the Original Design
* Concatenating untrusted user strings directly into SQL mixes **SQL grammar (control)** with **user values (data)**, allowing `'` and `-- ` to rewrite the query's syntax tree.
* `PreparedStatement` compiles the query structure *before* parameters are bound, forcing MySQL to treat user input strictly as an inert literal value.

---

## 2. Adversarial Damaged Bus-Pass File & Observed Behaviour

### Constructed Input File (`TASKB2/adversarial_passes.txt`)
```text
P2001,192411009,Fathi Muthu Johara M,14,4500.00
P2002,192411010,MissingFieldsRecord
P2003,192411011,Arun K,12,FOUR_THOUSAND

P2004,192411012,Divya S,7,3800.00