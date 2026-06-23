package budgettracker.ui.dialog;

import budgettracker.data.*;
import budgettracker.util.Theme;

import javax.swing.*;
import java.awt.*;

public class CategoryManagementDialog extends JDialog {
    private AppData appData;
    private MonthData monthData; // null in yearly mode
    private String categoryName;
    private boolean yearlyMode;
    private Runnable onChanged;

    public CategoryManagementDialog(Frame parent, AppData appData, MonthData monthData,
                                    String categoryName, boolean yearlyMode, Runnable onChanged) {
        super(parent, "Category: " + categoryName, true);
        this.appData = appData;
        this.monthData = monthData;
        this.categoryName = categoryName;
        this.yearlyMode = yearlyMode;
        this.onChanged = onChanged;
        buildUI();
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        getContentPane().setBackground(Theme.BG_PANEL);
        setLayout(new BorderLayout(0, 0));
        setMinimumSize(new Dimension(320, 0));

        Category cat = monthData != null ? monthData.getCategoryByName(categoryName) : null;
        boolean isProtected = cat != null && cat.isProtected();
        boolean inTemplate = appData.isInTemplate(categoryName);

        // Header with color swatch
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));
        Color catColor = Theme.hexToColor(appData.getCategoryColor(categoryName));
        JPanel swatch = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(catColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
            }
        };
        swatch.setPreferredSize(new Dimension(14, 14));
        swatch.setOpaque(false);

        JLabel title = new JLabel(categoryName);
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);
        titleRow.add(swatch);
        titleRow.add(title);
        header.add(titleRow, BorderLayout.WEST);

        if (isProtected) {
            JLabel prot = new JLabel("Protected");
            prot.setFont(Theme.FONT_SMALL);
            prot.setForeground(Theme.TEXT_MUTED);
            header.add(prot, BorderLayout.EAST);
        }
        add(header, BorderLayout.NORTH);

        // Buttons grid
        JPanel grid = new JPanel(new GridLayout(0, 2, 8, 8));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        if (!yearlyMode && monthData != null) {
            JButton addExpBtn = Theme.makeButton("Add Expense", Theme.ACCENT_ORANGE);
            addExpBtn.addActionListener(e -> doAddExpense());
            grid.add(addExpBtn);

            JButton viewTxBtn = Theme.makeButton("View Transactions", Theme.ACCENT_BLUE);
            viewTxBtn.addActionListener(e -> doViewTransactions());
            grid.add(viewTxBtn);

            JButton editBudgetBtn = Theme.makeButton("Edit Budget", Theme.ACCENT_BLUE);
            editBudgetBtn.addActionListener(e -> doEditBudget());
            grid.add(editBudgetBtn);
        }

        JButton colorBtn = Theme.makeButton("Change Color", Theme.ACCENT_PURPLE);
        colorBtn.addActionListener(e -> doChangeColor());
        grid.add(colorBtn);

        if (!isProtected) {
            JButton renameBtn = Theme.makeButton("Rename", Theme.BG_BUTTON);
            renameBtn.addActionListener(e -> doRename());
            grid.add(renameBtn);

            JButton deleteBtn = Theme.makeButton("Remove Category", Theme.ACCENT_RED);
            deleteBtn.addActionListener(e -> doDelete());
            grid.add(deleteBtn);
        }

        // Template toggle
        if (!isProtected && !yearlyMode && monthData != null) {
            JButton templateBtn;
            Category localCat = monthData.getCategoryByName(categoryName);
            boolean isTemplateHere = localCat != null && localCat.isTemplateCategory();

            if (inTemplate) {
                templateBtn = Theme.makeButton("Remove from Template", Theme.BG_BUTTON);
                templateBtn.addActionListener(e -> doRemoveFromTemplate());
            } else {
                templateBtn = Theme.makeButton("Add to Template", Theme.ACCENT_GREEN);
                templateBtn.addActionListener(e -> doAddToTemplate());
            }
            grid.add(templateBtn);
        }

        add(grid, BorderLayout.CENTER);

        // Close button
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        footer.setOpaque(false);
        JButton closeBtn = Theme.makeSecondaryButton("Close");
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);
        add(footer, BorderLayout.SOUTH);
    }

    private void doAddExpense() {
        dispose();
        AddExpenseDialog dlg = new AddExpenseDialog(
            (Frame) getOwner(), appData, monthData, categoryName, null);
        dlg.setVisible(true);
        if (dlg.wasConfirmed() && onChanged != null) onChanged.run();
    }

    private void doViewTransactions() {
        dispose();
        TransactionHistoryDialog dlg = new TransactionHistoryDialog(
            (Frame) getOwner(), appData, monthData,
            monthData.getYear(), monthData.getMonth(), false);
        dlg.setFilterCategory(categoryName);
        dlg.setVisible(true);
        if (onChanged != null) onChanged.run();
    }

    private void doEditBudget() {
        Category cat = monthData.getCategoryByName(categoryName);
        if (cat == null) return;

        JSpinner spin = new JSpinner(new SpinnerNumberModel(cat.getAllocatedBudget(), 0.0, 1_000_000.0, 10.0));
        spin.setPreferredSize(new Dimension(140, 30));
        Theme.addClearOnZeroFocus(spin);

        String sym = appData.getSettings().getCurrencySymbol();
        int result = JOptionPane.showConfirmDialog(this,
            new Object[]{"New budget allocation (" + sym + "):", spin},
            "Edit Budget", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            cat.setAllocatedBudget((Double) spin.getValue());
            if (onChanged != null) onChanged.run();
            dispose();
        }
    }

    private void doChangeColor() {
        Color current = Theme.hexToColor(appData.getCategoryColor(categoryName));
        Color chosen = JColorChooser.showDialog(this, "Choose Color for " + categoryName, current);
        if (chosen != null) {
            // Update in all existing months
            appData.setCategoryColor(categoryName, Theme.colorToHex(chosen));
            if (onChanged != null) onChanged.run();
            dispose();
        }
    }

    private void doRename() {
        JTextField tf = new JTextField(categoryName, 18);
        int result = JOptionPane.showConfirmDialog(this,
            new Object[]{"New name:", tf},
            "Rename Category", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String newName = tf.getText().trim();
        if (newName.isEmpty() || newName.equals(categoryName)) return;

        // Check if name already exists
        if (monthData != null && monthData.getCategoryByName(newName) != null) {
            JOptionPane.showMessageDialog(this, "A category with that name already exists.");
            return;
        }

        // Rename in all months
        for (MonthData md : appData.getMonths().values()) {
            Category c = md.getCategoryByName(categoryName);
            if (c != null) c.setName(newName);
            // Update expense transactions
            for (ExpenseTransaction tx : md.getExpenseTransactions()) {
                if (tx.getCategoryName().equals(categoryName)) tx.setCategoryName(newName);
            }
        }

        // Rename in template
        CategoryTemplate tmpl = appData.getTemplate(categoryName);
        if (tmpl != null) tmpl.setName(newName);

        // Rename color map
        appData.renameCategoryColor(categoryName, newName);

        categoryName = newName;
        if (onChanged != null) onChanged.run();
        dispose();
    }

    private void doDelete() {
        // Check if protected
        Category cat = monthData != null ? monthData.getCategoryByName(categoryName) : null;
        if (cat != null && cat.isProtected()) {
            JOptionPane.showMessageDialog(this, "This category is protected and cannot be removed.");
            return;
        }

        // Count expenses
        long expCount = monthData != null ?
            monthData.getExpenseTransactions().stream()
                .filter(e -> e.getCategoryName().equals(categoryName)).count() : 0;

        String msg = expCount > 0
            ? "Delete \"" + categoryName + "\"?\n" + expCount +
              " expense(s) will be moved to Miscellaneous."
            : "Delete \"" + categoryName + "\"? This cannot be undone.";

        int r = JOptionPane.showConfirmDialog(this, msg, "Confirm Delete",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;

        // Move expenses to Miscellaneous
        if (monthData != null) {
            for (ExpenseTransaction tx : monthData.getExpenseTransactions()) {
                if (tx.getCategoryName().equals(categoryName)) tx.setCategoryName("Miscellaneous");
            }
        }

        // Remove from this month
        if (monthData != null) {
            monthData.getCategories().removeIf(c -> c.getName().equals(categoryName));
        }

        if (onChanged != null) onChanged.run();
        dispose();
    }

    private void doAddToTemplate() {
        JSpinner spin = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1_000_000.0, 10.0));
        Theme.addClearOnZeroFocus(spin);
        Category cat = monthData != null ? monthData.getCategoryByName(categoryName) : null;
        if (cat != null) ((SpinnerNumberModel) spin.getModel()).setValue(cat.getAllocatedBudget());

        String sym = appData.getSettings().getCurrencySymbol();
        int result = JOptionPane.showConfirmDialog(this,
            new Object[]{"Default budget for future months (" + sym + "):", spin},
            "Add to Template", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        double defBudget = (Double) spin.getValue();
        appData.getCategoryTemplates().add(new CategoryTemplate(categoryName, defBudget, false));

        // Mark category in current month as template category
        if (cat != null) cat.setTemplateCategory(true);

        if (onChanged != null) onChanged.run();
        dispose();
    }

    private void doRemoveFromTemplate() {
        int r = JOptionPane.showConfirmDialog(this,
            "Remove \"" + categoryName + "\" from template?\nExisting months will not be affected.",
            "Remove from Template", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;

        appData.getCategoryTemplates().removeIf(t -> t.getName().equals(categoryName));

        // Mark as non-template in current month
        if (monthData != null) {
            Category cat = monthData.getCategoryByName(categoryName);
            if (cat != null) cat.setTemplateCategory(false);
        }

        if (onChanged != null) onChanged.run();
        dispose();
    }
}
