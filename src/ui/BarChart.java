package ui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

/** Small dependency-free bar chart (vertical or horizontal) drawn with Java2D. */
public class BarChart extends JComponent {
    private final boolean horizontal;
    private final List<String> labels = new ArrayList<>();
    private final List<Double> values = new ArrayList<>();
    private Color[] colors = { Theme.PRIMARY };
    private double max = 0;           // 0 = auto
    private String valueSuffix = "";
    private String emptyText = "No data yet";

    public BarChart(boolean horizontal) {
        this.horizontal = horizontal;
        setOpaque(false);
        setPreferredSize(new Dimension(300, 240));
    }

    public BarChart setData(List<String> labels, List<Double> values) {
        this.labels.clear(); this.labels.addAll(labels);
        this.values.clear(); this.values.addAll(values);
        repaint();
        return this;
    }

    public BarChart colors(Color... c) { this.colors = c; return this; }
    public BarChart fixedMax(double m) { this.max = m; return this; }
    public BarChart suffix(String s) { this.valueSuffix = s; return this; }
    public BarChart emptyText(String s) { this.emptyText = s; return this; }

    private Color colorFor(int i) { return colors[i % colors.length]; }

    private String fmt(double v) {
        return (v == Math.rint(v) ? String.valueOf((long) v) : String.format("%.1f", v)) + valueSuffix;
    }

    private String fit(String s, FontMetrics fm, int width) {
        if (fm.stringWidth(s) <= width) return s;
        while (s.length() > 1 && fm.stringWidth(s + "…") > width) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(Theme.SMALL);
        FontMetrics fm = g2.getFontMetrics();
        int w = getWidth(), h = getHeight();

        double top = max;
        if (top <= 0) for (double v : values) top = Math.max(top, v);
        if (values.isEmpty() || top <= 0) {
            g2.setColor(Theme.MUTED);
            g2.drawString(emptyText, (w - fm.stringWidth(emptyText)) / 2, h / 2);
            g2.dispose();
            return;
        }

        int n = values.size();
        if (horizontal) {
            int labelW = Math.min(w / 3, 130), valueW = 52, rowH = h / n;
            int barH = Math.max(8, Math.min(22, rowH - 10));
            for (int i = 0; i < n; i++) {
                int y = i * rowH + (rowH - barH) / 2;
                int barMax = w - labelW - valueW - 8;
                int bw = (int) Math.round(barMax * Math.min(values.get(i), top) / top);
                g2.setColor(Theme.TEXT);
                g2.drawString(fit(labels.get(i), fm, labelW - 8), 0, y + (barH + fm.getAscent()) / 2 - 2);
                g2.setColor(new Color(238, 240, 245));
                g2.fillRoundRect(labelW, y, barMax, barH, 8, 8);
                g2.setColor(colorFor(i));
                g2.fillRoundRect(labelW, y, Math.max(bw, 4), barH, 8, 8);
                g2.setColor(Theme.MUTED);
                g2.drawString(fmt(values.get(i)), labelW + barMax + 8, y + (barH + fm.getAscent()) / 2 - 2);
            }
        } else {
            int bottom = h - 24, topPad = 20;
            int slot = w / n, barW = Math.max(10, Math.min(48, slot - 14));
            g2.setColor(Theme.BORDER);
            g2.drawLine(0, bottom, w, bottom);
            for (int i = 0; i < n; i++) {
                int x = i * slot + (slot - barW) / 2;
                int bh = (int) Math.round((bottom - topPad) * Math.min(values.get(i), top) / top);
                if (values.get(i) > 0) {
                    g2.setColor(colorFor(i));
                    g2.fillRoundRect(x, bottom - bh, barW, bh, 8, 8);
                }
                String v = fmt(values.get(i));
                g2.setColor(Theme.TEXT);
                g2.drawString(v, x + (barW - fm.stringWidth(v)) / 2, bottom - bh - 5);
                String l = fit(labels.get(i), fm, slot - 4);
                g2.setColor(Theme.MUTED);
                g2.drawString(l, i * slot + (slot - fm.stringWidth(l)) / 2, bottom + 16);
            }
        }
        g2.dispose();
    }
}
