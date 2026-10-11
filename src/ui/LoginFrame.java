package ui;

import dao.UserDAO;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import model.User;

public class LoginFrame extends JFrame {

    private final JTextField usernameField = Theme.textField(18);
    private final JPasswordField passwordField = Theme.styleInput(new JPasswordField(18));
    private final JButton loginButton = Theme.button("Log in");
    private final JLabel statusLabel = Theme.label(" ", Theme.BASE, Theme.DANGER);
    private final UserDAO userDAO = new UserDAO();

    public LoginFrame() {
        initComponents();
    }

    private void initComponents() {
        setTitle("Student Result Management System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(440, 460));
        setSize(520, 540);
        setLocationRelativeTo(null);

        // Dark backdrop with a centered white card; the card stays centered when resized.
        JPanel backdrop = new JPanel(new GridBagLayout());
        backdrop.setBackground(Theme.SIDEBAR);

        JPanel card = new Theme.Card(new GridBagLayout());
        card.setBorder(new EmptyBorder(30, 34, 28, 34));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1; c.anchor = GridBagConstraints.WEST;

        c.gridy = 0; card.add(Theme.label("Welcome back", Theme.H1, Theme.TEXT), c);
        c.gridy = 1; c.insets = new Insets(2, 0, 22, 0); card.add(Theme.muted("Admins and students: log in to continue."), c);
        c.insets = new Insets(0, 0, 5, 0);
        c.gridy = 2; card.add(Theme.label("Username  (students: roll number)", Theme.BOLD, Theme.TEXT), c);
        c.gridy = 3; c.insets = new Insets(0, 0, 14, 0); card.add(usernameField, c);
        c.insets = new Insets(0, 0, 5, 0);
        c.gridy = 4; card.add(Theme.label("Password", Theme.BOLD, Theme.TEXT), c);
        c.gridy = 5; c.insets = new Insets(0, 0, 10, 0); card.add(passwordField, c);
        c.gridy = 6; c.insets = new Insets(0, 0, 10, 0); card.add(statusLabel, c);
        c.gridy = 7; c.insets = new Insets(0, 0, 0, 0); card.add(loginButton, c);

        GridBagConstraints bc = new GridBagConstraints();
        bc.fill = GridBagConstraints.NONE;
        backdrop.add(card, bc);
        setContentPane(backdrop);
        card.setPreferredSize(new Dimension(380, 380));

        loginButton.addActionListener(e -> handleLogin());
        passwordField.addActionListener(e -> handleLogin());
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
        getRootPane().setDefaultButton(loginButton);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Please enter username and password!", Theme.DANGER);
            return;
        }

        loginButton.setEnabled(false);
        showStatus("Signing in...", Theme.MUTED);
        Theme.async(() -> userDAO.authenticateUser(username, password), user -> {
            loginButton.setEnabled(true);
            if (user != null) {
                openDashboard(user);
            } else {
                showStatus("Invalid username or password!", Theme.DANGER);
                passwordField.setText("");
            }
        }, err -> {
            loginButton.setEnabled(true);
            showStatus("<html>" + Theme.describe(err) + "</html>", Theme.DANGER);
        });
    }

    private void openDashboard(User user) {
        dispose();
        new MainDashboard(user).setVisible(true);
    }

    private void showStatus(String text, Color color) {
        statusLabel.setForeground(color);
        statusLabel.setText(text);
    }

    public static void main(String[] args) {
        Theme.install();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
