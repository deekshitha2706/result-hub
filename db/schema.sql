-- Student Result System - SQLite schema.
-- Open db/student_result_system.db in DB Browser for SQLite, run this script,
-- then click Write Changes.

CREATE TABLE IF NOT EXISTS users (
    user_id  INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    role     TEXT NOT NULL DEFAULT 'ADMIN'
);

CREATE TABLE IF NOT EXISTS students (
    student_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    roll_number     TEXT NOT NULL UNIQUE,
    first_name      TEXT NOT NULL,
    last_name       TEXT,
    email           TEXT,
    phone           TEXT,
    date_of_birth   TEXT NOT NULL,
    gender          TEXT,
    address         TEXT,
    enrollment_date TEXT NOT NULL DEFAULT (CURRENT_DATE)
);

CREATE TABLE IF NOT EXISTS courses (
    course_id   INTEGER PRIMARY KEY AUTOINCREMENT,
    course_code TEXT NOT NULL UNIQUE,
    course_name TEXT NOT NULL,
    credits     INTEGER NOT NULL DEFAULT 0,
    semester    INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS results (
    result_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id     INTEGER NOT NULL,
    course_id      INTEGER NOT NULL,
    marks_obtained REAL NOT NULL,
    total_marks    REAL NOT NULL,
    grade          TEXT NOT NULL,
    exam_date      TEXT NOT NULL,
    semester       INTEGER NOT NULL,
    academic_year  TEXT NOT NULL,
    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    FOREIGN KEY (course_id)  REFERENCES courses(course_id)
);

-- First login (change this password!): admin / admin123
INSERT OR IGNORE INTO users (username, password, role) VALUES ('admin', 'admin123', 'ADMIN');
