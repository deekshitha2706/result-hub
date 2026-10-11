# Student Result Management System

A Java student result management system with both a Swing desktop app (SQLite) and a responsive web app (PostgreSQL).

* **Dashboard** - totals, average score, pass rate, grade-distribution chart, average score per course, latest results
* **Students / Courses** - searchable tables with an "Add" button
* **Student login** - students see only their own results (grades chart, average, table) and can change their password
* **Results** - search by roll number (or show everyone's), add results with a live grade preview
* Grades are calculated automatically: 90+ A+, 80+ A, 70+ B+, 60+ B, 50+ C, 40+ D, otherwise F

The project includes both the original desktop app and a responsive web app. The website uses PostgreSQL, Spring Boot, and role-protected administrator/student pages.

## Deploy the website to Render

1. Push this repository to GitHub.
2. Sign in to [Render](https://dashboard.render.com/) and choose **New → Blueprint**.
3. Connect the `result-hub` GitHub repository. Render reads [`render.yaml`](./render.yaml) and prepares the web service and PostgreSQL database. Both are configured on the Free plan for a demo.
4. When prompted for `BOOTSTRAP_ADMIN_PASSWORD`, enter a unique password of at least 12 characters. Render stores this as a secret; do not commit the password.
5. Deploy the Blueprint. When the service is live, open its `onrender.com` URL and sign in with username `admin` and the password you set.

The hosted website starts with a **new, empty cloud database**. It does not upload the local SQLite database or student records. Add courses and students from the admin pages. New student logins use their roll number and initial password (date of birth as `ddMMyyyy`); students can change it under **My Account**.

**Free-plan limitations:** the web service sleeps after inactivity, so the first visit can take about a minute to wake up. Render's free PostgreSQL database expires 30 days after creation and is deleted after the additional recovery period unless upgraded. Free services are for demos, not production; upgrade the database before storing records you need to keep.

### Run the website locally

Install JDK 17+ and Maven, then run in PowerShell from the project folder:

```powershell
$env:BOOTSTRAP_ADMIN_PASSWORD = "choose-a-unique-password-at-least-12-characters"
mvn spring-boot:run
```

The local web app uses an in-memory PostgreSQL-compatible H2 database and creates an `admin` account on first start. Its data is temporary. To use PostgreSQL, set `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, and `DB_PASSWORD` before starting.

## Folder structure

```
StudentResultSystem/
├── src/
│   ├── dao/, model/, ui/, security/   Desktop app and shared password hashing
│   ├── web/                           Spring Boot controllers, security and JDBC service
│   └── main/resources/                Web templates, CSS, schema and app settings
├── test/                              Desktop grading check and web integration tests
├── db/                                SQLite schema and local database file
├── lib/                               SQLite JDBC driver for the desktop app
├── Dockerfile, render.yaml             Render deployment
└── pom.xml                             Maven web build
```

## Setup

1. Install JDK 17+ and [DB Browser for SQLite](https://sqlitebrowser.org/).
2. In DB Browser, create `db/student_result_system.db`. Use **Execute SQL** to run `db/schema.sql`, then click **Write Changes**. The script creates the tables and the initial admin login.
3. The SQLite JDBC driver is included as `lib/sqlite-jdbc-3.49.1.0.jar`.
4. Compile and run the **desktop app** from the `StudentResultSystem` project folder:

**Linux / macOS**
```
javac -d out -cp "lib/*" $(find src/dao src/model src/ui src/security -name "*.java")
java -cp "out:lib/*" ui.LoginFrame
```
**Windows (PowerShell)**
```
$desktopSources = Get-ChildItem -Recurse src\dao,src\model,src\ui,src\security -Filter *.java
javac -d out -cp "lib/*" $desktopSources.FullName
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

Desktop grading test:

**Linux / macOS**
```
javac -d out -cp "lib/*" $(find src/dao src/model src/ui src/security -name "*.java") test/GradeCalculationTest.java
java -cp out GradeCalculationTest
```

**Windows (PowerShell)**
```
$desktopSources = Get-ChildItem -Recurse src\dao,src\model,src\ui,src\security -Filter *.java
javac -d out -cp "lib/*" ($desktopSources.FullName + "test\GradeCalculationTest.java")
java -cp "out;lib/*" GradeCalculationTest
```

Web app integration tests: `mvn test`.
