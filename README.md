# Student Result Management System

A Java Swing desktop application (SQLite via JDBC) to manage students, courses and exam results.

* **Dashboard** - totals, average score, pass rate, grade-distribution chart, average score per course, latest results
* **Students / Courses** - searchable tables with an "Add" button
* **Student login** - students see only their own results (grades chart, average, table) and can change their password
* **Results** - search by roll number (or show everyone's), add results with a live grade preview
* Grades are calculated automatically: 90+ A+, 80+ A, 70+ B+, 60+ B, 50+ C, 40+ D, otherwise F

## Folder structure

```
StudentResultSystem/
├── src/
│   ├── model/   Student, Course, Result, User
│   ├── dao/     DatabaseConnection + StudentDAO, CourseDAO, ResultDAO, UserDAO (all SQL lives here)
│   └── ui/      LoginFrame, MainDashboard (sidebar), pages (Dashboard/Students/Courses/Results),
│                MyResultsPanel / MyAccountPanel (student side), add-dialogs, shared Theme / BarChart / FormDialog / ListPage, ConsoleMenu (text version)
├── test/        GradeCalculationTest (no database needed)
├── db/          schema.sql (tables + a first admin user), SQLite database file
└── lib/         SQLite JDBC driver
```

## Setup

1. Install JDK 17+ and [DB Browser for SQLite](https://sqlitebrowser.org/).
2. In DB Browser, create `db/student_result_system.db`. Use **Execute SQL** to run `db/schema.sql`, then click **Write Changes**. The script creates the tables and the initial admin login.
3. The SQLite JDBC driver is included as `lib/sqlite-jdbc-3.49.1.0.jar`.
4. Compile and run from the `StudentResultSystem` project folder:

**Linux / macOS**
```
javac -d out -cp "lib/*" $(find src -name "*.java")
java -cp "out:lib/*" ui.LoginFrame
```
**Windows (PowerShell)**
```
javac -d out -cp "lib/*" (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp "out;lib/*" ui.LoginFrame
```

Text-menu version: `java -cp "out;lib/*" ui.ConsoleMenu` on Windows, or `java -cp "out:lib/*" ui.ConsoleMenu` on Linux/macOS.

By default, the app uses `db/student_result_system.db` relative to the project folder. Open that same file in DB Browser to view or edit app data. To use a different file, set `DB_URL` to a SQLite JDBC URL such as `jdbc:sqlite:C:/path/to/student_result_system.db`.

> The project previously used MySQL. A MySQL database cannot be opened by DB Browser for SQLite; this version uses a separate SQLite database file.

## Logins

| Who | Username | Password |
|-----|----------|----------|
| Admin | `admin` (from `schema.sql`) | `admin123` - change it |
| Student | their roll number | their date of birth as `ddMMyyyy` (2004-12-26 -> `26122004`) |

* Adding a student in the app creates their login automatically (the password is shown once to the admin).
* For students that already exist, open **Students -> Create Logins**. **Reset Password** (select a row first) restores the date-of-birth password.
* Students change their password under **My Account**.
* A user whose `role` is `STUDENT` gets the student screens; any other role gets the admin screens, as before.

## Test

```
javac -d out -cp "lib/*" $(find src test -name "*.java")
java -cp out GradeCalculationTest
```
