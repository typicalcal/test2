package budgettracker.ui.dialog;

import budgettracker.data.*;
import budgettracker.util.Theme;

import javax.swing.*;
import java.awt.*;

public class AddCategoryDialog extends JDialog {
    private AppData appData;
    private MonthData monthData;
    private boolean confirmed = false;

    private JTextField nameField;
    private JSpinner budgetSpinner;
    private JRadioButton monthOnlyRb, templateRb;

    public AddCategoryDialog(Frame parent, AppData appData, MonthData monthData) {
        super(parent, "Add Category", true);
        this.appData = appData;
        this.monthData = monthData;
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
        JLabel title = new JLabel("New Category");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        form.add(title, gbc);

        // Name
        gbc.gridy = 1; gbc.gridwidth = 1;
        form.add(makeLabel("Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(18);
        Theme.styleTextField(nameField);
        form.add(nameField, gbc);

        // Budget
        gbc.gridx = 0; gbc.gridy = 2;
        form.add(makeLabel("Monthly Budget:"), gbc);
        gbc.gridx = 1;
        budgetSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1_000_000.0, 10.0));
        budgetSpinner.setPreferredSize(new Dimension(140, 30));
        styleSpinner(budgetSpinner);
        Theme.addClearOnZeroFocus(budgetSpinner);
        form.add(budgetSpinner, gbc);

        // Scope
        gbc.gridx = 0; gbc.gridy = 3;
        form.add(makeLabel("Scope:"), gbc);
        gbc.gridx = 1;
        JPanel scopePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        scopePanel.setOpaque(false);
        monthOnlyRb = makeRadio("This month only");
        templateRb  = makeRadio("Add to global template (future months)");
        ButtonGroup bg = new ButtonGroup();
        bg.add(monthOnlyRb);
        bg.add(templateRb);
        templateRb.setSelected(true);
        scopePanel.add(templateRb);
        scopePanel.add(monthOnlyRb);
        form.add(scopePanel, gbc);

        add(form, BorderLayout.CENTER);

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btns.setOpaque(false);
        JButton cancel = Theme.makeSecondaryButton("Cancel");
        JButton create = Theme.makeButton("Create", Theme.ACCENT_BLUE);
        cancel.addActionListener(e -> dispose());
        create.addActionListener(e -> onConfirm());
        btns.add(cancel);
        btns.add(create);
        add(btns, BorderLayout.SOUTH);
    }

    private void onConfirm() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (monthData.getCategoryByName(name) != null) {
            JOptionPane.showMessageDialog(this, "Category already exists.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        double budget = (Double) budgetSpinner.getValue();
        boolean addToTemplate = templateRb.isSelected();

        // Add to current month
        Category cat = new Category(name, budget, false, addToTemplate);
        monthData.getCategories().add(cat);

        // Assign color
        appData.assignNextColor(name);

        if (addToTemplate) {
            // Add to global template if not already present
            if (!appData.isInTemplate(name)) {
                appData.getCategoryTemplates().add(new CategoryTemplate(name, budget, false));
            }
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

    private JRadioButton makeRadio(String text) {
        JRadioButton rb = new JRadioButton(text);
        rb.setOpaque(false);
        rb.setForeground(Theme.TEXT_SECONDARY);
        rb.setFont(Theme.FONT_SMALL);
        return rb;
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setBackground(Theme.BG_INPUT);
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
