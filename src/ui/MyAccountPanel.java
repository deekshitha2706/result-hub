package ui;

import dao.UserDAO;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import model.Student;
import model.User;

/** Student profile (read-only) and change-password form. */
public class MyAccountPanel extends JPanel implements Page {
    private final User user;
    private final JPanel profile = new JPanel(new GridBagLayout());
    private final JPasswordField current = Theme.styleInput(new JPasswordField(18)),
            next = Theme.styleInput(new JPasswordField(18)), again = Theme.styleInput(new JPasswordField(18));
    private final JLabel msg = Theme.label(" ", Theme.BASE, Theme.DANGER);

    public MyAccountPanel(User user) {
        super(new BorderLayout(0, 16));
        this.user = user;
        setOpaque(false);
        setBorder(new EmptyBorder(24, 28, 24, 28));
        add(Theme.pageHeader("My Account", "Your details and password.", new JComponent[0]), BorderLayout.NORTH);

        profile.setOpaque(false);
        JPanel profileCard = new Theme.Card(new BorderLayout(0, 10));
        profileCard.add(Theme.label("Profile", Theme.H2, Theme.TEXT), BorderLayout.NORTH);
        profileCard.add(profile, BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        addField(form, 0, "Current password", current);
        addField(form, 1, "New password", next);
        addField(form, 2, "Confirm new password", again);
        JButton save = Theme.button("Change Password");
        save.addActionListener(e -> change());
        again.addActionListener(e -> change());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1; c.gridy = 3; c.anchor = GridBagConstraints.WEST; c.insets = new Insets(10, 0, 0, 0);
        form.add(save, c);
        c.gridy = 4; c.insets = new Insets(8, 0, 0, 0);
        form.add(msg, c);
        JPanel pwCard = new Theme.Card(new BorderLayout(0, 10));
        pwCard.add(Theme.label("Change password", Theme.H2, Theme.TEXT), BorderLayout.NORTH);
        pwCard.add(form, BorderLayout.CENTER);

        Theme.ResponsiveGrid grid = new Theme.ResponsiveGrid(2, 380, 14);
        grid.add(profileCard);
        grid.add(pwCard);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(grid, BorderLayout.NORTH);
        add(Theme.scroll(top), BorderLayout.CENTER);
    }

    private static void addField(JPanel p, int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row; c.insets = new Insets(5, 0, 5, 14); c.anchor = GridBagConstraints.WEST;
        p.add(Theme.label(label, Theme.BOLD, Theme.TEXT), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(5, 0, 5, 0);
        p.add(field, c);
    }

    @Override
    public void refresh() {
        Theme.async(() -> AddResultDialog.findByRoll(user.getUsername()), this::showProfile,
                err -> msg.setText(Theme.describe(err)));
    }

    private void showProfile(Student s) {
        profile.removeAll();
        String[][] rows = s == null ? new String[][]{{"Username", user.getUsername()}} : new String[][]{
                {"Name", s.getFirstName() + " " + s.getLastName()}, {"Roll number", s.getRollNumber()},
                {"Email", s.getEmail()}, {"Phone", s.getPhone()}, {"Date of birth", String.valueOf(s.getDateOfBirth())},
                {"Gender", s.getGender()}, {"Address", s.getAddress()}, {"Enrolled", String.valueOf(s.getEnrollmentDate())}};
        for (int i = 0; i < rows.length; i++) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = i; c.anchor = GridBagConstraints.WEST; c.insets = new Insets(4, 0, 4, 18);
            profile.add(Theme.label(rows[i][0], Theme.BOLD, Theme.MUTED), c);
            c.gridx = 1; c.weightx = 1; c.insets = new Insets(4, 0, 4, 0);
            profile.add(Theme.label(rows[i][1] == null ? "" : rows[i][1], Theme.BASE, Theme.TEXT), c);
        }
        profile.revalidate();
        profile.repaint();
    }

    private void change() {
        String old = new String(current.getPassword()), pw = new String(next.getPassword()), pw2 = new String(again.getPassword());
        if (old.isEmpty() || pw.isEmpty()) { note("Fill in all the password fields.", Theme.DANGER); return; }
        if (pw.length() < 4) { note("The new password must be at least 4 characters.", Theme.DANGER); return; }
        if (!pw.equals(pw2)) { note("The new passwords don't match.", Theme.DANGER); return; }
        Theme.async(() -> new UserDAO().changePassword(user.getUsername(), old, pw), ok -> {
            if (ok) {
                current.setText(""); next.setText(""); again.setText("");
                note("Password changed.", Theme.SUCCESS);
            } else {
                note("Current password is incorrect.", Theme.DANGER);
            }
        }, err -> note(Theme.describe(err), Theme.DANGER));
    }

    private void note(String text, Color color) { msg.setForeground(color); msg.setText(text); }
}
