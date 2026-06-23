package budgettracker.ui;

import budgettracker.data.*;
import budgettracker.util.BudgetCalculator;
import budgettracker.util.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class DonutChartPanel extends JPanel {
    private AppData appData;
    private int year, month;
    private boolean yearlyMode;
    private String selectedCategory = null;
    private int hoveredIndex = -1;

    // Tooltip state
    private Point tooltipPos = null;
    private String[] tooltipLines = null;

    // Data for rendering
    private List<SliceData> slices = new ArrayList<>();
    private Consumer<String> onCategoryClicked;

    private static class SliceData {
        String name;
        double allocated;
        double spent;
        Color color;
        double startAngle;
        double sweepAngle;
        SliceData(String n, double a, double s, Color c, double sa, double sw) {
            name = n; allocated = a; spent = s; color = c; startAngle = sa; sweepAngle = sw;
        }
    }

    public DonutChartPanel(AppData data, int year, int month, boolean yearlyMode,
                           Consumer<String> onCategoryClicked) {
        this.appData = data;
        this.year = year;
        this.month = month;
        this.yearlyMode = yearlyMode;
        this.onCategoryClicked = onCategoryClicked;
        setBackground(Theme.BG_PANEL);
        setPreferredSize(new Dimension(380, 380));
        buildSlices();
        setupInteraction();
    }

    public void update(AppData data, int year, int month, boolean yearlyMode) {
        this.appData = data;
        this.year = year;
        this.month = month;
        this.yearlyMode = yearlyMode;
        hoveredIndex = -1;
        tooltipLines = null;
        buildSlices();
        repaint();
    }

    public void setSelectedCategory(String name) {
        this.selectedCategory = name;
        repaint();
    }

    private void buildSlices() {
        slices.clear();
        double totalAlloc;

        if (yearlyMode) {
            Map<String, double[]> agg = appData.getYearlyAggregation(year);
            totalAlloc = agg.values().stream().mapToDouble(a -> a[0]).sum();
            if (totalAlloc <= 0) {
                // Draw empty donut
                return;
            }
            double angle = 90; // start from top
            for (Map.Entry<String, double[]> e : agg.entrySet()) {
                String name = e.getKey();
                double alloc = e.getValue()[0];
                double spent = e.getValue()[1];
                if (alloc <= 0) continue;
                double sweep = (alloc / totalAlloc) * 360.0;
                Color c = Theme.hexToColor(appData.getCategoryColor(name));
                slices.add(new SliceData(name, alloc, spent, c, angle, sweep));
                angle -= sweep;
            }
        } else {
            MonthData md = appData.getOrCreateMonth(year, month);
            totalAlloc = md.getTotalAllocated();
            if (totalAlloc <= 0) return;
            double angle = 90;
            for (Category cat : md.getCategories()) {
                if (cat.getAllocatedBudget() <= 0) continue;
                double sweep = (cat.getAllocatedBudget() / totalAlloc) * 360.0;
                Color c = Theme.hexToColor(appData.getCategoryColor(cat.getName()));
                double spent = md.getSpentForCategory(cat.getName());
                slices.add(new SliceData(cat.getName(), cat.getAllocatedBudget(), spent, c, angle, sweep));
                angle -= sweep;
            }
        }
    }

    private void setupInteraction() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int prev = hoveredIndex;
                hoveredIndex = getSliceAt(e.getX(), e.getY());
                if (hoveredIndex >= 0) {
                    tooltipPos = e.getPoint();
                    buildTooltip(hoveredIndex);
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    tooltipLines = null;
                    tooltipPos = null;
                    setCursor(Cursor.getDefaultCursor());
                }
                if (prev != hoveredIndex) repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int idx = getSliceAt(e.getX(), e.getY());
                if (idx >= 0) {
                    selectedCategory = slices.get(idx).name;
                    repaint();
                    if (onCategoryClicked != null)
                        onCategoryClicked.accept(slices.get(idx).name);
                } else {
                    selectedCategory = null;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoveredIndex = -1;
                tooltipLines = null;
                repaint();
            }
        });
    }

    private void buildTooltip(int idx) {
        if (idx < 0 || idx >= slices.size()) { tooltipLines = null; return; }
        SliceData s = slices.get(idx);
        String sym = appData.getSettings().getCurrencySymbol();
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        double remaining = s.allocated - s.spent;
        tooltipLines = new String[]{
            s.name,
            "Allocated: " + sym + nf.format(s.allocated),
            "Spent:     " + sym + nf.format(s.spent),
            "Remaining: " + sym + nf.format(remaining)
        };
    }

    private int getSliceAt(int mx, int my) {
        int w = getWidth(), h = getHeight();
        int cx = w / 2, cy = h / 2;
        int outerR = Math.min(w, h) / 2 - 24;
        int innerR = (int)(outerR * 0.55);

        double dx = mx - cx, dy = my - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < innerR || dist > outerR) return -1;

        double angle = Math.toDegrees(Math.atan2(-dy, dx));
        if (angle < 0) angle += 360;

        for (int i = 0; i < slices.size(); i++) {
            SliceData s = slices.get(i);
            double start = s.startAngle % 360;
            if (start < 0) start += 360;
            double end = start - s.sweepAngle;

            // Normalize
            double testAngle = angle;
            // Convert: our arcs go counter-clockwise from start
            // Graphics arc: startAngle measured from 3 o'clock, CCW positive
            // Our startAngle is from 12 o'clock, CW in degrees but stored as subtracted
            // Let's do: for each slice, compute arc start/end in standard math coords
            double arcStart = start; // in math coords (CCW from east = 0)
            double arcEnd = end;

            // Normalize both to [0,360)
            while (arcStart < 0) arcStart += 360;
            while (arcEnd < 0) arcEnd += 360;
            arcStart = arcStart % 360;
            arcEnd = arcEnd % 360;

            boolean inSlice;
            if (arcStart >= arcEnd) {
                inSlice = (testAngle >= arcEnd && testAngle <= arcStart);
            } else {
                inSlice = (testAngle >= arcStart || testAngle <= arcEnd);
            }
            if (inSlice) return i;
        }
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int cx = w / 2, cy = h / 2;
        int outerR = Math.min(w, h) / 2 - 24;
        int innerR = (int)(outerR * 0.55);

        if (slices.isEmpty()) {
            drawEmptyState(g2, cx, cy, outerR, innerR);
            g2.dispose();
            return;
        }

        // Draw slices
        for (int i = 0; i < slices.size(); i++) {
            SliceData s = slices.get(i);
            boolean isHovered = (i == hoveredIndex);
            boolean isSelected = s.name.equals(selectedCategory);

            int expand = (isHovered || isSelected) ? 8 : 0;
            int r = outerR + expand;
            int ir = innerR;

            // Offset slice outward when selected/hovered
            int ox = 0, oy = 0;
            if (isSelected || isHovered) {
                double midAngle = Math.toRadians(s.startAngle - s.sweepAngle / 2.0);
                ox = (int)(Math.cos(midAngle) * 6);
                oy = (int)(-Math.sin(midAngle) * 6);
            }

            int x = cx - r + ox, y = cy - r + oy;
            int size = r * 2;

            // Gap between slices
            double gapDeg = slices.size() > 1 ? 1.5 : 0;
            double startAdj = s.startAngle - gapDeg / 2.0;
            double sweepAdj = s.sweepAngle - gapDeg;

            // Draw outer arc
            Color c = isHovered ? s.color.brighter() : s.color;
            g2.setColor(c);
            g2.fill(new Arc2D.Double(x, y, size, size, startAdj, -sweepAdj, Arc2D.PIE));

            // Cut out inner circle to create donut
            // We'll do this after drawing all slices

            // Draw border if selected
            if (isSelected) {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new Arc2D.Double(x + 1, y + 1, size - 2, size - 2, startAdj, -sweepAdj, Arc2D.PIE));
            }
        }

        // Punch out inner circle (donut hole)
        g2.setColor(getBackground());
        g2.fillOval(cx - innerR, cy - innerR, innerR * 2, innerR * 2);

        // Draw center text
        drawCenterText(g2, cx, cy, innerR);

        // Draw tooltip
        if (tooltipLines != null && tooltipPos != null) {
            drawTooltip(g2, tooltipPos.x, tooltipPos.y);
        }

        g2.dispose();
    }

    private void drawCenterText(Graphics2D g2, int cx, int cy, int innerR) {
        String sym = appData.getSettings().getCurrencySymbol();
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(0);

        double income, totalAlloc;
        if (yearlyMode) {
            income = appData.getYearlyIncome(year);
            Map<String, double[]> agg = appData.getYearlyAggregation(year);
            totalAlloc = agg.values().stream().mapToDouble(a -> a[0]).sum();
        } else {
            MonthData md = appData.getOrCreateMonth(year, month);
            income = md.getTotalIncome();
            totalAlloc = md.getTotalAllocated();
        }
        double remaining = income - totalAlloc;

        boolean isNegative = remaining < 0;
        Color remColor = isNegative ? Theme.TEXT_RED : Theme.TEXT_GREEN;

        // "Remaining" label
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_MUTED);
        String label = "Remaining";
        FontMetrics fms = g2.getFontMetrics();
        g2.drawString(label, cx - fms.stringWidth(label) / 2, cy - 18);

        // Remaining value
        String remStr = sym + nf.format(Math.abs(remaining));
        if (isNegative) remStr = "-" + remStr;
        g2.setFont(new Font("Segoe UI", Font.BOLD, innerR > 80 ? 18 : 14));
        g2.setColor(remColor);
        FontMetrics fmr = g2.getFontMetrics();
        g2.drawString(remStr, cx - fmr.stringWidth(remStr) / 2, cy + 4);

        // "of $X income" label
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_MUTED);
        String incStr = "of " + sym + nf.format(income);
        FontMetrics fmi = g2.getFontMetrics();
        g2.drawString(incStr, cx - fmi.stringWidth(incStr) / 2, cy + 22);
    }

    private void drawEmptyState(Graphics2D g2, int cx, int cy, int outerR, int innerR) {
        // Draw a dashed circle placeholder
        g2.setColor(Theme.BG_CARD);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
            0, new float[]{8, 6}, 0));
        g2.drawOval(cx - outerR, cy - outerR, outerR * 2, outerR * 2);
        g2.setStroke(new BasicStroke(1f));

        g2.setFont(Theme.FONT_MEDIUM);
        g2.setColor(Theme.TEXT_MUTED);
        String msg = "No categories";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, cx - fm.stringWidth(msg) / 2, cy);
    }

    private void drawTooltip(Graphics2D g2, int mx, int my) {
        if (tooltipLines == null) return;

        Font boldFont = Theme.FONT_BOLD_SM;
        Font normalFont = Theme.FONT_SMALL;

        g2.setFont(normalFont);
        FontMetrics fm = g2.getFontMetrics();

        int padding = 10;
        int lineHeight = fm.getHeight() + 2;
        int maxWidth = 0;
        for (String l : tooltipLines) maxWidth = Math.max(maxWidth, fm.stringWidth(l));
        maxWidth += padding * 2;
        int totalHeight = lineHeight * tooltipLines.length + padding * 2 + 4;

        int tx = mx + 14, ty = my - totalHeight / 2;
        int w = getWidth(), h = getHeight();
        if (tx + maxWidth > w - 8) tx = mx - maxWidth - 14;
        if (ty < 4) ty = 4;
        if (ty + totalHeight > h - 4) ty = h - totalHeight - 4;

        // Shadow
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRoundRect(tx + 3, ty + 3, maxWidth, totalHeight, 10, 10);

        // Background
        g2.setColor(new Color(32, 32, 56, 240));
        g2.fillRoundRect(tx, ty, maxWidth, totalHeight, 10, 10);
        g2.setColor(Theme.BORDER_LIGHT);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(tx, ty, maxWidth, totalHeight, 10, 10);

        // Text
        int lineY = ty + padding + fm.getAscent();
        for (int i = 0; i < tooltipLines.length; i++) {
            if (i == 0) {
                g2.setFont(boldFont);
                g2.setColor(Theme.TEXT_PRIMARY);
            } else {
                g2.setFont(normalFont);
                g2.setColor(Theme.TEXT_SECONDARY);
            }
            g2.drawString(tooltipLines[i], tx + padding, lineY);
            lineY += lineHeight + (i == 0 ? 4 : 0);
        }
    }
}
