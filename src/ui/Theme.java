package ui;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/**
 * Shared look-and-feel: colors, fonts and small reusable widgets, so every
 * screen looks the same and the page classes stay short.
 */
public final class Theme {
    private Theme() {}

    // ---- Colors -----------------------------------------------------------
    public static final Color PRIMARY = new Color(79, 70, 229);
    public static final Color PRIMARY_DARK = new Color(67, 56, 202);
    public static final Color SIDEBAR = new Color(17, 24, 39);
    public static final Color SIDEBAR_HOVER = new Color(31, 41, 55);
    public static final Color BG = new Color(243, 244, 246);
    public static final Color CARD = Color.WHITE;
    public static final Color TEXT = new Color(17, 24, 39);
    public static final Color MUTED = new Color(107, 114, 128);
    public static final Color BORDER = new Color(229, 231, 235);
    public static final Color SUCCESS = new Color(22, 163, 74);
    public static final Color WARNING = new Color(217, 119, 6);
    public static final Color DANGER = new Color(220, 38, 38);
    public static final Color STRIPE = new Color(249, 250, 251);

    // ---- Fonts ------------------------------------------------------------
    public static final Font BASE = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    public static final Font BOLD = BASE.deriveFont(Font.BOLD);
    public static final Font H1 = BASE.deriveFont(Font.BOLD, 24f);
    public static final Font H2 = BASE.deriveFont(Font.BOLD, 16f);
    public static final Font BIG_NUMBER = BASE.deriveFont(Font.BOLD, 28f);
    public static final Font SMALL = BASE.deriveFont(12f);

    /** Call once at start-up, before any window is created. */
    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // fall back to the default look and feel
        }
        for (Object key : UIManager.getLookAndFeelDefaults().keySet().toArray()) {
            if (key.toString().endsWith(".font")) UIManager.put(key, BASE);
        }
        UIManager.put("OptionPane.messageFont", BASE);
        UIManager.put("OptionPane.buttonFont", BASE);
    }

    /** Grade -> color used in tables and charts (same meaning everywhere). */
    public static Color gradeColor(String grade) {
        if (grade == null) return MUTED;
        switch (grade) {
            case "A+": case "A": return SUCCESS;
            case "B+": case "B": return new Color(13, 148, 136);
            case "C": return new Color(202, 138, 4);
            case "D": return WARNING;
            default: return DANGER;
        }
    }

    // ---- Text helpers -----------------------------------------------------
    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    public static JLabel title(String text) { return label(text, H1, TEXT); }
    public static JLabel muted(String text) { return label(text, BASE, MUTED); }

    // ---- Buttons ----------------------------------------------------------
    public enum Kind { PRIMARY, SECONDARY }

    public static JButton button(String text, Kind kind) {
        return new RoundedButton(text, kind);
    }

    public static JButton button(String text) { return button(text, Kind.PRIMARY); }

    private static class RoundedButton extends JButton {
        private final Kind kind;

        RoundedButton(String text, Kind kind) {
            super(text);
            this.kind = kind;
            setFont(BOLD);
            setForeground(kind == Kind.PRIMARY ? Color.WHITE : TEXT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 18, 9, 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean down = getModel().isArmed() || getModel().isPressed();
            boolean hover = getModel().isRollover();
            if (kind == Kind.PRIMARY) {
                g2.setColor(down || hover ? PRIMARY_DARK : PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            } else {
                g2.setColor(down || hover ? STRIPE : CARD);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ---- Cards and layout -------------------------------------------------
    /** White rounded panel with a thin border; use as the container for any "card". */
    public static class Card extends JPanel {
        public Card() { this(new BorderLayout()); }

        public Card(LayoutManager layout) {
            super(layout);
            setOpaque(false);
            setBorder(new EmptyBorder(16, 18, 16, 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CARD);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Page title + subtitle on the left, optional actions on the right. */
    public static JPanel pageHeader(String title, String subtitle, JComponent... actions) {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(title(title));
        text.add(Box.createVerticalStrut(2));
        text.add(muted(subtitle));
        header.add(text, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        for (JComponent a : actions) right.add(a);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    /** Grid whose column count adapts to its width (1 / 2 / n columns). */
    public static class ResponsiveGrid extends JPanel {
        private final int maxCols;
        private final int minCellWidth;

        public ResponsiveGrid(int maxCols, int minCellWidth, int gap) {
            super(new GridLayout(0, maxCols, gap, gap));
            this.maxCols = maxCols;
            this.minCellWidth = minCellWidth;
            setOpaque(false);
            addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent e) { adapt(); }
            });
        }

        private void adapt() {
            int cols = Math.max(1, Math.min(maxCols, getWidth() / minCellWidth));
            GridLayout gl = (GridLayout) getLayout();
            if (gl.getColumns() != cols) {
                gl.setColumns(cols);
                revalidate();
            }
        }
    }

    /** Panel that follows the viewport width, so a scroll pane only scrolls vertically. */
    public static class ScrollPage extends JPanel implements Scrollable {
        public ScrollPage(LayoutManager lm) { super(lm); setOpaque(false); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 80; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    public static JScrollPane scroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(null);
        sp.getViewport().setBackground(BG);
        sp.setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    // ---- Form inputs ------------------------------------------------------
    public static <T extends JComponent> T styleInput(T c) {
        c.setFont(BASE);
        c.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(6, 8, 6, 8)));
        if (c instanceof JComboBox) c.setBackground(Color.WHITE);
        return c;
    }

    public static JTextField textField(int columns) { return styleInput(new JTextField(columns)); }

    // ---- Tables -----------------------------------------------------------
    public static JTable table(DefaultTableModel model) {
        JTable t = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) c.setBackground(row % 2 == 0 ? CARD : STRIPE);
                return c;
            }
        };
        t.setRowHeight(32);
        t.setFont(BASE);
        t.setForeground(TEXT);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(new Color(224, 231, 255));
        t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);

        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(0, 36));
        h.setDefaultRenderer((tbl, v, sel, foc, row, col) -> {
            JLabel l = new JLabel(v == null ? "" : v.toString());
            l.setOpaque(true);
            l.setFont(BOLD);
            l.setForeground(MUTED);
            l.setBackground(STRIPE);
            l.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(0, 10, 0, 10)));
            return l;
        });

        t.setDefaultRenderer(Object.class, new CellRenderer());
        t.setDefaultRenderer(Number.class, new CellRenderer());
        return t;
    }

    /** Left-aligned, padded cells so values line up under their column headers. */
    private static class CellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int row, int col) {
            super.getTableCellRendererComponent(t, v, s, false, row, col);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            setHorizontalAlignment(SwingConstants.LEFT);
            return this;
        }
    }

    /** A card that contains a scrolling table. */
    public static JComponent tableCard(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new LineBorder(BORDER, 1, true));
        sp.getViewport().setBackground(CARD);
        return sp;
    }

    /** Colors the text of a grade column using {@link #gradeColor}. */
    public static void colorGradeColumn(JTable table, int modelColumn) {
        DefaultTableCellRenderer r = new CellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int row, int col) {
                super.getTableCellRendererComponent(t, v, s, f, row, col);
                setForeground(gradeColor(v == null ? null : v.toString()));
                setFont(BOLD);
                return this;
            }
        };
        table.getColumnModel().getColumn(table.convertColumnIndexToView(modelColumn)).setCellRenderer(r);
    }

    public static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    // ---- Dialogs ----------------------------------------------------------
    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Student Result System", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Student Result System", JOptionPane.ERROR_MESSAGE);
    }

    /** Friendly message for the most common failure: the database is not reachable. */
    public static String describe(Throwable t) {
        if (t instanceof NullPointerException) {
            return "Could not connect to the database. Check that the SQLite JDBC driver is in lib, "
                 + "the database file exists, and DB_URL is correct.";
        }
        return "Something went wrong: " + (t.getMessage() == null ? t.toString() : t.getMessage());
    }

    // ---- Background work --------------------------------------------------
    /** Runs {@code work} off the UI thread, then hands the result (or error) back on it. */
    public static <T> void async(Callable<T> work, Consumer<T> onDone, Consumer<Throwable> onError) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return work.call(); }
            @Override protected void done() {
                try {
                    onDone.accept(get());
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    cause.printStackTrace();
                    onError.accept(cause);
                }
            }
        }.execute();
    }
}
