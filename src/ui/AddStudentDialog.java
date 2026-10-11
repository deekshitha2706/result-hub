package ui;

import dao.StudentDAO;
import dao.UserDAO;
import model.User;
import java.awt.Window;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.*;
import model.Student;

public class AddStudentDialog extends FormDialog {
    private final JTextField tfRoll = Theme.textField(20), tfFirst = Theme.textField(20),
            tfLast = Theme.textField(20), tfEmail = Theme.textField(20), tfPhone = Theme.textField(20),
            tfDob = Theme.textField(20), tfAddress = Theme.textField(20);
    private final JComboBox<String> cbGender = Theme.styleInput(new JComboBox<>(new String[]{"Male", "Female", "Other"}));
    private final Runnable onSaved;

    public AddStudentDialog(Window owner, Runnable onSaved) {
        super(owner, "Add Student", "Fill in the student's details.", "Save Student");
        this.onSaved = onSaved;
        addRow("Roll Number", tfRoll);
        addRow("First Name", tfFirst);
        addRow("Last Name", tfLast);
        addRow("Email", tfEmail);
        addRow("Phone", tfPhone);
        addRow("Date of Birth", tfDob, "Format: yyyy-MM-dd  (example: 2004-12-26)");
        addRow("Gender", cbGender);
        addRow("Address", tfAddress);
        open();
    }

    @Override
    protected void onSave() {
        if (tfRoll.getText().isBlank() || tfFirst.getText().isBlank()) {
            showError("Roll number and first name are required.");
            return;
        }
        LocalDate dob;
        try {
            dob = LocalDate.parse(tfDob.getText().trim());
        } catch (DateTimeParseException ex) {
            showError("Date of birth must look like 2004-12-26.");
            return;
        }
        Student s = new Student(tfRoll.getText().trim(), tfFirst.getText().trim(), tfLast.getText().trim(),
                tfEmail.getText().trim(), tfPhone.getText().trim(), dob,
                (String) cbGender.getSelectedItem(), tfAddress.getText().trim());

        setSaving(true);
        // Also create the student's login: username = roll number, password = date of birth (ddMMyyyy).
        Theme.async(() -> {
            if (!new StudentDAO().addStudent(s)) return false;
            UserDAO users = new UserDAO();
            if (!users.usernameExists(s.getRollNumber())) {
                users.addUser(new User(s.getRollNumber(), UserDAO.defaultStudentPassword(s.getDateOfBirth()), UserDAO.ROLE_STUDENT));
            }
            return true;
        }, ok -> {
            setSaving(false);
            if (ok) {
                Theme.info(this, "Student added successfully!\n\nStudent login created:\n  Username: " + s.getRollNumber()
                        + "\n  Password: " + UserDAO.defaultStudentPassword(s.getDateOfBirth()) + "  (date of birth, ddMMyyyy)");
                dispose();
                onSaved.run();
            } else {
                showError("Failed to add student. The roll number may already exist.");
            }
        }, err -> { setSaving(false); showError(Theme.describe(err)); });
    }
}
