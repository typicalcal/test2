package budgettracker.ui;

import budgettracker.data.*;
import budgettracker.util.BudgetCalculator;
import budgettracker.util.Theme;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Map;
import java.util.function.Consumer;

public class SummaryPanel extends JPanel {
    private AppData appData;
    private int year, month;
    private boolean yearlyMode;

    private JLabel incomeVal, allocVal, spentVal, remainVal;
    private Runnable onAddIncome, onAddExpense, onHistory, onSettings;

    public SummaryPanel(AppData data, int year, int month, boolean yearlyMode,
                        Runnable onAddIncome, Runnable onAddExpense,
                        Runnable onHistory, Runnable onSettings) {
        this.appData = data;
        this.year = year;
        this.month = month;
        this.yearlyMode = yearlyMode;
        this.onAddIncome = onAddIncome;
        this.onAddExpense = onAddExpense;
        this.onHistory = onHistory;
        this.onSettings = onSettings;

        setBackground(Theme.BG_PANEL);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER));
        setLayout(new BorderLayout(0, 0));
        setPreferredSize(new Dimension(0, 90));

        buildUI();
    }

    private void buildUI() {
        // Financial summary (top part)
        JPanel summary = new JPanel(new GridLayout(1, 4, 1, 0));
        summary.setOpaque(false);
        summary.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
            BorderFactory.createEmptyBorder(8, 0, 8, 0)
        ));

        incomeVal  = new JLabel("$0");
        allocVal   = new JLabel("$0");
        spentVal   = new JLabel("$0");
        remainVal  = new JLabel("$0");

        summary.add(makeStat("Total Income",      incomeVal, Theme.ACCENT_GREEN));
        summary.add(makeStat("Total Allocated",   allocVal,  Theme.ACCENT_BLUE));
        summary.add(makeStat("Total Spent",       spentVal,  Theme.ACCENT_ORANGE));
        summary.add(makeStat("Remaining",         remainVal, Theme.ACCENT_GREEN));
        add(summary, BorderLayout.CENTER);

        // Action buttons (bottom part)
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        actions.setOpaque(false);

        actions.add(makeActionBtn("+ Income",      Theme.ACCENT_GREEN,  onAddIncome));
        actions.add(makeActionBtn("+ Expense",     Theme.ACCENT_ORANGE, onAddExpense));
        actions.add(makeActionBtn("Transactions",  Theme.ACCENT_BLUE,   onHistory));
        actions.add(makeActionBtn("Settings",      Theme.BG_BUTTON,     onSettings));
        add(actions, BorderLayout.SOUTH);
    }

    private JPanel makeStat(String label, JLabel valueLabel, Color color) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));

        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(Theme.FONT_SMALL);
        lbl.setForeground(Theme.TEXT_MUTED);
        p.add(lbl);

        valueLabel.setFont(Theme.FONT_BOLD);
        valueLabel.setForeground(color);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(valueLabel);
        return p;
    }

    private JButton makeActionBtn(String text, Color color, Runnable action) {
        JButton btn = Theme.makeButton(text, color);
        btn.setPreferredSize(new Dimension(120, 28));
        if (action != null) btn.addActionListener(e -> action.run());
        return btn;
    }

    public void refresh(AppData data, int year, int month, boolean yearlyMode) {
        this.appData = data;
        this.year = year;
        this.month = month;
        this.yearlyMode = yearlyMode;

        String sym = data.getSettings().getCurrencySymbol();
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        double income, allocated, spent, remaining;

        if (yearlyMode) {
            BudgetCalculator.YearlySummary ys = BudgetCalculator.computeYearly(data, year);
            income = ys.totalIncome;
            allocated = ys.totalAllocated;
            spent = ys.totalSpent;
            remaining = ys.remaining;
        } else {
            MonthData md = data.getOrCreateMonth(year, month);
            income = md.getTotalIncome();
            allocated = md.getTotalAllocated();
            spent = md.getTotalSpent();
            remaining = income - allocated;
        }

        incomeVal.setText(sym + nf.format(income));
        allocVal.setText(sym + nf.format(allocated));
        spentVal.setText(sym + nf.format(spent));

        boolean neg = remaining < 0;
        remainVal.setText((neg ? "-" : "") + sym + nf.format(Math.abs(remaining)));
        remainVal.setForeground(neg ? Theme.TEXT_RED : Theme.ACCENT_GREEN);
    }
}
