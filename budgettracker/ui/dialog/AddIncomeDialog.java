package budgettracker.ui.dialog;

import budgettracker.data.*;
import budgettracker.util.Theme;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.UUID;

public class AddIncomeDialog extends JDialog {
    private AppData appData;
    private IncomeTransaction existing; // non-null when editing
    private boolean confirmed = false;

    private JSpinner amountSpinner;
    private JTextField sourceField;
    private JSpinner yearSpin, daySpin;
    private JComboBox<String> monthCombo;

    // pre-filled year/month
    private int prefYear;
    private int prefMonth; // -1 = no preference

    public AddIncomeDialog(Frame parent, AppData appData, int prefYear, int prefMonth,
                           IncomeTransaction existing) {
        super(parent, existing == null ? "Add Income" : "Edit Income", true);
        this.appData = appData;
        this.existing = existing;
        this.prefYear = prefYear;
        this.prefMonth = prefMonth;
        buildUI();
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        getContentPane().setBackground(Theme.BG_PANEL);
        setLayout(new BorderLayout(0, 0));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel title = new JLabel(existing == null ? "Add Income" : "Edit Income");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        form.add(title, gbc);

        // Amount
        gbc.gridy = 1; gbc.gridwidth = 1;
        form.add(makeLabel("Amount (" + appData.getSettings().getCurrencySymbol() + "):"), gbc);
        gbc.gridx = 1;
        amountSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 10_000_000.0, 10.0));
        amountSpinner.setPreferredSize(new Dimension(150, 30));
        styleSpinner(amountSpinner);
        Theme.addClearOnZeroFocus(amountSpinner);
        form.add(amountSpinner, gbc);

        // Source
        gbc.gridx = 0; gbc.gridy = 2;
        form.add(makeLabel("Source:"), gbc);
        gbc.gridx = 1;
        sourceField = new JTextField(18);
        Theme.styleTextField(sourceField);
        form.add(sourceField, gbc);

        // Date
        gbc.gridx = 0; gbc.gridy = 3;
        form.add(makeLabel("Date:"), gbc);
        gbc.gridx = 1;
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        datePanel.setOpaque(false);

        LocalDate today = LocalDate.now();
        int initYear = prefYear > 0 ? prefYear : today.getYear();
        int initMonth = prefMonth > 0 ? prefMonth : today.getMonthValue();
        int initDay = today.getDayOfMonth();

        String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        monthCombo = new JComboBox<>(months);
        monthCombo.setSelectedIndex(initMonth - 1);
        monthCombo.setPreferredSize(new Dimension(72, 28));
        Theme.styleComboBox(monthCombo);

        daySpin = new JSpinner(new SpinnerNumberModel(initDay, 1, 31, 1));
        daySpin.setPreferredSize(new Dimension(62, 28));
        styleSpinner(daySpin);

        yearSpin = new JSpinner(new SpinnerNumberModel(initYear, 1900, today.getYear() + 5, 1));
        yearSpin.setPreferredSize(new Dimension(82, 28));
        styleSpinner(yearSpin);

        datePanel.add(monthCombo);
        datePanel.add(daySpin);
        datePanel.add(yearSpin);
        form.add(datePanel, gbc);

        // Populate if editing
        if (existing != null) {
            ((SpinnerNumberModel) amountSpinner.getModel()).setValue(existing.getAmount());
            sourceField.setText(existing.getSource());
            String[] parts = existing.getDate().split("-");
            yearSpin.setValue(Integer.parseInt(parts[0]));
            monthCombo.setSelectedIndex(Integer.parseInt(parts[1]) - 1);
            daySpin.setValue(Integer.parseInt(parts[2]));
        }

        add(form, BorderLayout.CENTER);

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btns.setOpaque(false);
        JButton cancel = Theme.makeSecondaryButton("Cancel");
        JButton save = Theme.makeButton(existing == null ? "Add" : "Save", Theme.ACCENT_GREEN);
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
        String source = sourceField.getText().trim();
        if (source.isEmpty()) source = "Income";

        int y = (int) yearSpin.getValue();
        int m = monthCombo.getSelectedIndex() + 1;
        int d = (int) daySpin.getValue();
        // Clamp day
        d = Math.min(d, java.time.YearMonth.of(y, m).lengthOfMonth());
        String date = String.format("%04d-%02d-%02d", y, m, d);

        // Remove from old month if editing
        if (existing != null) {
            for (MonthData md : appData.getMonths().values()) {
                md.getIncomeTransactions().removeIf(t -> t.getId().equals(existing.getId()));
            }
        }

        // Add to appropriate month
        MonthData md = appData.getOrCreateMonth(y, m);
        String id = existing != null ? existing.getId() : UUID.randomUUID().toString();
        md.getIncomeTransactions().add(new IncomeTransaction(id, amount, source, date));

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
