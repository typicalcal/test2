package budgettracker.ui;

import budgettracker.data.*;
import budgettracker.util.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class CategoryDashboardPanel extends JPanel {
    private AppData appData;
    private int year, month;
    private boolean yearlyMode;
    private String selectedCategory = null;

    private JPanel listPanel;
    private JScrollPane scrollPane;
    private Consumer<String> onCategoryClicked;
    private Runnable onAddCategory;

    public CategoryDashboardPanel(AppData data, int year, int month, boolean yearlyMode,
                                  Consumer<String> onCategoryClicked, Runnable onAddCategory) {
        this.appData = data;
        this.year = year;
        this.month = month;
        this.yearlyMode = yearlyMode;
        this.onCategoryClicked = onCategoryClicked;
        this.onAddCategory = onAddCategory;

        setBackground(Theme.BG_PANEL);
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Theme.BORDER));
        setLayout(new BorderLayout(0, 0));

        buildUI();
        refresh(data, year, month, yearlyMode);
    }

    private void buildUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        JLabel title = new JLabel("Categories");
        title.setFont(Theme.FONT_LARGE);
        title.setForeground(Theme.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        JButton addBtn = Theme.makeButton("+ Add", Theme.ACCENT_BLUE);
        addBtn.addActionListener(e -> { if (onAddCategory != null) onAddCategory.run(); });
        header.add(addBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Scrollable list
        listPanel = new JPanel();
        listPanel.setBackground(Theme.BG_PANEL);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Theme.BG_PANEL);
        Theme.styleScrollPane(scrollPane);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void refresh(AppData data, int year, int month, boolean yearlyMode) {
        this.appData = data;
        this.year = year;
        this.month = month;
        this.yearlyMode = yearlyMode;
        listPanel.removeAll();

        if (yearlyMode) {
            Map<String, double[]> agg = data.getYearlyAggregation(year);
            for (Map.Entry<String, double[]> e : agg.entrySet()) {
                listPanel.add(makeCategoryRow(e.getKey(), e.getValue()[0], e.getValue()[1]));
                listPanel.add(Box.createVerticalStrut(1));
            }
        } else {
            MonthData md = data.getOrCreateMonth(year, month);
            for (Category cat : md.getCategories()) {
                double spent = md.getSpentForCategory(cat.getName());
                listPanel.add(makeCategoryRow(cat.getName(), cat.getAllocatedBudget(), spent));
                listPanel.add(Box.createVerticalStrut(1));
            }
        }

        // Filler
        listPanel.add(Box.createVerticalGlue());
        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel makeCategoryRow(String name, double allocated, double spent) {
        Color catColor = Theme.hexToColor(appData.getCategoryColor(name));
        boolean overBudget = spent > allocated && allocated > 0;
        boolean isSelected = name.equals(selectedCategory);

        JPanel row = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                boolean hovered = getClientProperty("hovered") == Boolean.TRUE;
                Color bg = isSelected ? Theme.BG_SELECTED : hovered ? Theme.BG_HOVER : Theme.BG_PANEL;
                g2.setColor(bg);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Left color bar
                g2.setColor(catColor);
                g2.fillRect(0, 0, 4, getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        row.setOpaque(false);
        row.setLayout(new BorderLayout(8, 4));
        row.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Top: name + amounts
        JPanel topRow = new JPanel(new BorderLayout(4, 0));
        topRow.setOpaque(false);

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(Theme.FONT_BOLD);
        nameLabel.setForeground(overBudget ? Theme.TEXT_RED : Theme.TEXT_PRIMARY);
        topRow.add(nameLabel, BorderLayout.WEST);

        String sym = appData.getSettings().getCurrencySymbol();
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(0);

        String amtText = sym + nf.format(spent) + " / " + sym + nf.format(allocated);
        JLabel amtLabel = new JLabel(amtText);
        amtLabel.setFont(Theme.FONT_SMALL);
        amtLabel.setForeground(overBudget ? Theme.TEXT_RED : Theme.TEXT_SECONDARY);
        topRow.add(amtLabel, BorderLayout.EAST);

        row.add(topRow, BorderLayout.NORTH);

        // Progress bar
        double progress = allocated > 0 ? Math.min(spent / allocated, 1.2) : 0;
        JPanel progressBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // Track
                g2.setColor(Theme.BG_CARD);
                g2.fillRoundRect(0, 0, w, h, h, h);
                // Fill
                int fillW = (int)(w * Math.min(progress, 1.0));
                if (fillW > 0) {
                    Color fillColor = overBudget ? Theme.ACCENT_RED : catColor;
                    g2.setColor(fillColor);
                    g2.fillRoundRect(0, 0, fillW, h, h, h);
                }
                g2.dispose();
            }
        };
        progressBar.setOpaque(false);
        progressBar.setPreferredSize(new Dimension(0, 6));

        row.add(progressBar, BorderLayout.SOUTH);

        // Hover
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                row.putClientProperty("hovered", Boolean.TRUE);
                row.repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                row.putClientProperty("hovered", Boolean.FALSE);
                row.repaint();
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedCategory = name;
                if (onCategoryClicked != null) onCategoryClicked.accept(name);
                listPanel.repaint();
            }
        });

        return row;
    }

    public void setSelectedCategory(String name) {
        this.selectedCategory = name;
        listPanel.repaint();
    }
}
