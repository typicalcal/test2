package budgettracker.ui;

import budgettracker.util.Theme;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.BiConsumer;

public class NavigationPanel extends JPanel {
    private int currentYear;
    private int currentMonth;
    private boolean yearlyMode = false;

    private JLabel periodLabel;
    private JComboBox<String> viewSelector;
    private JButton prevBtn, nextBtn, jumpBtn;

    private BiConsumer<Integer, Integer> onPeriodChanged; // (year, month)

    private static final int MAX_FUTURE_YEARS = 5;

    public NavigationPanel(int year, int month, BiConsumer<Integer, Integer> onPeriodChanged) {
        this.currentYear = year;
        this.currentMonth = month;
        this.onPeriodChanged = onPeriodChanged;
        setBackground(Theme.BG_PANEL);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        setLayout(new BorderLayout(10, 0));
        setPreferredSize(new Dimension(0, 56));
        buildUI();
    }

    private void buildUI() {
        // Left: view selector
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        JLabel viewLabel = new JLabel("View:");
        viewLabel.setForeground(Theme.TEXT_SECONDARY);
        viewLabel.setFont(Theme.FONT_SMALL);
        leftPanel.add(viewLabel);

        viewSelector = new JComboBox<>(new String[]{"Monthly", "Yearly"});
        viewSelector.setPreferredSize(new Dimension(110, 30));
        Theme.styleComboBox(viewSelector);
        viewSelector.addActionListener(e -> {
            boolean wasYearly = yearlyMode;
            yearlyMode = viewSelector.getSelectedIndex() == 1;
            if (wasYearly != yearlyMode) {
                updateLabel();
                onPeriodChanged.accept(currentYear, currentMonth);
            }
        });
        leftPanel.add(viewSelector);
        add(leftPanel, BorderLayout.WEST);

        // Center: nav controls
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        centerPanel.setOpaque(false);

        prevBtn = makeArrowButton(false);
        prevBtn.addActionListener(e -> navigate(-1));
        centerPanel.add(prevBtn);

        periodLabel = new JLabel();
        periodLabel.setFont(Theme.FONT_LARGE);
        periodLabel.setForeground(Theme.TEXT_PRIMARY);
        periodLabel.setPreferredSize(new Dimension(200, 30));
        periodLabel.setHorizontalAlignment(SwingConstants.CENTER);
        centerPanel.add(periodLabel);

        nextBtn = makeArrowButton(true);
        nextBtn.addActionListener(e -> navigate(1));
        centerPanel.add(nextBtn);

        jumpBtn = makeJumpButton();
        jumpBtn.setToolTipText("Jump to month/year");
        jumpBtn.addActionListener(e -> showJumpDialog());
        centerPanel.add(jumpBtn);

        add(centerPanel, BorderLayout.CENTER);

        // Right: spacer to balance layout
        JPanel rightPanel = new JPanel();
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(160, 30));
        add(rightPanel, BorderLayout.EAST);

        updateLabel();
    }

    /** Arrow button drawn with Graphics2D — avoids any font/HTML rendering issues. */
    private JButton makeArrowButton(boolean pointRight) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Background
                Color bg = getModel().isPressed() ? Theme.BG_SELECTED :
                           getModel().isRollover() ? Theme.BG_HOVER : Theme.BG_CARD;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                // Arrow triangle
                int w = getWidth(), h = getHeight();
                int mx = w / 2, my = h / 2;
                int aw = 7, ah = 9; // arrow half-width, half-height
                int[] xs, ys;
                if (pointRight) {
                    xs = new int[]{mx - aw, mx - aw, mx + aw};
                    ys = new int[]{my - ah, my + ah, my};
                } else {
                    xs = new int[]{mx + aw, mx + aw, mx - aw};
                    ys = new int[]{my - ah, my + ah, my};
                }
                g2.setColor(getModel().isRollover() ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY);
                g2.fillPolygon(xs, ys, 3);
                g2.dispose();
            }
        };
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setPreferredSize(new Dimension(34, 30));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** Jump button with three dots drawn via Graphics2D. */
    private JButton makeJumpButton() {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed() ? Theme.BG_SELECTED :
                           getModel().isRollover() ? Theme.BG_HOVER : Theme.BG_CARD;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                // Three dots
                g2.setColor(getModel().isRollover() ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY);
                int cx = getWidth() / 2, cy = getHeight() / 2, r = 3, gap = 7;
                g2.fillOval(cx - gap - r, cy - r, r * 2, r * 2);
                g2.fillOval(cx - r,       cy - r, r * 2, r * 2);
                g2.fillOval(cx + gap - r, cy - r, r * 2, r * 2);
                g2.dispose();
            }
        };
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setPreferredSize(new Dimension(36, 30));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void navigate(int direction) {
        LocalDate now = LocalDate.now();
        int maxYear = now.getYear() + MAX_FUTURE_YEARS;

        if (yearlyMode) {
            int newYear = currentYear + direction;
            if (newYear > maxYear) return;
            currentYear = newYear;
        } else {
            int newMonth = currentMonth + direction;
            int newYear = currentYear;
            if (newMonth < 1) { newMonth = 12; newYear--; }
            if (newMonth > 12) { newMonth = 1; newYear++; }
            if (newYear > maxYear || (newYear == maxYear && newMonth > now.getMonthValue())) return;
            currentMonth = newMonth;
            currentYear = newYear;
        }
        updateLabel();
        onPeriodChanged.accept(currentYear, currentMonth);
    }

    private void showJumpDialog() {
        LocalDate now = LocalDate.now();
        int maxYear = now.getYear() + MAX_FUTURE_YEARS;

        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Jump to Period", true);
        dlg.getContentPane().setBackground(Theme.BG_PANEL);
        dlg.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        // Year spinner
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel yl = new JLabel("Year:");
        yl.setForeground(Theme.TEXT_SECONDARY);
        yl.setFont(Theme.FONT_MEDIUM);
        form.add(yl, gbc);

        gbc.gridx = 1;
        SpinnerNumberModel yearModel = new SpinnerNumberModel(currentYear, 1900, maxYear, 1);
        JSpinner yearSpin = new JSpinner(yearModel);
        yearSpin.setPreferredSize(new Dimension(90, 30));
        styleSpinner(yearSpin);
        form.add(yearSpin, gbc);

        // Month combo (hidden in yearly mode)
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel ml = new JLabel("Month:");
        ml.setForeground(Theme.TEXT_SECONDARY);
        ml.setFont(Theme.FONT_MEDIUM);

        String[] monthNames = new String[12];
        for (int i = 1; i <= 12; i++)
            monthNames[i - 1] = Month.of(i).getDisplayName(TextStyle.FULL, Locale.getDefault());
        JComboBox<String> monthCombo = new JComboBox<>(monthNames);
        monthCombo.setSelectedIndex(currentMonth - 1);
        monthCombo.setPreferredSize(new Dimension(140, 30));
        Theme.styleComboBox(monthCombo);

        if (!yearlyMode) {
            form.add(ml, gbc);
            gbc.gridx = 1;
            form.add(monthCombo, gbc);
        }

        dlg.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btnPanel.setOpaque(false);
        JButton cancelBtn = Theme.makeSecondaryButton("Cancel");
        JButton goBtn = Theme.makeButton("Go", Theme.ACCENT_BLUE);
        cancelBtn.addActionListener(e -> dlg.dispose());
        goBtn.addActionListener(e -> {
            int y = (int) yearSpin.getValue();
            int m = yearlyMode ? currentMonth : monthCombo.getSelectedIndex() + 1;
            // Clamp to max future
            if (y > maxYear) y = maxYear;
            if (y == maxYear && !yearlyMode && m > now.getMonthValue()) m = now.getMonthValue();
            currentYear = y;
            currentMonth = m;
            updateLabel();
            onPeriodChanged.accept(currentYear, currentMonth);
            dlg.dispose();
        });
        btnPanel.add(cancelBtn);
        btnPanel.add(goBtn);
        dlg.add(btnPanel, BorderLayout.SOUTH);

        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setBackground(Theme.BG_INPUT);
        spinner.setForeground(Theme.TEXT_PRIMARY);
        spinner.setFont(Theme.FONT_MEDIUM);
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(Theme.BG_INPUT);
            tf.setForeground(Theme.TEXT_PRIMARY);
            tf.setCaretColor(Theme.TEXT_PRIMARY);
            tf.setFont(Theme.FONT_MEDIUM);
            tf.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        }
    }

    private void updateLabel() {
        if (yearlyMode) {
            periodLabel.setText(String.valueOf(currentYear));
        } else {
            String monthName = Month.of(currentMonth).getDisplayName(TextStyle.FULL, Locale.getDefault());
            periodLabel.setText(monthName + " " + currentYear);
        }
    }

    public boolean isYearlyMode() { return yearlyMode; }
    public int getCurrentYear() { return currentYear; }
    public int getCurrentMonth() { return currentMonth; }

    public void setPeriod(int year, int month) {
        this.currentYear = year;
        this.currentMonth = month;
        updateLabel();
    }
}
