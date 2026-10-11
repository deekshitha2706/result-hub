package web;

import dao.ResultDAO;
import dao.UserDAO;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Course;
import model.Result;
import model.Student;
import org.springframework.security.core.Authentication;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WebController {
    private static final List<String> GRADES = List.of("A+", "A", "B+", "B", "C", "D", "F");
    private final WebService service;

    public WebController(WebService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/home")
    public String home(Authentication authentication) {
        return isStudent(authentication) ? "redirect:/student/results" : "redirect:/admin";
    }

    @GetMapping("/admin")
    public String adminDashboard(Model model) {
        List<Student> students = service.students();
        List<Course> courses = service.courses();
        List<Result> results = service.allResults();
        double average = results.stream().mapToDouble(Result::getPercentage).average().orElse(0);
        long passed = results.stream().filter(result -> !"F".equals(result.getGrade())).count();

        Map<String, Long> gradeCounts = new LinkedHashMap<>();
        GRADES.forEach(grade -> gradeCounts.put(grade, 0L));
        results.forEach(result -> gradeCounts.computeIfPresent(result.getGrade(), (grade, count) -> count + 1));
        long maxGradeCount = gradeCounts.values().stream().mapToLong(Long::longValue).max().orElse(1);
        model.addAttribute("students", students.size());
        model.addAttribute("courses", courses.size());
        model.addAttribute("results", results.size());
        model.addAttribute("average", results.isEmpty() ? null : average);
        model.addAttribute("passRate", results.isEmpty() ? null : 100.0 * passed / results.size());
        model.addAttribute("gradeCounts", gradeCounts);
        model.addAttribute("maxGradeCount", maxGradeCount);
        model.addAttribute("latestResults", results.stream().limit(8).toList());

        Map<String, double[]> courseAverages = new LinkedHashMap<>();
        for (Result result : results) {
            double[] totals = courseAverages.computeIfAbsent(result.getCourseName(), ignored -> new double[2]);
            totals[0] += result.getPercentage();
            totals[1]++;
        }
        List<Map<String, Object>> topCourses = new ArrayList<>();
        courseAverages.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<String, double[]> entry) ->
                        entry.getValue()[0] / entry.getValue()[1]).reversed())
                .limit(6)
                .forEach(entry -> topCourses.add(Map.of("name", entry.getKey(),
                        "average", entry.getValue()[0] / entry.getValue()[1])));
        model.addAttribute("topCourses", topCourses);
        return "admin/dashboard";
    }

    @GetMapping("/admin/students")
    public String students(Model model) {
        model.addAttribute("students", service.students());
        return "admin/students";
    }

    @PostMapping("/admin/students")
    public String addStudent(@RequestParam String rollNumber, @RequestParam String firstName,
                             @RequestParam(defaultValue = "") String lastName,
                             @RequestParam(defaultValue = "") String email,
                             @RequestParam(defaultValue = "") String phone,
                             @RequestParam String dateOfBirth,
                             @RequestParam(defaultValue = "") String gender,
                             @RequestParam(defaultValue = "") String address,
                             RedirectAttributes flash) {
        try {
            String password = service.addStudent(rollNumber, firstName, lastName, email, phone,
                    LocalDate.parse(dateOfBirth), gender, address);
            flash.addFlashAttribute("success", "Student created. Login: " + rollNumber.trim()
                    + " · initial password (date of birth, ddMMyyyy): " + password);
        } catch (DateTimeParseException | IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex instanceof DateTimeParseException
                    ? "Date of birth must use yyyy-MM-dd."
                    : ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", "That roll number or student login already exists.");
        }
        return "redirect:/admin/students";
    }

    @PostMapping("/admin/students/create-logins")
    public String createMissingLogins(RedirectAttributes flash) {
        int count = service.createMissingStudentLogins();
        flash.addFlashAttribute("success", count == 0
                ? "All students already have a login."
                : count + " student login(s) created. Their initial passwords are their dates of birth (ddMMyyyy).");
        return "redirect:/admin/students";
    }

    @PostMapping("/admin/students/reset-password")
    public String resetStudentPassword(@RequestParam String rollNumber, RedirectAttributes flash) {
        try {
            String password = service.resetStudentPassword(rollNumber);
            flash.addFlashAttribute("success", "Password reset for " + rollNumber.trim()
                    + ". Initial password (date of birth, ddMMyyyy): " + password);
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", "That course code is already in use.");
        }
        return "redirect:/admin/students";
    }

    @GetMapping("/admin/courses")
    public String courses(Model model) {
        model.addAttribute("courses", service.courses());
        return "admin/courses";
    }

    @PostMapping("/admin/courses")
    public String addCourse(@RequestParam(defaultValue = "") String courseCode,
                            @RequestParam String courseName,
                            @RequestParam(defaultValue = "3") int credits,
                            @RequestParam(defaultValue = "1") int semester,
                            RedirectAttributes flash) {
        try {
            service.addCourse(courseCode, courseName, credits, semester);
            flash.addFlashAttribute("success", "Course added.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/courses";
    }

    @GetMapping("/admin/results")
    public String results(@RequestParam(defaultValue = "") String rollNumber, Model model) {
        model.addAttribute("rollNumber", rollNumber);
        model.addAttribute("today", LocalDate.now());
        int year = LocalDate.now().getYear();
        int start = LocalDate.now().getMonthValue() >= 6 ? year : year - 1;
        model.addAttribute("academicYear", start + "-" + String.format("%02d", (start + 1) % 100));
        model.addAttribute("results", rollNumber.isBlank()
                ? service.allResults() : service.resultsByRoll(rollNumber.trim()));
        model.addAttribute("students", service.students());
        model.addAttribute("courses", service.courses());
        return "admin/results";
    }

    @PostMapping("/admin/results")
    public String addResult(@RequestParam String rollNumber, @RequestParam long courseId,
                            @RequestParam double marksObtained, @RequestParam double totalMarks,
                            @RequestParam String examDate, @RequestParam int semester,
                            @RequestParam String academicYear, RedirectAttributes flash) {
        try {
            service.addResult(rollNumber, courseId, marksObtained, totalMarks,
                    LocalDate.parse(examDate), semester, academicYear);
            flash.addFlashAttribute("success", "Result recorded.");
        } catch (DateTimeParseException | IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex instanceof DateTimeParseException
                    ? "Exam date must use yyyy-MM-dd."
                    : ex.getMessage());
        }
        flash.addAttribute("rollNumber", rollNumber);
        return "redirect:/admin/results";
    }

    @GetMapping("/student/results")
    public String studentResults(Authentication authentication, Model model) {
        Student student = service.studentForLogin(authentication.getName());
        List<Result> results = service.resultsForStudent(authentication.getName());
        double average = results.stream().mapToDouble(Result::getPercentage).average().orElse(0);
        double best = results.stream().mapToDouble(Result::getPercentage).max().orElse(0);
        Map<String, Long> gradeCounts = new LinkedHashMap<>();
        GRADES.forEach(grade -> gradeCounts.put(grade, 0L));
        results.forEach(result -> gradeCounts.computeIfPresent(result.getGrade(), (grade, count) -> count + 1));
        model.addAttribute("student", student);
        model.addAttribute("results", results);
        model.addAttribute("average", results.isEmpty() ? null : average);
        model.addAttribute("best", results.isEmpty() ? null : best);
        model.addAttribute("backlog", results.stream().anyMatch(result -> "F".equals(result.getGrade())));
        model.addAttribute("gradeCounts", gradeCounts);
        model.addAttribute("maxGradeCount", gradeCounts.values().stream().mapToLong(Long::longValue).max().orElse(1));
        return "student/results";
    }

    @GetMapping("/student/account")
    public String studentAccount(Authentication authentication, Model model) {
        model.addAttribute("student", service.studentForLogin(authentication.getName()));
        return "student/account";
    }

    @PostMapping("/student/account/password")
    public String changePassword(Authentication authentication,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes flash) {
        try {
            service.changeStudentPassword(authentication.getName(), currentPassword, newPassword, confirmPassword);
            flash.addFlashAttribute("success", "Password updated.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/student/account";
    }

    private static boolean isStudent(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_STUDENT".equals(authority.getAuthority()));
    }
}
