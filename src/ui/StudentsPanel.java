package ui;

import dao.StudentDAO;
import dao.UserDAO;
import model.User;
import java.time.LocalDate;
import javax.swing.*;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import model.Student;

public class StudentsPanel extends ListPage {
    public StudentsPanel() {
        super("Students", "Everyone enrolled in the system.", "+ Add Student",
                "ID", "Roll No.", "Name", "Email", "Phone", "Date of Birth", "Gender", "Address", "Enrolled");
    }

    @Override
    protected List<Object[]> fetchRows() {
        List<Object[]> rows = new ArrayList<>();
        for (Student s : new StudentDAO().getAllStudents()) {
            rows.add(new Object[]{s.getStudentId(), s.getRollNumber(), s.getFirstName() + " " + s.getLastName(),
                    s.getEmail(), s.getPhone(), s.getDateOfBirth(), s.getGender(), s.getAddress(), s.getEnrollmentDate()});
        }
        return rows;
    }

    @Override
    protected void onAdd() {
        Window w = SwingUtilities.getWindowAncestor(this);
        new AddStudentDialog(w, this::refresh);
    }

    @Override
    protected JComponent[] extraButtons() {
        JButton create = Theme.button("Create Logins", Theme.Kind.SECONDARY);
        create.setToolTipText("Create a login for every student that doesn't have one yet");
        create.addActionListener(e -> createMissingLogins());
        JButton reset = Theme.button("Reset Password", Theme.Kind.SECONDARY);
        reset.setToolTipText("Select a student in the table first");
        reset.addActionListener(e -> resetSelectedPassword());
        return new JComponent[]{create, reset};
    }

    /** Username = roll number, password = date of birth (ddMMyyyy). Existing logins are left alone. */
    private void createMissingLogins() {
        Theme.async(() -> {
            UserDAO users = new UserDAO();
            int created = 0;
            for (Student s : new StudentDAO().getAllStudents()) {
                if (!users.usernameExists(s.getRollNumber())
                        && users.addUser(new User(s.getRollNumber(),
                                UserDAO.defaultStudentPassword(s.getDateOfBirth()), UserDAO.ROLE_STUDENT))) {
                    created++;
                }
            }
            return created;
        }, n -> Theme.info(this, n == 0 ? "Every student already has a login."
                : n + " student login(s) created.\nUsername: roll number    Password: date of birth (ddMMyyyy)"),
           err -> Theme.error(this, Theme.describe(err)));
    }

    private void resetSelectedPassword() {
        int row = table.getSelectedRow();
        if (row < 0) { Theme.info(this, "Select a student in the table first."); return; }
        int m = table.convertRowIndexToModel(row);
        String roll = String.valueOf(table.getModel().getValueAt(m, 1));
        LocalDate dob = (LocalDate) table.getModel().getValueAt(m, 5);
        String pw = UserDAO.defaultStudentPassword(dob);
        if (JOptionPane.showConfirmDialog(this, "Reset the password for " + roll + " to their date of birth (" + pw + ")?",
                "Reset Password", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        Theme.async(() -> new UserDAO().resetStudentPassword(roll, pw),
            ok -> {
                if (ok) Theme.info(this, "Password reset. " + roll + " can now log in with " + pw + ".");
                else Theme.error(this, "No login found for " + roll + ". Use \"Create Logins\" first.");
            }, err -> Theme.error(this, Theme.describe(err)));
    }
}
