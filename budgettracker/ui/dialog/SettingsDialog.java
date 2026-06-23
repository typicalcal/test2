package budgettracker.ui.dialog;

import budgettracker.data.*;
import budgettracker.persistence.DataManager;
import budgettracker.persistence.SnapshotManager;
import budgettracker.util.Theme;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;

public class SettingsDialog extends JDialog {
    private AppData appData;
    private AppData resultData = null; // set if snapshot was restored

    private JComboBox<String> currencyCombo;
    private DefaultTableModel templateModel;
    private JTable templateTable;

    public SettingsDialog(Frame parent, AppData appData) {
        super(parent, "Settings", true);
        this.appData = appData;
        buildUI();
        setSize(660, 580);
        setResizable(true);
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        getContentPane().setBackground(Theme.BG_PANEL);
        setLayout(new BorderLayout(0, 0));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(Theme.BG_PANEL);
        tabs.setForeground(Theme.TEXT_PRIMARY);
        tabs.setFont(Theme.FONT_MEDIUM);
        tabs.addTab("General", buildGeneralPanel());
        tabs.addTab("Category Template", buildTemplatePanel());
        tabs.addTab("Snapshots", buildSnapshotPanel());
        add(tabs, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createMatteBorder(1,0,0,0,Theme.BORDER));
        JButton close = Theme.makeSecondaryButton("Close");
        close.addActionListener(e -> { applyGeneral(); dispose(); });
        footer.add(close);
        add(footer, BorderLayout.SOUTH);
    }

    private JPanel buildGeneralPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.BG_PANEL);
        p.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 4, 8, 4);
        gbc.anchor = GridBagConstraints.WEST;

        // Currency
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel currLabel = new JLabel("Currency:");
        currLabel.setFont(Theme.FONT_MEDIUM);
        currLabel.setForeground(Theme.TEXT_SECONDARY);
        p.add(currLabel, gbc);

        gbc.gridx = 1;
        AppSettings s = appData.getSettings();
        currencyCombo = new JComboBox<>(s.getAvailableCurrencies().toArray(new String[0]));
        currencyCombo.setSelectedItem(s.getCurrency());
        currencyCombo.setPreferredSize(new Dimension(120, 30));
        Theme.styleComboBox(currencyCombo);
        p.add(currencyCombo, gbc);

        // Reset All Data
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JSeparator sep = new JSeparator();
        sep.setForeground(Theme.BORDER);
        p.add(sep, gbc);

        gbc.gridy = 3; gbc.gridwidth = 1;
        JLabel resetLabel = new JLabel("Reset All Data:");
        resetLabel.setFont(Theme.FONT_MEDIUM);
        resetLabel.setForeground(Theme.TEXT_SECONDARY);
        p.add(resetLabel, gbc);

        gbc.gridx = 1;
        JButton resetBtn = Theme.makeButton("Reset All Data", Theme.ACCENT_RED);
        resetBtn.addActionListener(e -> doResetAllData());
        p.add(resetBtn, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JLabel resetNote = new JLabel("A snapshot will be created before resetting.");
        resetNote.setFont(Theme.FONT_SMALL);
        resetNote.setForeground(Theme.TEXT_MUTED);
        p.add(resetNote, gbc);

        // Filler
        gbc.gridy = 10; gbc.weighty = 1.0;
        p.add(Box.createVerticalGlue(), gbc);

        return p;
    }

    private void applyGeneral() {
        String sel = (String) currencyCombo.getSelectedItem();
        if (sel != null) appData.getSettings().setCurrency(sel);
        DataManager.save(appData);
    }

    private void doResetAllData() {
        // Show warning with typed confirmation
        JTextField confirmField = new JTextField(12);
        Theme.styleTextField(confirmField);
        int r = JOptionPane.showConfirmDialog(this,
            new Object[]{
                "THIS WILL DELETE ALL DATA.\nA snapshot will be saved first.\n\nType DELETE to confirm:",
                confirmField
            },
            "Reset All Data", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        if (!"DELETE".equals(confirmField.getText().trim())) {
            JOptionPane.showMessageDialog(this, "Confirmation phrase incorrect. Reset cancelled.");
            return;
        }

        try {
            SnapshotManager.createSnapshot(appData);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Warning: Could not create backup snapshot.\n" + e.getMessage());
        }

        // Reset to fresh data preserving settings
        AppSettings oldSettings = appData.getSettings();
        AppData fresh = new AppData();
        fresh.setSettings(oldSettings);
        this.appData = fresh;
        DataManager.save(fresh);
        resultData = fresh;

        JOptionPane.showMessageDialog(this, "All data has been reset. A snapshot was saved.", "Done",
            JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    private JPanel buildTemplatePanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Theme.BG_PANEL);
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel note = new JLabel("Template changes only affect future months — existing months are never modified.");
        note.setFont(Theme.FONT_SMALL);
        note.setForeground(Theme.TEXT_MUTED);
        note.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));
        p.add(note, BorderLayout.NORTH);

        // Table
        String[] cols = {"Name", "Default Budget", "Protected"};
        templateModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        refreshTemplateTable();

        templateTable = new JTable(templateModel) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                c.setBackground(isRowSelected(row) ? Theme.BG_SELECTED : (row % 2 == 0 ? Theme.BG_PANEL : Theme.BG_CARD));
                c.setForeground(Theme.TEXT_PRIMARY);
                return c;
            }
        };
        styleTable(templateTable);
        templateTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        templateTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        templateTable.getColumnModel().getColumn(2).setPreferredWidth(80);

        JScrollPane sp = new JScrollPane(templateTable);
        Theme.styleScrollPane(sp);
        p.add(sp, BorderLayout.CENTER);

        // Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actions.setOpaque(false);

        JButton addBtn = Theme.makeButton("+ New Category", Theme.ACCENT_BLUE);
        addBtn.addActionListener(e -> doAddTemplateCategory());
        actions.add(addBtn);

        JButton editBudgetBtn = Theme.makeButton("Edit Budget", Theme.BG_BUTTON);
        editBudgetBtn.addActionListener(e -> doEditTemplateBudget());
        actions.add(editBudgetBtn);

        JButton renameBtn = Theme.makeButton("Rename", Theme.BG_BUTTON);
        renameBtn.addActionListener(e -> doRenameTemplate());
        actions.add(renameBtn);

        JButton colorBtn = Theme.makeButton("Change Color", Theme.ACCENT_PURPLE);
        colorBtn.addActionListener(e -> doChangeTemplateColor());
        actions.add(colorBtn);

        JButton removeBtn = Theme.makeButton("Remove", Theme.ACCENT_RED);
        removeBtn.addActionListener(e -> doRemoveTemplate());
        actions.add(removeBtn);

        p.add(actions, BorderLayout.SOUTH);
        return p;
    }

    private void refreshTemplateTable() {
        templateModel.setRowCount(0);
        String sym = appData.getSettings().getCurrencySymbol();
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(2); nf.setMaximumFractionDigits(2);
        for (CategoryTemplate t : appData.getCategoryTemplates()) {
            templateModel.addRow(new Object[]{
                t.getName(),
                sym + nf.format(t.getDefaultBudget()),
                t.isProtected() ? "Yes" : "No"
            });
        }
    }

    private void doAddTemplateCategory() {
        JTextField nameField = new JTextField(16);
        Theme.styleTextField(nameField);
        JSpinner budgetSpin = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1_000_000.0, 10.0));
        Theme.addClearOnZeroFocus(budgetSpin);

        int r = JOptionPane.showConfirmDialog(this,
            new Object[]{"Category name:", nameField, "Default budget:", budgetSpin},
            "New Template Category", JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        if (name.isEmpty()) return;
        if (appData.isInTemplate(name)) {
            JOptionPane.showMessageDialog(this, "Already exists in template.");
            return;
        }
        double budget = (Double) budgetSpin.getValue();
        appData.getCategoryTemplates().add(new CategoryTemplate(name, budget, false));
        appData.assignNextColor(name);
        DataManager.save(appData);
        refreshTemplateTable();
    }

    private void doEditTemplateBudget() {
        int row = templateTable.getSelectedRow();
        if (row < 0) return;
        CategoryTemplate t = appData.getCategoryTemplates().get(row);
        JSpinner spin = new JSpinner(new SpinnerNumberModel(t.getDefaultBudget(), 0.0, 1_000_000.0, 10.0));
        Theme.addClearOnZeroFocus(spin);
        int r = JOptionPane.showConfirmDialog(this,
            new Object[]{"New default budget:", spin},
            "Edit Budget", JOptionPane.OK_CANCEL_OPTION);
        if (r == JOptionPane.OK_OPTION) {
            t.setDefaultBudget((Double) spin.getValue());
            DataManager.save(appData);
            refreshTemplateTable();
        }
    }

    private void doRenameTemplate() {
        int row = templateTable.getSelectedRow();
        if (row < 0) return;
        CategoryTemplate t = appData.getCategoryTemplates().get(row);
        if (t.isProtected()) { JOptionPane.showMessageDialog(this, "Protected categories cannot be renamed."); return; }
        JTextField tf = new JTextField(t.getName(), 16);
        int r = JOptionPane.showConfirmDialog(this, new Object[]{"New name:", tf}, "Rename", JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;
        String newName = tf.getText().trim();
        if (newName.isEmpty() || newName.equals(t.getName())) return;
        // Rename everywhere
        String oldName = t.getName();
        t.setName(newName);
        for (MonthData md : appData.getMonths().values()) {
            Category c = md.getCategoryByName(oldName);
            if (c != null) c.setName(newName);
            for (ExpenseTransaction tx : md.getExpenseTransactions()) {
                if (tx.getCategoryName().equals(oldName)) tx.setCategoryName(newName);
            }
        }
        appData.renameCategoryColor(oldName, newName);
        DataManager.save(appData);
        refreshTemplateTable();
    }

    private void doChangeTemplateColor() {
        int row = templateTable.getSelectedRow();
        if (row < 0) return;
        CategoryTemplate t = appData.getCategoryTemplates().get(row);
        Color cur = Theme.hexToColor(appData.getCategoryColor(t.getName()));
        Color chosen = JColorChooser.showDialog(this, "Color for " + t.getName(), cur);
        if (chosen != null) {
            appData.setCategoryColor(t.getName(), Theme.colorToHex(chosen));
            DataManager.save(appData);
        }
    }

    private void doRemoveTemplate() {
        int row = templateTable.getSelectedRow();
        if (row < 0) return;
        CategoryTemplate t = appData.getCategoryTemplates().get(row);
        if (t.isProtected()) { JOptionPane.showMessageDialog(this, "Protected categories cannot be removed."); return; }
        int r = JOptionPane.showConfirmDialog(this,
            "Remove \"" + t.getName() + "\" from template?\nExisting months are not affected.",
            "Confirm Remove", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) return;
        appData.getCategoryTemplates().remove(row);
        DataManager.save(appData);
        refreshTemplateTable();
    }

    private JPanel buildSnapshotPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Theme.BG_PANEL);
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel note = new JLabel("Snapshots are created automatically before data resets.");
        note.setFont(Theme.FONT_SMALL);
        note.setForeground(Theme.TEXT_MUTED);
        note.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));
        p.add(note, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        btnPanel.setOpaque(false);

        JButton manageBtn = Theme.makeButton("Open Snapshot Manager", Theme.ACCENT_BLUE);
        manageBtn.addActionListener(e -> {
            applyGeneral();
            SnapshotManagerDialog dlg = new SnapshotManagerDialog((Frame) getOwner(), appData);
            dlg.setVisible(true);
            AppData restored = dlg.getRestoredData();
            if (restored != null) {
                resultData = restored;
                dispose();
            }
        });
        btnPanel.add(manageBtn);

        JButton createBtn = Theme.makeButton("Create Snapshot Now", Theme.ACCENT_GREEN);
        createBtn.addActionListener(e -> {
            try {
                SnapshotManager.createSnapshot(appData);
                JOptionPane.showMessageDialog(this, "Snapshot created.", "Done", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed: " + ex.getMessage());
            }
        });
        btnPanel.add(createBtn);
        p.add(btnPanel, BorderLayout.CENTER);
        return p;
    }

    private void styleTable(JTable table) {
        table.setBackground(Theme.BG_PANEL);
        table.setForeground(Theme.TEXT_PRIMARY);
        table.setFont(Theme.FONT_MEDIUM);
        table.setRowHeight(28);
        table.setGridColor(Theme.BORDER);
        table.setSelectionBackground(Theme.BG_SELECTED);
        table.setSelectionForeground(Theme.TEXT_PRIMARY);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setBackground(Theme.BG_CARD);
        table.getTableHeader().setForeground(Theme.TEXT_SECONDARY);
        table.getTableHeader().setFont(Theme.FONT_BOLD_SM);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0,0,1,0, Theme.BORDER));
    }

    public AppData getResultData() { return resultData; }
}
