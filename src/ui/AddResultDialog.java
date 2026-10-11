package ui;

import dao.CourseDAO;
import dao.ResultDAO;
import dao.StudentDAO;
import java.awt.Window;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import model.Course;
import model.Result;
import model.Student;

public class AddResultDialog extends FormDialog {
    private final JTextField tfRoll = Theme.textField(14), tfMarks = Theme.textField(8),
            tfTotal = Theme.textField(8), tfDate = Theme.textField(10), tfYear = Theme.textField(10);
    private final JLabel lblStudent = Theme.muted("Type a roll number and press Find.");
    private final JLabel lblPreview = Theme.muted("Grade preview: -");
    private final JComboBox<String> cbCourse = Theme.styleInput(new JComboBox<>());
    private final JComboBox<Integer> cbSemester = Theme.styleInput(new JComboBox<>());
    private final Runnable onSaved;
    private List<Course> courses = List.of();
    private Student student;

    public AddResultDialog(Window owner, Runnable onSaved) {
        super(owner, "Add Result", "Record a student's marks for a course.", "Save Result");
        this.onSaved = onSaved;

        JButton find = Theme.button("Find", Theme.Kind.SECONDARY);
        find.addActionListener(e -> findStudent());
        tfRoll.addActionListener(e -> findStudent());
        JPanel rollRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        rollRow.setOpaque(false);
        rollRow.add(tfRoll);
        rollRow.add(find);
        addRow("Student Roll No.", rollRow);
        addRow("", lblStudent);
        addRow("Course", cbCourse);
        addRow("Marks Obtained", tfMarks);
        addRow("Total Marks", tfTotal);
        addRow("", lblPreview);
        addRow("Exam Date", tfDate, "Format: yyyy-MM-dd");
        for (int i = 1; i <= 12; i++) cbSemester.addItem(i);
        addRow("Semester", cbSemester);
        addRow("Academic Year", tfYear, "Example: 2024-25");

        tfTotal.setText("100");
        tfDate.setText(LocalDate.now().toString());
        tfYear.setText(currentAcademicYear());
        DocumentListener preview = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updatePreview(); }
            public void removeUpdate(DocumentEvent e) { updatePreview(); }
            public void changedUpdate(DocumentEvent e) { updatePreview(); }
        };
        tfMarks.getDocument().addDocumentListener(preview);
        tfTotal.getDocument().addDocumentListener(preview);

        loadCourses();
        open();
    }

    private static String currentAcademicYear() {
        LocalDate d = LocalDate.now();
        int start = d.getMonthValue() >= 6 ? d.getYear() : d.getYear() - 1;
        return start + "-" + String.format("%02d", (start + 1) % 100);
    }

    private void loadCourses() {
        Theme.async(() -> new CourseDAO().getAllCourses(), list -> {
            courses = list;
            cbCourse.removeAllItems();
            if (list.isEmpty()) {
                cbCourse.addItem("No courses available - add a course first");
                cbCourse.setEnabled(false);
            } else {
                for (Course c : list) cbCourse.addItem(c.getCourseCode() + " - " + c.getCourseName());
            }
        }, err -> showError(Theme.describe(err)));
    }

    private void findStudent() {
        String roll = tfRoll.getText().trim();
        if (roll.isEmpty()) { lblStudent.setText("Enter a roll number first."); return; }
        Theme.async(() -> findByRoll(roll), s -> {
            student = s;
            lblStudent.setForeground(s == null ? Theme.DANGER : Theme.SUCCESS);
            lblStudent.setText(s == null ? "Student not found." : s.getFirstName() + " " + s.getLastName() + " (id " + s.getStudentId() + ")");
        }, err -> showError(Theme.describe(err)));
    }

    /** Shared lookup (same approach as before: scan all students for the roll number). */
    static Student findByRoll(String roll) {
        for (Student s : new StudentDAO().getAllStudents()) {
            if (roll.equalsIgnoreCase(s.getRollNumber())) return s;
        }
        return null;
    }

    private void updatePreview() {
        try {
            double m = Double.parseDouble(tfMarks.getText().trim());
            double t = Double.parseDouble(tfTotal.getText().trim());
            if (t <= 0) throw new NumberFormatException();
            double pct = m / t * 100;
            lblPreview.setText(String.format("Grade preview: %s (%.2f%%)", ResultDAO.calculateGrade(pct), pct));
        } catch (NumberFormatException e) {
            lblPreview.setText("Grade preview: -");
        }
    }

    @Override
    protected void onSave() {
        String roll = tfRoll.getText().trim();
        if (roll.isEmpty()) { showError("Enter the student's roll number first."); return; }
        if (courses.isEmpty()) { showError("No course available. Add a course first."); return; }
        double marks, total;
        try {
            marks = Double.parseDouble(tfMarks.getText().trim());
            total = Double.parseDouble(tfTotal.getText().trim());
        } catch (NumberFormatException e) {
            showError("Please enter valid numeric marks.");
            return;
        }
        if (total <= 0) { showError("Total marks must be greater than 0."); return; }
        if (marks < 0 || marks > total) { showError("Marks obtained must be between 0 and the total marks."); return; }
        LocalDate examDate;
        try {
            examDate = LocalDate.parse(tfDate.getText().trim());
        } catch (DateTimeParseException e) {
            showError("Exam date must look like 2025-03-15.");
            return;
        }
        String year = tfYear.getText().trim();
        if (year.isEmpty()) { showError("Enter the academic year (e.g. 2024-25)."); return; }

        Course course = courses.get(cbCourse.getSelectedIndex());
        int semester = (Integer) cbSemester.getSelectedItem();
        double pct = marks / total * 100.0;
        String grade = ResultDAO.calculateGrade(pct);

        setSaving(true);
        Theme.async(() -> {
            Student s = findByRoll(roll);
            if (s == null) return -1;
            Result r = new Result(s.getStudentId(), course.getCourseId(), marks, total, grade, examDate, semester, year);
            return new ResultDAO().addResult(r) ? 1 : 0;
        }, code -> {
            setSaving(false);
            if (code < 0) showError("Student not found for roll: " + roll);
            else if (code == 0) showError("Failed to add result.");
            else {
                Theme.info(this, String.format("Result added successfully!%nGrade: %s (%.2f%%)", grade, pct));
                dispose();
                onSaved.run();
            }
        }, err -> { setSaving(false); showError(Theme.describe(err)); });
    }
}
