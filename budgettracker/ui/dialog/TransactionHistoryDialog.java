package budgettracker.ui.dialog;

import budgettracker.data.*;
import budgettracker.util.Theme;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionHistoryDialog extends JDialog {
    private AppData appData;
    private MonthData monthData;
    private int year, month;
    private boolean yearlyMode;

    private JTabbedPane tabs;
    private IncomeTablePanel incomePanel;
    private ExpenseTablePanel expensePanel;

    private String filterCategory = null;

    public TransactionHistoryDialog(Frame parent, AppData appData, MonthData monthData,
                                    int year, int month, boolean yearlyMode) {
        super(parent, "Transaction History", true);
        this.appData = appData;
        this.monthData = monthData;
        this.year = year;
        this.month = month;
        this.yearlyMode = yearlyMode;
        buildUI();
        setSize(800, 580);
        setResizable(true);
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        getContentPane().setBackground(Theme.BG_PANEL);
        setLayout(new BorderLayout(0, 0));

        tabs = new JTabbedPane();
        tabs.setBackground(Theme.BG_PANEL);
        tabs.setForeground(Theme.TEXT_PRIMARY);
        tabs.setFont(Theme.FONT_MEDIUM);

        // Collect transactions
        List<IncomeTransaction> incomeList = collectIncome();
        List<ExpenseTransaction> expenseList = collectExpenses();

        incomePanel = new IncomeTablePanel(incomeList);
        expensePanel = new ExpenseTablePanel(expenseList);

        tabs.addTab("Income (" + incomeList.size() + ")", incomePanel);
        tabs.addTab("Expenses (" + expenseList.size() + ")", expensePanel);

        add(tabs, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        footer.setOpaque(false);
        JButton close = Theme.makeSecondaryButton("Close");
        close.addActionListener(e -> dispose());
        footer.add(close);
        add(footer, BorderLayout.SOUTH);
    }

    private List<IncomeTransaction> collectIncome() {
        List<IncomeTransaction> list = new ArrayList<>();
        if (yearlyMode) {
            for (int m = 1; m <= 12; m++) {
                MonthData md = appData.getMonth(year, m);
                if (md != null) list.addAll(md.getIncomeTransactions());
            }
        } else if (monthData != null) {
            list.addAll(monthData.getIncomeTransactions());
        }
        list.sort(Comparator.comparing(IncomeTransaction::getDate).reversed());
        return list;
    }

    private List<ExpenseTransaction> collectExpenses() {
        List<ExpenseTransaction> list = new ArrayList<>();
        if (yearlyMode) {
            for (int m = 1; m <= 12; m++) {
                MonthData md = appData.getMonth(year, m);
                if (md != null) list.addAll(md.getExpenseTransactions());
            }
        } else if (monthData != null) {
            list.addAll(monthData.getExpenseTransactions());
        }
        list.sort(Comparator.comparing(ExpenseTransaction::getDate).reversed());
        return list;
    }

    public void setFilterCategory(String cat) {
        this.filterCategory = cat;
        // Apply immediately since buildUI() already ran
        if (expensePanel != null && tabs != null) {
            tabs.setSelectedIndex(1);
            expensePanel.setFilter(cat);
        }
    }

    // ---- Income Table ----
    private class IncomeTablePanel extends JPanel {
        private List<IncomeTransaction> allData;
        private DefaultTableModel model;
        private JTable table;
        private JTextField searchField;
        private JTextField minAmt, maxAmt;
        private JTextField startDate, endDate;
        private JTextField sourceFilter;

        IncomeTablePanel(List<IncomeTransaction> data) {
            this.allData = new ArrayList<>(data);
            setBackground(Theme.BG_PANEL);
            setLayout(new BorderLayout(0, 6));
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            buildTable();
            buildFilterPanel();
            applyFilter();
        }

        private void buildTable() {
            String[] cols = {"Date", "Source", "Amount"};
            model = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            table = makeTable(model);
            table.getColumnModel().getColumn(0).setPreferredWidth(100);
            table.getColumnModel().getColumn(1).setPreferredWidth(200);
            table.getColumnModel().getColumn(2).setPreferredWidth(120);
            sortByColumn(table, 0, false);

            JScrollPane sp = new JScrollPane(table);
            Theme.styleScrollPane(sp);
            add(sp, BorderLayout.CENTER);

            // Edit / Delete
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
            actions.setOpaque(false);
            JButton editBtn = Theme.makeButton("Edit", Theme.ACCENT_BLUE);
            JButton delBtn  = Theme.makeButton("Delete", Theme.ACCENT_RED);
            editBtn.addActionListener(e -> editSelected());
            delBtn.addActionListener(e -> deleteSelected());
            actions.add(editBtn);
            actions.add(delBtn);

            JButton addBtn = Theme.makeButton("+ Add Income", Theme.ACCENT_GREEN);
            addBtn.addActionListener(e -> {
                AddIncomeDialog dlg = new AddIncomeDialog(
                    (Frame) SwingUtilities.getWindowAncestor(this),
                    appData, year, yearlyMode ? -1 : month, null);
                dlg.setVisible(true);
                if (dlg.wasConfirmed()) { refreshData(); }
            });
            actions.add(Box.createHorizontalStrut(10));
            actions.add(addBtn);
            add(actions, BorderLayout.SOUTH);
        }

        private void buildFilterPanel() {
            JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            filter.setOpaque(false);
            filter.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0,0,1,0, Theme.BORDER),
                BorderFactory.createEmptyBorder(4, 0, 4, 0)
            ));

            filter.add(makeFilterLabel("Search:"));
            searchField = makeFilterField(12);
            filter.add(searchField);

            filter.add(makeFilterLabel("Source:"));
            sourceFilter = makeFilterField(10);
            filter.add(sourceFilter);

            filter.add(makeFilterLabel("Amount:"));
            minAmt = makeFilterField(7); minAmt.setToolTipText("Min amount");
            filter.add(minAmt);
            filter.add(new JLabel("–") {{ setForeground(Theme.TEXT_MUTED); }});
            maxAmt = makeFilterField(7); maxAmt.setToolTipText("Max amount");
            filter.add(maxAmt);

            filter.add(makeFilterLabel("Dates:"));
            startDate = makeFilterField(9); startDate.setToolTipText("Start yyyy-mm-dd");
            filter.add(startDate);
            filter.add(new JLabel("–") {{ setForeground(Theme.TEXT_MUTED); }});
            endDate = makeFilterField(9); endDate.setToolTipText("End yyyy-mm-dd");
            filter.add(endDate);

            JButton applyBtn = Theme.makeButton("Filter", Theme.ACCENT_BLUE);
            applyBtn.addActionListener(e -> applyFilter());
            filter.add(applyBtn);

            JButton resetBtn = Theme.makeSecondaryButton("Reset");
            resetBtn.addActionListener(e -> {
                searchField.setText(""); sourceFilter.setText("");
                minAmt.setText(""); maxAmt.setText("");
                startDate.setText(""); endDate.setText("");
                applyFilter();
            });
            filter.add(resetBtn);
            add(filter, BorderLayout.NORTH);
        }

        void applyFilter() {
            String search = searchField != null ? searchField.getText().trim().toLowerCase() : "";
            String src = sourceFilter != null ? sourceFilter.getText().trim().toLowerCase() : "";
            double minA = parseDouble(minAmt, 0);
            double maxA = parseDouble(maxAmt, Double.MAX_VALUE);
            String sd = startDate != null ? startDate.getText().trim() : "";
            String ed2 = endDate != null ? endDate.getText().trim() : "";

            model.setRowCount(0);
            String sym = appData.getSettings().getCurrencySymbol();
            NumberFormat nf = getAmountFormat();

            for (IncomeTransaction t : allData) {
                if (!search.isEmpty() && !t.getSource().toLowerCase().contains(search) &&
                    !t.getDate().contains(search)) continue;
                if (!src.isEmpty() && !t.getSource().toLowerCase().contains(src)) continue;
                if (t.getAmount() < minA || t.getAmount() > maxA) continue;
                if (!sd.isEmpty() && t.getDate().compareTo(sd) < 0) continue;
                if (!ed2.isEmpty() && t.getDate().compareTo(ed2) > 0) continue;
                model.addRow(new Object[]{t.getDate(), t.getSource(), sym + nf.format(t.getAmount())});
            }
        }

        private void editSelected() {
            int row = table.getSelectedRow();
            if (row < 0) return;
            String date = (String) model.getValueAt(row, 0);
            String source = (String) model.getValueAt(row, 1);
            IncomeTransaction found = allData.stream()
                .filter(t -> t.getDate().equals(date) && t.getSource().equals(source))
                .findFirst().orElse(null);
            if (found == null) return;
            AddIncomeDialog dlg = new AddIncomeDialog(
                (Frame) SwingUtilities.getWindowAncestor(this), appData,
                found.getYear(), found.getMonth(), found);
            dlg.setVisible(true);
            if (dlg.wasConfirmed()) refreshData();
        }

        private void deleteSelected() {
            int row = table.getSelectedRow();
            if (row < 0) return;
            String date = (String) model.getValueAt(row, 0);
            String source = (String) model.getValueAt(row, 1);
            int r = JOptionPane.showConfirmDialog(this,
                "Delete this income transaction?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (r != JOptionPane.YES_OPTION) return;
            for (MonthData md : appData.getMonths().values()) {
                md.getIncomeTransactions().removeIf(t -> t.getDate().equals(date) && t.getSource().equals(source));
            }
            refreshData();
        }

        private void refreshData() {
            allData = new ArrayList<>(collectIncome());
            applyFilter();
            tabs.setTitleAt(0, "Income (" + allData.size() + ")");
        }
    }

    // ---- Expense Table ----
    private class ExpenseTablePanel extends JPanel {
        private List<ExpenseTransaction> allData;
        private DefaultTableModel model;
        private JTable table;
        private JTextField searchField;
        private JTextField minAmt, maxAmt;
        private JTextField startDate, endDate;
        private JTextField descFilter;
        private JComboBox<String> categoryFilter;

        ExpenseTablePanel(List<ExpenseTransaction> data) {
            this.allData = new ArrayList<>(data);
            setBackground(Theme.BG_PANEL);
            setLayout(new BorderLayout(0, 6));
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            buildTable();
            buildFilterPanel();
            applyFilter();
        }

        private void buildTable() {
            String[] cols = {"Date", "Category", "Amount", "Description"};
            model = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            table = makeTable(model);
            table.getColumnModel().getColumn(0).setPreferredWidth(100);
            table.getColumnModel().getColumn(1).setPreferredWidth(160);
            table.getColumnModel().getColumn(2).setPreferredWidth(110);
            table.getColumnModel().getColumn(3).setPreferredWidth(250);
            sortByColumn(table, 0, false);

            JScrollPane sp = new JScrollPane(table);
            Theme.styleScrollPane(sp);
            add(sp, BorderLayout.CENTER);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
            actions.setOpaque(false);
            JButton editBtn = Theme.makeButton("Edit", Theme.ACCENT_BLUE);
            JButton delBtn  = Theme.makeButton("Delete", Theme.ACCENT_RED);
            editBtn.addActionListener(e -> editSelected());
            delBtn.addActionListener(e -> deleteSelected());
            actions.add(editBtn);
            actions.add(delBtn);

            if (!yearlyMode && monthData != null) {
                JButton addBtn = Theme.makeButton("+ Add Expense", Theme.ACCENT_ORANGE);
                addBtn.addActionListener(e -> {
                    AddExpenseDialog dlg = new AddExpenseDialog(
                        (Frame) SwingUtilities.getWindowAncestor(this),
                        appData, monthData, null, null);
                    dlg.setVisible(true);
                    if (dlg.wasConfirmed()) refreshData();
                });
                actions.add(Box.createHorizontalStrut(10));
                actions.add(addBtn);
            }
            add(actions, BorderLayout.SOUTH);
        }

        private void buildFilterPanel() {
            JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            filter.setOpaque(false);
            filter.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0,0,1,0, Theme.BORDER),
                BorderFactory.createEmptyBorder(4,0,4,0)
            ));

            filter.add(makeFilterLabel("Search:"));
            searchField = makeFilterField(10);
            filter.add(searchField);

            filter.add(makeFilterLabel("Category:"));
            Set<String> catNames = allData.stream().map(ExpenseTransaction::getCategoryName)
                .collect(java.util.LinkedHashSet::new, java.util.LinkedHashSet::add, java.util.LinkedHashSet::addAll);
            String[] cats = new String[catNames.size() + 1];
            cats[0] = "(All)";
            int i = 1;
            for (String n : catNames) cats[i++] = n;
            categoryFilter = new JComboBox<>(cats);
            categoryFilter.setPreferredSize(new Dimension(140, 26));
            Theme.styleComboBox(categoryFilter);
            filter.add(categoryFilter);

            filter.add(makeFilterLabel("Desc:"));
            descFilter = makeFilterField(10);
            filter.add(descFilter);

            filter.add(makeFilterLabel("Amount:"));
            minAmt = makeFilterField(7); minAmt.setToolTipText("Min");
            filter.add(minAmt);
            filter.add(new JLabel("–") {{ setForeground(Theme.TEXT_MUTED); }});
            maxAmt = makeFilterField(7); maxAmt.setToolTipText("Max");
            filter.add(maxAmt);

            filter.add(makeFilterLabel("Dates:"));
            startDate = makeFilterField(9); startDate.setToolTipText("yyyy-mm-dd");
            filter.add(startDate);
            filter.add(new JLabel("–") {{ setForeground(Theme.TEXT_MUTED); }});
            endDate = makeFilterField(9); endDate.setToolTipText("yyyy-mm-dd");
            filter.add(endDate);

            JButton applyBtn = Theme.makeButton("Filter", Theme.ACCENT_BLUE);
            applyBtn.addActionListener(e -> applyFilter());
            filter.add(applyBtn);
            JButton resetBtn = Theme.makeSecondaryButton("Reset");
            resetBtn.addActionListener(e -> {
                searchField.setText(""); descFilter.setText("");
                minAmt.setText(""); maxAmt.setText("");
                startDate.setText(""); endDate.setText("");
                categoryFilter.setSelectedIndex(0);
                applyFilter();
            });
            filter.add(resetBtn);
            add(filter, BorderLayout.NORTH);
        }

        void setFilter(String category) {
            if (categoryFilter != null) {
                for (int i = 0; i < categoryFilter.getItemCount(); i++) {
                    if (category.equals(categoryFilter.getItemAt(i))) {
                        categoryFilter.setSelectedIndex(i);
                        break;
                    }
                }
            }
            applyFilter();
        }

        void applyFilter() {
            String search = searchField != null ? searchField.getText().trim().toLowerCase() : "";
            String selCat = categoryFilter != null && categoryFilter.getSelectedIndex() > 0
                ? (String) categoryFilter.getSelectedItem() : null;
            String desc = descFilter != null ? descFilter.getText().trim().toLowerCase() : "";
            double minA = parseDouble(minAmt, 0);
            double maxA = parseDouble(maxAmt, Double.MAX_VALUE);
            String sd = startDate != null ? startDate.getText().trim() : "";
            String ed2 = endDate != null ? endDate.getText().trim() : "";

            model.setRowCount(0);
            String sym = appData.getSettings().getCurrencySymbol();
            NumberFormat nf = getAmountFormat();

            for (ExpenseTransaction t : allData) {
                if (selCat != null && !t.getCategoryName().equals(selCat)) continue;
                if (!desc.isEmpty() && (t.getDescription() == null ||
                    !t.getDescription().toLowerCase().contains(desc))) continue;
                if (!search.isEmpty()) {
                    boolean match = t.getCategoryName().toLowerCase().contains(search) ||
                        t.getDate().contains(search) ||
                        (t.getDescription() != null && t.getDescription().toLowerCase().contains(search));
                    if (!match) continue;
                }
                if (t.getAmount() < minA || t.getAmount() > maxA) continue;
                if (!sd.isEmpty() && t.getDate().compareTo(sd) < 0) continue;
                if (!ed2.isEmpty() && t.getDate().compareTo(ed2) > 0) continue;

                model.addRow(new Object[]{
                    t.getDate(), t.getCategoryName(),
                    sym + nf.format(t.getAmount()),
                    t.getDescription() != null ? t.getDescription() : ""
                });
            }
        }

        private void editSelected() {
            int row = table.getSelectedRow();
            if (row < 0 || yearlyMode || monthData == null) return;
            String date = (String) model.getValueAt(row, 0);
            String cat  = (String) model.getValueAt(row, 1);
            ExpenseTransaction found = allData.stream()
                .filter(t -> t.getDate().equals(date) && t.getCategoryName().equals(cat))
                .findFirst().orElse(null);
            if (found == null) return;
            AddExpenseDialog dlg = new AddExpenseDialog(
                (Frame) SwingUtilities.getWindowAncestor(this), appData, monthData, null, found);
            dlg.setVisible(true);
            if (dlg.wasConfirmed()) refreshData();
        }

        private void deleteSelected() {
            int row = table.getSelectedRow();
            if (row < 0) return;
            String date = (String) model.getValueAt(row, 0);
            String cat  = (String) model.getValueAt(row, 1);
            int r = JOptionPane.showConfirmDialog(this,
                "Delete this expense?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (r != JOptionPane.YES_OPTION) return;

            for (MonthData md : appData.getMonths().values()) {
                md.getExpenseTransactions().removeIf(t -> t.getDate().equals(date) &&
                    t.getCategoryName().equals(cat));
            }
            refreshData();
        }

        private void refreshData() {
            allData = new ArrayList<>(collectExpenses());
            applyFilter();
            tabs.setTitleAt(1, "Expenses (" + allData.size() + ")");
        }
    }

    // ---- Shared Helpers ----
    private JTable makeTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                c.setBackground(isRowSelected(row) ? Theme.BG_SELECTED : (row % 2 == 0 ? Theme.BG_PANEL : Theme.BG_CARD));
                c.setForeground(Theme.TEXT_PRIMARY);
                if (c instanceof JLabel) ((JLabel)c).setBorder(BorderFactory.createEmptyBorder(0,6,0,6));
                return c;
            }
        };
        table.setBackground(Theme.BG_PANEL);
        table.setForeground(Theme.TEXT_PRIMARY);
        table.setFont(Theme.FONT_SMALL);
        table.setRowHeight(26);
        table.setGridColor(Theme.BORDER);
        table.setShowGrid(true);
        table.setSelectionBackground(Theme.BG_SELECTED);
        table.setSelectionForeground(Theme.TEXT_PRIMARY);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setBackground(Theme.BG_CARD);
        table.getTableHeader().setForeground(Theme.TEXT_SECONDARY);
        table.getTableHeader().setFont(Theme.FONT_BOLD_SM);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0,0,1,0, Theme.BORDER));
        return table;
    }

    private void sortByColumn(JTable table, int col, boolean asc) {
        table.setAutoCreateRowSorter(true);
        TableRowSorter<?> sorter = (TableRowSorter<?>) table.getRowSorter();
        if (sorter != null) {
            sorter.setSortKeys(List.of(new RowSorter.SortKey(col,
                asc ? SortOrder.ASCENDING : SortOrder.DESCENDING)));
        }
    }

    private JLabel makeFilterLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Theme.TEXT_SECONDARY);
        l.setFont(Theme.FONT_SMALL);
        return l;
    }

    private JTextField makeFilterField(int cols) {
        JTextField tf = new JTextField(cols);
        Theme.styleTextField(tf);
        tf.setPreferredSize(new Dimension(tf.getPreferredSize().width, 26));
        return tf;
    }

    private double parseDouble(JTextField tf, double def) {
        if (tf == null || tf.getText().trim().isEmpty()) return def;
        try { return Double.parseDouble(tf.getText().trim()); } catch (Exception e) { return def; }
    }

    private NumberFormat getAmountFormat() {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf;
    }
}
