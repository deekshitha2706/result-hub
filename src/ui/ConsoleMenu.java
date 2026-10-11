package ui;

import dao.*;
import model.*;
import java.time.LocalDate;
import java.util.Scanner;

public class ConsoleMenu {

    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {

        while (true) {
            System.out.println("\n========= STUDENT RESULT SYSTEM ========");
            System.out.println("1. Add Student");
            System.out.println("2. Add Course");
            System.out.println("3. Add Result");
            System.out.println("4. View Results of a Student");
            System.out.println("0. Exit");
            System.out.print("Enter choice: ");
            int choice = sc.nextInt();
            sc.nextLine(); // clear enter

            switch (choice) {
                case 1 -> addStudentUI();
                case 2 -> addCourseUI();
                case 3 -> addResultUI();
                case 4 -> viewResultUI();
                case 0 -> {
                    return;
                }
                default -> System.out.println("Invalid choice");
            }
        }
    }

    private static void addStudentUI() {
        StudentDAO dao = new StudentDAO();
        System.out.print("Roll Number: ");
        String rn = sc.nextLine();
        System.out.print("First Name: ");
        String fn = sc.nextLine();
        System.out.print("Last Name: ");
        String ln = sc.nextLine();
        System.out.print("Email: ");
        String email = sc.nextLine();
        System.out.print("Phone: ");
        String phone = sc.nextLine();
        System.out.print("DOB (YYYY-MM-DD): ");
        LocalDate dob = LocalDate.parse(sc.nextLine());
        System.out.print("Gender: ");
        String gen = sc.nextLine();
        System.out.print("Address: ");
        String addr = sc.nextLine();

        Student s = new Student(rn, fn, ln, email, phone, dob, gen, addr);
        boolean ok = dao.addStudent(s);
        System.out.println(ok ? "Student Added" : "Failed");
    }

    private static void addCourseUI() {
        CourseDAO dao = new CourseDAO();
        System.out.print("Course Code: ");
        String code = sc.nextLine();
        System.out.print("Course Name: ");
        String name = sc.nextLine();
        System.out.print("Credits: ");
        int cr = sc.nextInt();
        System.out.print("Semester: ");
        int sem = sc.nextInt();
        sc.nextLine();

        Course c = new Course(code, name, cr, sem);
        boolean ok = dao.addCourse(c);
        System.out.println(ok ? "Course Added" : "Failed");
    }

    private static void addResultUI() {
        ResultDAO dao = new ResultDAO();
        System.out.print("Student ID: ");
        int sid = sc.nextInt();
        System.out.print("Course ID: ");
        int cid = sc.nextInt();
        System.out.print("Marks Obtained: ");
        double mo = sc.nextDouble();
        System.out.print("Total Marks: ");
        double tm = sc.nextDouble();
        System.out.print("Semester: ");
        int sem = sc.nextInt();
        sc.nextLine();
        System.out.print("Academic Year: ");
        String ay = sc.nextLine();

        double percent = (mo / tm) * 100;
        String grade = ResultDAO.calculateGrade(percent);

        Result r = new Result(sid, cid, mo, tm, grade, LocalDate.now(), sem, ay);
        boolean ok = dao.addResult(r);
        System.out.println(ok ? "Result Added" : "Failed");
    }

    private static void viewResultUI() {
        ResultDAO dao = new ResultDAO();
        System.out.print("Enter Student ID: ");
        int sid = sc.nextInt();
        sc.nextLine();

        var list = dao.getResultsByStudentId(sid);
        if (list.isEmpty()) {
            System.out.println("No results found");
            return;
        }

        System.out.println("\nRESULTS:");
        for (Result r : list) System.out.println(r);
    }
}
