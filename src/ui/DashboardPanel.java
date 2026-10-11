package ui;

import dao.CourseDAO;
import dao.ResultDAO;
import dao.StudentDAO;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.Course;
import model.Result;
import model.Student;

/** Overview screen: key numbers, two charts, quick actions and the latest results. */
public class DashboardPanel extends JPanel implements Page {
    private static final String[] GRADES = {"A+", "A", "B+", "B", "C", "D", "F"};

    private final JLabel welcome;
    private final JLabel students = bigNumber(), courses = bigNumber(), results = bigNumber(),
            average = bigNumber(), passRate = bigNumber();
    private final BarChart gradeChart = new BarChart(false).suffix("").emptyText("No results recorded yet")
            .colors(Theme.gradeColor("A+"), Theme.gradeColor("A"), Theme.gradeColor("B+"), Theme.gradeColor("B"),
                    Theme.gradeColor("C"), Theme.gradeColor("D"), Theme.gradeColor("F"));
    private final BarChart courseChart = new BarChart(true).fixedMax(100).suffix("%")
            .emptyText("No results recorded yet").colors(Theme.PRIMARY);
    private final DefaultTableModel latest = Theme.readOnlyModel("Student", "Course", "Marks", "%", "Grade", "Date");
    private final JLabel error = Theme.label(" ", Theme.BASE, Theme.DANGER);

    public DashboardPanel(String username, Runnable goStudents, Runnable goCourses, Runnable goResults) {
        super(new BorderLayout());
        setOpaque(false);
        welcome = Theme.title("Welcome back, " + username + "!");

        Theme.ScrollPage page = new Theme.ScrollPage(new BorderLayout(0, 18));
        page.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel head = new JPanel(new BorderLayout(0, 2));
        head.setOpaque(false);
        head.add(welcome, BorderLayout.NORTH);
        head.add(Theme.muted("Here is how your students are doing."), BorderLayout.CENTER);
        head.add(error, BorderLayout.SOUTH);

        Theme.ResponsiveGrid stats = new Theme.ResponsiveGrid(5, 170, 14);
        stats.add(statCard("Students", students, Theme.PRIMARY));
        stats.add(statCard("Courses", courses, new Color(13, 148, 136)));
        stats.add(statCard("Results", results, Theme.WARNING));
        stats.add(statCard("Average Score", average, Theme.SUCCESS));
        stats.add(statCard("Pass Rate", passRate, new Color(37, 99, 235)));

        Theme.ResponsiveGrid charts = new Theme.ResponsiveGrid(2, 380, 14);
        charts.add(chartCard("Grade distribution", "Number of results per grade", gradeChart));
        charts.add(chartCard("Average score by course", "Top courses by average percentage", courseChart));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        for (Object[] a : new Object[][]{{"+ Add Student", goStudents}, {"+ Add Course", goCourses}, {"+ Add Result", goResults}}) {
            JButton b = Theme.button((String) a[0], Theme.Kind.SECONDARY);
            b.addActionListener(e -> ((Runnable) a[1]).run());
            actions.add(b);
        }

        JTable table = Theme.table(latest);
        table.setRowSorter(null);
        table.setPreferredScrollableViewportSize(new Dimension(300, 200));
        Theme.colorGradeColumn(table, 4);
        JPanel latestCard = new Theme.Card(new BorderLayout(0, 10));
        latestCard.add(Theme.label("Latest results", Theme.H2, Theme.TEXT), BorderLayout.NORTH);
        latestCard.add(Theme.tableCard(table), BorderLayout.CENTER);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{actions, stats, charts, latestCard}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            column.add(c);
            column.add(Box.createVerticalStrut(16));
        }
        latestCard.setPreferredSize(new Dimension(300, 380));
        latestCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 380));
        charts.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));

        page.add(head, BorderLayout.NORTH);
        page.add(column, BorderLayout.CENTER);
        add(Theme.scroll(page), BorderLayout.CENTER);
    }

    private static JLabel bigNumber() { return Theme.label("-", Theme.BIG_NUMBER, Theme.TEXT); }

    private static JComponent statCard(String caption, JLabel value, Color accent) {
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

    private static JComponent chartCard(String title, String subtitle, BarChart chart) {
        JPanel card = new Theme.Card(new BorderLayout(0, 10));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Theme.label(title, Theme.H2, Theme.TEXT), BorderLayout.NORTH);
        head.add(Theme.label(subtitle, Theme.SMALL, Theme.MUTED), BorderLayout.SOUTH);
        card.add(head, BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    private record Data(List<Student> students, List<Course> courses, List<Result> results) {}

    @Override
    public void refresh() {
        error.setText(" ");
        Theme.async(() -> new Data(new StudentDAO().getAllStudents(), new CourseDAO().getAllCourses(),
                new ResultDAO().getAllResults()), this::show, err -> error.setText(Theme.describe(err)));
    }

    private void show(Data d) {
        students.setText(String.valueOf(d.students().size()));
        courses.setText(String.valueOf(d.courses().size()));
        results.setText(String.valueOf(d.results().size()));

        List<Result> rs = d.results();
        double avg = rs.stream().mapToDouble(Result::getPercentage).average().orElse(Double.NaN);
        long passed = rs.stream().filter(r -> !"F".equals(r.getGrade())).count();
        average.setText(rs.isEmpty() ? "-" : String.format("%.1f%%", avg));
        passRate.setText(rs.isEmpty() ? "-" : String.format("%.0f%%", 100.0 * passed / rs.size()));

        Map<String, Integer> byGrade = new LinkedHashMap<>();
        for (String g : GRADES) byGrade.put(g, 0);
        for (Result r : rs) byGrade.merge(r.getGrade(), 1, Integer::sum);
        List<String> gl = new ArrayList<>();
        List<Double> gv = new ArrayList<>();
        byGrade.forEach((g, n) -> { gl.add(g); gv.add((double) n); });
        gradeChart.setData(rs.isEmpty() ? List.of() : gl, rs.isEmpty() ? List.of() : gv);

        Map<String, double[]> byCourse = new HashMap<>(); // name -> {sum, count}
        for (Result r : rs) {
            double[] a = byCourse.computeIfAbsent(r.getCourseName(), k -> new double[2]);
            a[0] += r.getPercentage();
            a[1]++;
        }
        List<Map.Entry<String, double[]>> top = new ArrayList<>(byCourse.entrySet());
        top.sort((a, b) -> Double.compare(b.getValue()[0] / b.getValue()[1], a.getValue()[0] / a.getValue()[1]));
        List<String> cl = new ArrayList<>();
        List<Double> cv = new ArrayList<>();
        for (Map.Entry<String, double[]> e : top.subList(0, Math.min(6, top.size()))) {
            cl.add(e.getKey());
            cv.add(e.getValue()[0] / e.getValue()[1]);
        }
        courseChart.setData(cl, cv);

        latest.setRowCount(0);
        List<Result> sorted = new ArrayList<>(rs);
        sorted.sort(Comparator.comparing(Result::getExamDate).reversed());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        for (Result r : sorted.subList(0, Math.min(8, sorted.size()))) {
            latest.addRow(new Object[]{r.getStudentName(), r.getCourseName(),
                    r.getMarksObtained() + " / " + r.getTotalMarks(), String.format("%.1f", r.getPercentage()),
                    r.getGrade(), r.getExamDate().format(fmt)});
        }
    }
}
