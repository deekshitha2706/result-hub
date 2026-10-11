package web;

import java.time.LocalDate;
import java.util.List;
import model.Course;
import model.Result;
import model.Student;
import model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dao.ResultDAO;
import dao.UserDAO;

@Service
public class WebService {
    private final WebRepository repository;
    private final PasswordEncoder passwords;

    public WebService(WebRepository repository, PasswordEncoder passwords) {
        this.repository = repository;
        this.passwords = passwords;
    }

    @Transactional
    public String addStudent(String roll, String first, String last, String email, String phone,
                             LocalDate dob, String gender, String address) {
        String normalizedRoll = required(roll, "Enter a roll number.");
        if (repository.findStudentByRoll(normalizedRoll).isPresent() || repository.usernameExists(normalizedRoll)) {
            throw new IllegalArgumentException("That roll number or login already exists.");
        }
        Student student = new Student(normalizedRoll, required(first, "Enter a first name."),
                blankToNull(last), blankToNull(email), blankToNull(phone), dob,
                blankToNull(gender), blankToNull(address));
        repository.createStudent(student);
        String initialPassword = UserDAO.defaultStudentPassword(dob);
        repository.createUser(normalizedRoll, passwords.encode(initialPassword), UserDAO.ROLE_STUDENT);
        return initialPassword;
    }

    @Transactional
    public String resetStudentPassword(String roll) {
        Student student = repository.findStudentByRoll(roll)
                .orElseThrow(() -> new IllegalArgumentException("No student has that roll number."));
        String initialPassword = UserDAO.defaultStudentPassword(student.getDateOfBirth());
        User account = repository.findUser(student.getRollNumber())
                .orElseThrow(() -> new IllegalArgumentException("This student has no login. Create the login first."));
        if (!UserDAO.ROLE_STUDENT.equalsIgnoreCase(account.getRole())) {
            throw new IllegalArgumentException("That username is not a student login.");
        }
        repository.updatePassword(account.getUsername(), passwords.encode(initialPassword));
        return initialPassword;
    }

    @Transactional
    public int createMissingStudentLogins() {
        int created = 0;
        for (Student student : repository.findAllStudents()) {
            if (!repository.usernameExists(student.getRollNumber())) {
                repository.createUser(student.getRollNumber(),
                        passwords.encode(UserDAO.defaultStudentPassword(student.getDateOfBirth())),
                        UserDAO.ROLE_STUDENT);
                created++;
            }
        }
        return created;
    }

    public void addCourse(String code, String name, int credits, int semester) {
        String courseName = required(name, "Enter a course name.");
        String courseCode = code == null || code.isBlank()
                ? "COURSE-" + System.currentTimeMillis()
                : code.trim();
        if (credits < 0 || credits > 12) throw new IllegalArgumentException("Credits must be between 0 and 12.");
        if (semester < 1 || semester > 12) throw new IllegalArgumentException("Semester must be between 1 and 12.");
        Course course = new Course(courseCode, courseName, credits, semester);
        repository.createCourse(course);
    }

    public void addResult(String roll, long courseId, double marks, double total,
                          LocalDate examDate, int semester, String academicYear) {
        Student student = repository.findStudentByRoll(roll)
                .orElseThrow(() -> new IllegalArgumentException("No student has that roll number."));
        Course course = repository.findCourseById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Select an available course."));
        if (!Double.isFinite(marks) || !Double.isFinite(total) || total <= 0 || marks < 0 || marks > total) {
            throw new IllegalArgumentException("Marks must be between zero and a positive total.");
        }
        if (semester < 1 || semester > 12) throw new IllegalArgumentException("Semester must be between 1 and 12.");
        String year = required(academicYear, "Enter an academic year.");
        Result result = new Result(student.getStudentId(), course.getCourseId(), marks, total,
                ResultDAO.calculateGrade(marks / total * 100), examDate, semester, year);
        repository.createResult(result);
    }

    public void changeStudentPassword(String username, String current, String next, String confirmation) {
        if (next == null || next.length() < 8) {
            throw new IllegalArgumentException("Use a new password with at least 8 characters.");
        }
        if (!next.equals(confirmation)) {
            throw new IllegalArgumentException("The new passwords do not match.");
        }
        User user = repository.findUser(username)
                .orElseThrow(() -> new IllegalArgumentException("The account could not be found."));
        if (!passwords.matches(current, user.getPassword())) {
            throw new IllegalArgumentException("The current password is incorrect.");
        }
        repository.updatePassword(username, passwords.encode(next));
    }

    public List<Student> students() { return repository.findAllStudents(); }
    public List<Course> courses() { return repository.findAllCourses(); }
    public List<Result> allResults() { return repository.findAllResults(); }
    public List<Result> resultsByRoll(String roll) { return repository.findResultsByRoll(roll); }

    public Student studentForLogin(String username) {
        return repository.findStudentByRoll(username).orElse(null);
    }

    public List<Result> resultsForStudent(String username) {
        Student student = studentForLogin(username);
        return student == null ? List.of() : repository.findResultsByStudent(student.getStudentId());
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
