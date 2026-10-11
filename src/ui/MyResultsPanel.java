package ui;

import dao.ResultDAO;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.Result;
import model.Student;
import model.User;

/** Student-facing screen: only the logged-in student's own results. */
public class MyResultsPanel extends JPanel implements Page {
    private static final String[] GRADES = {"A+", "A", "B+", "B", "C", "D", "F"};
    private final User user;
    private final JLabel headline = Theme.title("My Results");
    private final JLabel sub = Theme.muted(" ");
    private final JLabel error = Theme.label(" ", Theme.BASE, Theme.DANGER);
    private final JLabel courses = Theme.label("-", Theme.BIG_NUMBER, Theme.TEXT),
            average = Theme.label("-", Theme.BIG_NUMBER, Theme.TEXT),
            best = Theme.label("-", Theme.BIG_NUMBER, Theme.TEXT),
            status = Theme.label("-", Theme.BIG_NUMBER, Theme.TEXT);
    private final BarChart chart = new BarChart(false).emptyText("No results published yet")
            .colors(Theme.gradeColor("A+"), Theme.gradeColor("A"), Theme.gradeColor("B+"), Theme.gradeColor("B"),
                    Theme.gradeColor("C"), Theme.gradeColor("D"), Theme.gradeColor("F"));
    private final DefaultTableModel model = Theme.readOnlyModel(
            "Course", "Marks", "Total", "%", "Grade", "Exam Date", "Semester", "Year");

    public MyResultsPanel(User user) {
        super(new BorderLayout());
        this.user = user;
        setOpaque(false);

        Theme.ScrollPage page = new Theme.ScrollPage(new BorderLayout(0, 18));
        page.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel head = new JPanel(new BorderLayout(0, 2));
        head.setOpaque(false);
        head.add(headline, BorderLayout.NORTH);
        head.add(sub, BorderLayout.CENTER);
        head.add(error, BorderLayout.SOUTH);

        Theme.ResponsiveGrid stats = new Theme.ResponsiveGrid(4, 190, 14);
        stats.add(stat("Courses", courses, Theme.PRIMARY));
        stats.add(stat("Average Score", average, Theme.SUCCESS));
        stats.add(stat("Best Score", best, new Color(13, 148, 136)));
        stats.add(stat("Overall", status, new Color(37, 99, 235)));

        JPanel chartCard = new Theme.Card(new BorderLayout(0, 10));
        chartCard.add(Theme.label("My grades", Theme.H2, Theme.TEXT), BorderLayout.NORTH);
        chartCard.add(chart, BorderLayout.CENTER);
        chartCard.setPreferredSize(new Dimension(300, 270));
        chartCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 270));

        JTable table = Theme.table(model);
        Theme.colorGradeColumn(table, 4);
        JPanel tableCard = new Theme.Card(new BorderLayout(0, 10));
        tableCard.add(Theme.label("All my results", Theme.H2, Theme.TEXT), BorderLayout.NORTH);
        tableCard.add(Theme.tableCard(table), BorderLayout.CENTER);
        tableCard.setPreferredSize(new Dimension(300, 380));
        tableCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 380));

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{stats, chartCard, tableCard}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            column.add(c);
            column.add(Box.createVerticalStrut(16));
        }
        page.add(head, BorderLayout.NORTH);
        page.add(column, BorderLayout.CENTER);
        add(Theme.scroll(page), BorderLayout.CENTER);
    }

    private static JComponent stat(String caption, JLabel value, Color accent) {
        JPanel card = new Theme.Card(new BorderLayout(0, 4)) {
            @Override protected void paintChildren(Graphics g) {
                super.paintChildren(g);
                g.setColor(accent);
                g.fillRoundRect(0, 14, 4, getHeight() - 28, 4, 4);
            }
        };
        card.add(Theme.label(caption, Theme.BOLD, Theme.MUTED), BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private record Data(Student student, List<Result> results) {}

    @Override
    public void refresh() {
        error.setText(" ");
        Theme.async(() -> {
            Student s = AddResultDialog.findByRoll(user.getUsername());
            return new Data(s, s == null ? List.of() : new ResultDAO().getResultsByStudentId(s.getStudentId()));
        }, this::show, err -> error.setText(Theme.describe(err)));
    }

    private void show(Data d) {
        if (d.student() == null) {
            error.setText("No student record matches the login \"" + user.getUsername() + "\". Please contact your administrator.");
            return;
        }
        headline.setText("Hello, " + d.student().getFirstName() + "!");
        sub.setText("Roll number " + d.student().getRollNumber() + "  |  Your published results");

        List<Result> rs = d.results();
        courses.setText(String.valueOf(rs.size()));
        double avg = rs.stream().mapToDouble(Result::getPercentage).average().orElse(0);
        double top = rs.stream().mapToDouble(Result::getPercentage).max().orElse(0);
        boolean failed = rs.stream().anyMatch(r -> "F".equals(r.getGrade()));
        average.setText(rs.isEmpty() ? "-" : String.format("%.1f%%", avg));
        best.setText(rs.isEmpty() ? "-" : String.format("%.1f%%", top));
        status.setText(rs.isEmpty() ? "-" : failed ? "Backlog" : "Pass");
        status.setForeground(rs.isEmpty() ? Theme.TEXT : failed ? Theme.DANGER : Theme.SUCCESS);

        Map<String, Integer> byGrade = new LinkedHashMap<>();
        for (String g : GRADES) byGrade.put(g, 0);
        for (Result r : rs) byGrade.merge(r.getGrade(), 1, Integer::sum);
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        byGrade.forEach((g, n) -> { labels.add(g); values.add((double) n); });
        chart.setData(rs.isEmpty() ? List.of() : labels, rs.isEmpty() ? List.of() : values);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        model.setRowCount(0);
        for (Result r : rs) {
            model.addRow(new Object[]{r.getCourseName(), r.getMarksObtained(), r.getTotalMarks(),
                    String.format("%.2f", r.getPercentage()), r.getGrade(), r.getExamDate().format(fmt),
                    r.getSemester(), r.getAcademicYear()});
        }
    }
}
