package web;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import model.Course;
import model.Result;
import model.Student;
import model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class WebRepository {
    private static final RowMapper<Student> STUDENT_ROW = (rs, row) -> mapStudent(rs);
    private static final RowMapper<Course> COURSE_ROW = (rs, row) -> mapCourse(rs);
    private static final RowMapper<Result> RESULT_ROW = (rs, row) -> mapResult(rs);

    private final JdbcTemplate jdbc;

    public WebRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int countUsers() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
    }

    public void createUser(String username, String password, String role) {
        jdbc.update("INSERT INTO users (username, password, role) VALUES (?, ?, ?)", username, password, role);
    }

    public Optional<User> findUser(String username) {
        List<User> users = jdbc.query("SELECT user_id, username, password, role FROM users WHERE username = ?",
                (rs, row) -> {
                    User user = new User(rs.getString("username"), rs.getString("password"), rs.getString("role"));
                    user.setUserId(rs.getInt("user_id"));
                    return user;
                }, username);
        return users.stream().findFirst();
    }

    public void updatePassword(String username, String password) {
        int updated = jdbc.update("UPDATE users SET password = ? WHERE username = ?", password, username);
        if (updated != 1) throw new IllegalStateException("The account password could not be updated.");
    }

    public boolean usernameExists(String username) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM users WHERE username = ?)", Boolean.class, username));
    }

    public List<Student> findAllStudents() {
        return jdbc.query("SELECT * FROM students ORDER BY roll_number", STUDENT_ROW);
    }

    public Optional<Student> findStudentByRoll(String rollNumber) {
        List<Student> students = jdbc.query("SELECT * FROM students WHERE LOWER(roll_number) = LOWER(?)",
                STUDENT_ROW, rollNumber);
        return students.stream().findFirst();
    }

    public Optional<Student> findStudentById(long studentId) {
        List<Student> students = jdbc.query("SELECT * FROM students WHERE student_id = ?",
                STUDENT_ROW, studentId);
        return students.stream().findFirst();
    }

    public void createStudent(Student student) {
        jdbc.update("INSERT INTO students (roll_number, first_name, last_name, email, phone, date_of_birth, gender, address) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                student.getRollNumber(), student.getFirstName(), student.getLastName(), student.getEmail(),
                student.getPhone(), student.getDateOfBirth(), student.getGender(), student.getAddress());
    }

    public List<Course> findAllCourses() {
        return jdbc.query("SELECT * FROM courses ORDER BY semester, course_code", COURSE_ROW);
    }

    public Optional<Course> findCourseById(long courseId) {
        List<Course> courses = jdbc.query("SELECT * FROM courses WHERE course_id = ?", COURSE_ROW, courseId);
        return courses.stream().findFirst();
    }

    public void createCourse(Course course) {
        jdbc.update("INSERT INTO courses (course_code, course_name, credits, semester) VALUES (?, ?, ?, ?)",
                course.getCourseCode(), course.getCourseName(), course.getCredits(), course.getSemester());
    }

    public List<Result> findAllResults() {
        return jdbc.query(resultQuery() + " ORDER BY r.exam_date DESC, s.roll_number", RESULT_ROW);
    }

    public List<Result> findResultsByStudent(long studentId) {
        return jdbc.query(resultQuery() + " WHERE r.student_id = ? ORDER BY r.semester, r.exam_date",
                RESULT_ROW, studentId);
    }

    public List<Result> findResultsByRoll(String rollNumber) {
        return jdbc.query(resultQuery() + " WHERE LOWER(s.roll_number) = LOWER(?) "
                + "ORDER BY r.academic_year DESC, r.semester, r.exam_date", RESULT_ROW, rollNumber);
    }

    public void createResult(Result result) {
        jdbc.update("INSERT INTO results "
                        + "(student_id, course_id, marks_obtained, total_marks, grade, exam_date, semester, academic_year) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                result.getStudentId(), result.getCourseId(), result.getMarksObtained(), result.getTotalMarks(),
                result.getGrade(), result.getExamDate(), result.getSemester(), result.getAcademicYear());
    }

    private static String resultQuery() {
        return "SELECT r.*, s.first_name, s.last_name, s.roll_number, c.course_name "
                + "FROM results r JOIN students s ON r.student_id = s.student_id "
                + "JOIN courses c ON r.course_id = c.course_id";
    }

    private static Student mapStudent(ResultSet rs) throws SQLException {
        Student student = new Student();
        student.setStudentId(rs.getInt("student_id"));
        student.setRollNumber(rs.getString("roll_number"));
        student.setFirstName(rs.getString("first_name"));
        student.setLastName(rs.getString("last_name"));
        student.setEmail(rs.getString("email"));
        student.setPhone(rs.getString("phone"));
        student.setDateOfBirth(rs.getDate("date_of_birth").toLocalDate());
        student.setGender(rs.getString("gender"));
        student.setAddress(rs.getString("address"));
        student.setEnrollmentDate(rs.getDate("enrollment_date").toLocalDate());
        return student;
    }

    private static Course mapCourse(ResultSet rs) throws SQLException {
        Course course = new Course();
        course.setCourseId(rs.getInt("course_id"));
        course.setCourseCode(rs.getString("course_code"));
        course.setCourseName(rs.getString("course_name"));
        course.setCredits(rs.getInt("credits"));
        course.setSemester(rs.getInt("semester"));
        return course;
    }

    private static Result mapResult(ResultSet rs) throws SQLException {
        Result result = new Result();
        result.setResultId(rs.getInt("result_id"));
        result.setStudentId(rs.getInt("student_id"));
        result.setCourseId(rs.getInt("course_id"));
        result.setMarksObtained(rs.getDouble("marks_obtained"));
        result.setTotalMarks(rs.getDouble("total_marks"));
        result.setGrade(rs.getString("grade"));
        result.setExamDate(rs.getDate("exam_date").toLocalDate());
        result.setSemester(rs.getInt("semester"));
        result.setAcademicYear(rs.getString("academic_year"));
        String firstName = rs.getString("first_name");
        String lastName = rs.getString("last_name");
        result.setStudentName(lastName == null || lastName.isBlank() ? firstName : firstName + " " + lastName);
        result.setStudentRollNumber(rs.getString("roll_number"));
        result.setCourseName(rs.getString("course_name"));
        return result;
    }
}
