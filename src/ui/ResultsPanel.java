package ui;

import dao.ResultDAO;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.Result;
import model.Student;

/** Look up results by roll number (or show everyone's) and add new results. */
public class ResultsPanel extends JPanel implements Page {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private final JTextField tfRoll = Theme.textField(14);
    private final JLabel summary = Theme.label(" ", Theme.BASE, Theme.MUTED);
    private final DefaultTableModel model = Theme.readOnlyModel(
            "Student", "Course", "Marks", "Total", "%", "Grade", "Exam Date", "Semester", "Year");
    private final JTable table = Theme.table(model);

    public ResultsPanel() {
        super(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        JButton add = Theme.button("+ Add Result");
        add.addActionListener(e -> new AddResultDialog(SwingUtilities.getWindowAncestor(this), this::refresh));
        add(Theme.pageHeader("Results", "Search a student by roll number, or view everyone's results.", add), BorderLayout.NORTH);

        JButton search = Theme.button("Search");
        JButton all = Theme.button("Show All", Theme.Kind.SECONDARY);
        search.addActionListener(e -> loadForRoll());
        tfRoll.addActionListener(e -> loadForRoll());
        all.addActionListener(e -> { tfRoll.setText(""); refresh(); });

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.add(Theme.label("Roll Number:", Theme.BOLD, Theme.TEXT));
        bar.add(tfRoll);
        bar.add(search);
        bar.add(all);

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        top.add(bar, BorderLayout.WEST);
        top.add(summary, BorderLayout.EAST);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.add(top, BorderLayout.NORTH);
        body.add(Theme.tableCard(table), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
        Theme.colorGradeColumn(table, 5);
    }

    /** Reloads whatever is currently shown (all results, or the searched student). */
    @Override
    public void refresh() {
        if (tfRoll.getText().isBlank()) loadAll(); else loadForRoll();
    }

    private void loadAll() {
        note("Loading...", Theme.MUTED);
        Theme.async(() -> new ResultDAO().getAllResults(), this::show,
                err -> note(Theme.describe(err), Theme.DANGER));
    }

    private void loadForRoll() {
        String roll = tfRoll.getText().trim();
        if (roll.isEmpty()) { loadAll(); return; }
        note("Searching...", Theme.MUTED);
        Theme.async(() -> {
            Student s = AddResultDialog.findByRoll(roll);
            return s == null ? null : new ResultDAO().getResultsByStudentId(s.getStudentId());
        }, list -> {
            if (list == null) {
                model.setRowCount(0);
                note("Student not found: " + roll, Theme.DANGER);
            } else if (list.isEmpty()) {
                model.setRowCount(0);
                note("No results found for this student.", Theme.MUTED);
            } else {
                show(list);
            }
        }, err -> note(Theme.describe(err), Theme.DANGER));
    }

    private void show(List<Result> results) {
        model.setRowCount(0);
        double sum = 0;
        for (Result r : results) {
            model.addRow(new Object[]{r.getStudentName(), r.getCourseName(), r.getMarksObtained(), r.getTotalMarks(),
                    String.format("%.2f", r.getPercentage()), r.getGrade(), r.getExamDate().format(DATE),
                    r.getSemester(), r.getAcademicYear()});
            sum += r.getPercentage();
        }
        note(results.isEmpty() ? "No results yet."
                : String.format("%d result(s)  |  average %.1f%%", results.size(), sum / results.size()), Theme.MUTED);
    }

    private void note(String text, Color color) {
        summary.setForeground(color);
        summary.setText(text);
    }
}
