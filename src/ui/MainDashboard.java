package ui;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import dao.UserDAO;
import model.User;

/** Main window: sidebar navigation on the left, the selected page on the right. */
public class MainDashboard extends JFrame {

    private final User currentUser;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, Page> pages = new LinkedHashMap<>();
    private final Map<String, NavButton> navButtons = new LinkedHashMap<>();

    public MainDashboard(User user) {
        this.currentUser = user;
        initComponents();
    }

    private void initComponents() {
        setTitle("Student Result Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 740);
        setMinimumSize(new Dimension(820, 560));
        setLocationRelativeTo(null);

        if (isStudent()) {
            addPage("My Results", new MyResultsPanel(currentUser));
            addPage("My Account", new MyAccountPanel(currentUser));
        } else {
            DashboardPanel dashboard = new DashboardPanel(currentUser.getUsername(),
                    () -> show("Students"), () -> show("Courses"), () -> show("Results"));
            addPage("Dashboard", dashboard);
            addPage("Students", new StudentsPanel());
            addPage("Courses", new CoursesPanel());
            addPage("Results", new ResultsPanel());
        }

        content.setBackground(Theme.BG);
        JPanel root = new JPanel(new BorderLayout());
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
        show(pages.keySet().iterator().next());
    }

    /** Student accounts only see their own results; every other role keeps the full admin screens. */
    private boolean isStudent() {
        return UserDAO.ROLE_STUDENT.equalsIgnoreCase(currentUser.getRole());
    }

    private void addPage(String name, JPanel page) {
        page.setOpaque(false);
        content.add(page, name);
        pages.put(name, (Page) page);
    }

    private void show(String name) {
        cards.show(content, name);
        navButtons.forEach((n, b) -> b.setSelected(n.equals(name)));
        pages.get(name).refresh();
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setBackground(Theme.SIDEBAR);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(22, 14, 18, 14));
        side.setPreferredSize(new Dimension(210, 0));

        JLabel brand = Theme.label("Student Results", Theme.H2, Color.WHITE);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.setBorder(new EmptyBorder(0, 8, 0, 0));
        JLabel tag = Theme.label("Management System", Theme.SMALL, new Color(156, 163, 175));
        tag.setBorder(new EmptyBorder(0, 8, 0, 0));
        tag.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(brand);
        side.add(tag);
        side.add(Box.createVerticalStrut(24));

        for (String name : pages.keySet()) {
            NavButton b = new NavButton(name);
            b.addActionListener(e -> show(name));
            navButtons.put(name, b);
            side.add(b);
            side.add(Box.createVerticalStrut(4));
        }
        side.add(Box.createVerticalGlue());

        JLabel user = Theme.label(currentUser.getUsername(), Theme.BOLD, Color.WHITE);
        JLabel role = Theme.label(currentUser.getRole() == null ? "" : currentUser.getRole(), Theme.SMALL, new Color(156, 163, 175));
        user.setBorder(new EmptyBorder(0, 8, 0, 0));
        role.setBorder(new EmptyBorder(0, 8, 8, 0));
        user.setAlignmentX(Component.LEFT_ALIGNMENT);
        role.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(user);
        side.add(role);

        NavButton logout = new NavButton("Log out");
        logout.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        side.add(logout);
        return side;
    }

    /** Full-width sidebar button that highlights when selected. */
    private static class NavButton extends JButton {
        private boolean selected;

        NavButton(String text) {
            super(text);
            setFont(Theme.BOLD);
            setForeground(new Color(209, 213, 219));
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new EmptyBorder(10, 12, 10, 12));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        }

        @Override public void setSelected(boolean s) {
            selected = s;
            setForeground(s ? Color.WHITE : new Color(209, 213, 219));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (selected) g2.setColor(Theme.PRIMARY);
            else if (getModel().isRollover()) g2.setColor(Theme.SIDEBAR_HOVER);
            else g2.setColor(Theme.SIDEBAR);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
