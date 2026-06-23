package budgettracker.ui.dialog;

import budgettracker.data.*;
import budgettracker.util.Theme;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class AddExpenseDialog extends JDialog {
    private AppData appData;
    private MonthData monthData;
    private ExpenseTransaction existing;
    private boolean confirmed = false;

    private JSpinner amountSpinner;
    private JComboBox<String> categoryCombo;
    private JTextField descField;
    private JSpinner yearSpin, daySpin;
    private JComboBox<String> monthCombo;

    public AddExpenseDialog(Frame parent, AppData appData, MonthData monthData,
                            String preselectedCategory, ExpenseTransaction existing) {
        super(parent, existing == null ? "Add Expense" : "Edit Expense", true);
        this.appData = appData;
        this.monthData = monthData;
        this.existing = existing;
        buildUI(preselectedCategory);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void buildUI(String preselectedCategory) {
        getContentPane().setBackground(Theme.BG_PANEL);
        setLayout(new BorderLayout(0, 0));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel title = new JLabel(existing == null ? "Add Expense" : "Edit Expense");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        form.add(title, gbc);

        // Amount
        gbc.gridy = 1; gbc.gridwidth = 1;
        form.add(makeLabel("Amount (" + appData.getSettings().getCurrencySymbol() + "):"), gbc);
        gbc.gridx = 1;
        amountSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 10_000_000.0, 1.0));
        amountSpinner.setPreferredSize(new Dimension(150, 30));
        styleSpinner(amountSpinner);
        Theme.addClearOnZeroFocus(amountSpinner);
        form.add(amountSpinner, gbc);

        // Category
        gbc.gridx = 0; gbc.gridy = 2;
        form.add(makeLabel("Category:"), gbc);
        gbc.gridx = 1;
        List<Category> cats = monthData.getCategories();
        String[] catNames = cats.stream().map(Category::getName).toArray(String[]::new);
        categoryCombo = new JComboBox<>(catNames);
        categoryCombo.setPreferredSize(new Dimension(180, 30));
        Theme.styleComboBox(categoryCombo);
        if (preselectedCategory != null) {
            for (int i = 0; i < catNames.length; i++) {
                if (catNames[i].equals(preselectedCategory)) { categoryCombo.setSelectedIndex(i); break; }
            }
        }
        form.add(categoryCombo, gbc);

        // Date
        gbc.gridx = 0; gbc.gridy = 3;
        form.add(makeLabel("Date:"), gbc);
        gbc.gridx = 1;
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        datePanel.setOpaque(false);

        String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        monthCombo = new JComboBox<>(months);
        monthCombo.setSelectedIndex(monthData.getMonth() - 1);
        monthCombo.setPreferredSize(new Dimension(72, 28));
        Theme.styleComboBox(monthCombo);

        LocalDate today = LocalDate.now();
        int initDay = monthData.getYear() == today.getYear() && monthData.getMonth() == today.getMonthValue()
            ? today.getDayOfMonth() : 1;

        daySpin = new JSpinner(new SpinnerNumberModel(initDay, 1, 31, 1));
        daySpin.setPreferredSize(new Dimension(62, 28));
        styleSpinner(daySpin);

        yearSpin = new JSpinner(new SpinnerNumberModel(monthData.getYear(), 1900, today.getYear() + 5, 1));
        yearSpin.setPreferredSize(new Dimension(82, 28));
        styleSpinner(yearSpin);

        datePanel.add(monthCombo);
        datePanel.add(daySpin);
        datePanel.add(yearSpin);
        form.add(datePanel, gbc);

        // Description
        gbc.gridx = 0; gbc.gridy = 4;
        form.add(makeLabel("Description (opt):"), gbc);
        gbc.gridx = 1;
        descField = new JTextField(18);
        Theme.styleTextField(descField);
        form.add(descField, gbc);

        // Populate if editing
        if (existing != null) {
            ((SpinnerNumberModel) amountSpinner.getModel()).setValue(existing.getAmount());
            String[] parts = existing.getDate().split("-");
            yearSpin.setValue(Integer.parseInt(parts[0]));
            monthCombo.setSelectedIndex(Integer.parseInt(parts[1]) - 1);
            daySpin.setValue(Integer.parseInt(parts[2]));
            descField.setText(existing.getDescription() != null ? existing.getDescription() : "");
            for (int i = 0; i < catNames.length; i++) {
                if (catNames[i].equals(existing.getCategoryName())) { categoryCombo.setSelectedIndex(i); break; }
            }
        }

        add(form, BorderLayout.CENTER);

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btns.setOpaque(false);
        JButton cancel = Theme.makeSecondaryButton("Cancel");
        JButton save = Theme.makeButton(existing == null ? "Add" : "Save", Theme.ACCENT_ORANGE);
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> onConfirm());
        btns.add(cancel);
        btns.add(save);
        add(btns, BorderLayout.SOUTH);
    }

    private void onConfirm() {
        double amount = (Double) amountSpinner.getValue();
        if (amount <= 0) {
            JOptionPane.showMessageDialog(this, "Amount must be greater than 0.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String category = (String) categoryCombo.getSelectedItem();
        if (category == null) { JOptionPane.showMessageDialog(this, "Select a category."); return; }

        int y = (int) yearSpin.getValue();
        int m = monthCombo.getSelectedIndex() + 1;
        int d = (int) daySpin.getValue();
        d = Math.min(d, java.time.YearMonth.of(y, m).lengthOfMonth());
        String date = String.format("%04d-%02d-%02d", y, m, d);
        String desc = descField.getText().trim();

        if (existing != null) {
            existing.setAmount(amount);
            existing.setCategoryName(category);
            existing.setDate(date);
            existing.setDescription(desc.isEmpty() ? null : desc);
        } else {
            monthData.getExpenseTransactions().add(
                new ExpenseTransaction(UUID.randomUUID().toString(), amount, category, date, desc.isEmpty() ? null : desc));
        }

        confirmed = true;
        dispose();
    }

    private JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(Theme.TEXT_SECONDARY);
        lbl.setFont(Theme.FONT_MEDIUM);
        return lbl;
    }

    private void styleSpinner(JSpinner spinner) {
        JComponent ed = spinner.getEditor();
        if (ed instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) ed).getTextField();
            tf.setBackground(Theme.BG_INPUT);
            tf.setForeground(Theme.TEXT_PRIMARY);
            tf.setCaretColor(Theme.TEXT_PRIMARY);
            tf.setFont(Theme.FONT_MEDIUM);
        }
    }

    public boolean wasConfirmed() { return confirmed; }
}
