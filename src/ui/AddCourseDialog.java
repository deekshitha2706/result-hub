package ui;

import dao.CourseDAO;
import java.awt.Window;
import javax.swing.*;
import model.Course;

public class AddCourseDialog extends FormDialog {
    private final JTextField tfCode = Theme.textField(20), tfName = Theme.textField(20);
    private final JSpinner spCredits = Theme.styleInput(new JSpinner(new SpinnerNumberModel(3, 0, 12, 1)));
    private final JComboBox<Integer> cbSemester = Theme.styleInput(new JComboBox<>());
    private final Runnable onSaved;

    public AddCourseDialog(Window owner, Runnable onSaved) {
        super(owner, "Add Course", "Add a subject students can receive results for.", "Save Course");
        this.onSaved = onSaved;
        for (int i = 1; i <= 12; i++) cbSemester.addItem(i);
        addRow("Course Name", tfName);
        addRow("Course Code", tfCode, "Optional - a code is generated if you leave it empty.");
        addRow("Credits", spCredits);
        addRow("Semester", cbSemester);
        open();
    }

    @Override
    protected void onSave() {
        String name = tfName.getText().trim();
        if (name.isEmpty()) {
            showError("Enter a course name.");
            return;
        }
        Course c = new Course();
        c.setCourseName(name);
        c.setSemester((Integer) cbSemester.getSelectedItem());
        c.setCredits((Integer) spCredits.getValue());
        String code = tfCode.getText().trim();
        c.setCourseCode(code.isEmpty() ? "COURSE" + System.currentTimeMillis() : code); // same auto-code as before

        setSaving(true);
        Theme.async(() -> new CourseDAO().addCourse(c), ok -> {
            setSaving(false);
            if (ok) {
                Theme.info(this, "Course added!");
                dispose();
                onSaved.run();
            } else {
                showError("Failed to add course. The course code may already exist.");
            }
        }, err -> { setSaving(false); showError(Theme.describe(err)); });
    }
}
