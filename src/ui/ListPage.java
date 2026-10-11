package ui;

import java.awt.*;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * Reusable "table with search box and Add button" screen. Students and Courses
 * only describe their columns, how to load rows, and what the Add button does.
 */
public abstract class ListPage extends JPanel implements Page {
    private final DefaultTableModel model;
    protected final JTable table;
    private final JTextField search = Theme.textField(22);
    private final JLabel status = Theme.label(" ", Theme.SMALL, Theme.MUTED);

    protected ListPage(String title, String subtitle, String addText, String... columns) {
        super(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        JButton add = Theme.button(addText);
        add.addActionListener(e -> onAdd());
        JComponent[] extra = extraButtons();
        JComponent[] all = new JComponent[extra.length + 1];
        System.arraycopy(extra, 0, all, 0, extra.length);
        all[extra.length] = add;
        add(Theme.pageHeader(title, subtitle, all), BorderLayout.NORTH);

        model = Theme.readOnlyModel(columns);
        table = Theme.table(model);

        search.putClientProperty("JTextField.placeholderText", "Search...");
        search.setToolTipText("Type to filter the table");
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setOpaque(false);
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBox.setOpaque(false);
        searchBox.add(Theme.label("Search:", Theme.BOLD, Theme.TEXT));
        searchBox.add(search);
        bar.add(searchBox, BorderLayout.WEST);
        bar.add(status, BorderLayout.EAST);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.add(bar, BorderLayout.NORTH);
        body.add(Theme.tableCard(table), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    /** Load all rows (called off the UI thread). */
    protected abstract List<Object[]> fetchRows() throws Exception;

    /** Optional extra buttons shown left of the Add button (called while constructing). */
    protected JComponent[] extraButtons() { return new JComponent[0]; }

    /** The Add button was clicked. */
    protected abstract void onAdd();

    @Override
    public void refresh() {
        status.setForeground(Theme.MUTED);
        status.setText("Loading...");
        Theme.async(this::fetchRows, rows -> {
            model.setRowCount(0);
            for (Object[] r : rows) model.addRow(r);
            afterLoad();
            updateStatus();
        }, err -> {
            status.setForeground(Theme.DANGER);
            status.setText(Theme.describe(err));
        });
    }

    /** Hook for subclasses, e.g. to color a column once the rows exist. */
    protected void afterLoad() {}

    @SuppressWarnings("unchecked")
    private void applyFilter() {
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) table.getRowSorter();
        String q = search.getText().trim();
        sorter.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(q)));
        updateStatus();
    }

    private void updateStatus() {
        status.setForeground(Theme.MUTED);
        status.setText(table.getRowCount() + " of " + model.getRowCount() + " shown");
    }
}
