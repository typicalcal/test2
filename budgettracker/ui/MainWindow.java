package budgettracker.ui;

import budgettracker.data.*;
import budgettracker.persistence.DataManager;
import budgettracker.ui.dialog.*;
import budgettracker.util.Theme;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class MainWindow extends JFrame {
    private AppData appData;
    private int currentYear;
    private int currentMonth;

    private NavigationPanel navPanel;
    private DonutChartPanel donutPanel;
    private CategoryDashboardPanel dashPanel;
    private SummaryPanel summaryPanel;

    private String activeCategory = null;

    public MainWindow(AppData data) {
        super("Budget Tracker");
        this.appData = data;
        LocalDate now = LocalDate.now();
        this.currentYear = now.getYear();
        this.currentMonth = now.getMonthValue();

        // Ensure current month exists
        appData.getOrCreateMonth(currentYear, currentMonth);
        DataManager.save(appData);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 640));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG_DARK);

        buildUI();
        refreshAll();
    }

    private void buildUI() {
        setLayout(new BorderLayout(0, 0));

        // Navigation bar
        navPanel = new NavigationPanel(currentYear, currentMonth, (year, month) -> {
            currentYear = year;
            currentMonth = month;
            // Ensure month data exists when navigating to a new month
            if (!navPanel.isYearlyMode()) {
                appData.getOrCreateMonth(currentYear, currentMonth);
                DataManager.save(appData);
            }
            refreshAll();
        });
        add(navPanel, BorderLayout.NORTH);

        // Central content area
        JPanel contentArea = new JPanel(new BorderLayout(0, 0));
        contentArea.setBackground(Theme.BG_DARK);

        // Donut chart (left)
        donutPanel = new DonutChartPanel(appData, currentYear, currentMonth, false,
            this::onCategorySelected);
        JPanel leftWrapper = new JPanel(new GridBagLayout());
        leftWrapper.setBackground(Theme.BG_PANEL);
        leftWrapper.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 0, Theme.BORDER));
        leftWrapper.add(donutPanel);
        contentArea.add(leftWrapper, BorderLayout.CENTER);

        // Category dashboard (right)
        dashPanel = new CategoryDashboardPanel(appData, currentYear, currentMonth, false,
            this::onCategorySelected, this::onAddCategory);
        dashPanel.setPreferredSize(new Dimension(310, 0));
        contentArea.add(dashPanel, BorderLayout.EAST);

        add(contentArea, BorderLayout.CENTER);

        // Summary + actions (bottom)
        summaryPanel = new SummaryPanel(appData, currentYear, currentMonth, false,
            this::onAddIncome, this::onAddExpense, this::onHistory, this::onSettings);
        add(summaryPanel, BorderLayout.SOUTH);
    }

    private void refreshAll() {
        boolean yearly = navPanel.isYearlyMode();
        currentYear = navPanel.getCurrentYear();
        currentMonth = navPanel.getCurrentMonth();

        donutPanel.update(appData, currentYear, currentMonth, yearly);
        dashPanel.refresh(appData, currentYear, currentMonth, yearly);
        summaryPanel.refresh(appData, currentYear, currentMonth, yearly);

        if (activeCategory != null) {
            donutPanel.setSelectedCategory(activeCategory);
            dashPanel.setSelectedCategory(activeCategory);
        }
    }

    private void onCategorySelected(String name) {
        activeCategory = name;
        donutPanel.setSelectedCategory(name);
        dashPanel.setSelectedCategory(name);
        showCategoryDialog(name);
    }

    private void onAddCategory() {
        boolean yearly = navPanel.isYearlyMode();
        if (yearly) {
            JOptionPane.showMessageDialog(this,
                "Switch to Monthly view to add categories to a specific month.",
                "Monthly View Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        showAddCategoryDialog();
    }

    private void showAddCategoryDialog() {
        MonthData md = appData.getOrCreateMonth(currentYear, currentMonth);
        AddCategoryDialog dlg = new AddCategoryDialog(this, appData, md);
        dlg.setVisible(true);
        if (dlg.wasConfirmed()) {
            DataManager.save(appData);
            refreshAll();
        }
    }

    private void showCategoryDialog(String categoryName) {
        boolean yearly = navPanel.isYearlyMode();
        MonthData md = yearly ? null : appData.getOrCreateMonth(currentYear, currentMonth);
        CategoryManagementDialog dlg = new CategoryManagementDialog(this, appData, md,
            categoryName, yearly, this::onAfterCategoryChange);
        dlg.setVisible(true);
    }

    private void onAfterCategoryChange() {
        activeCategory = null;
        donutPanel.setSelectedCategory(null);
        dashPanel.setSelectedCategory(null);
        DataManager.save(appData);
        refreshAll();
    }

    private void onAddIncome() {
        boolean yearly = navPanel.isYearlyMode();
        AddIncomeDialog dlg = new AddIncomeDialog(this, appData,
            yearly ? currentYear : currentYear,
            yearly ? -1 : currentMonth, null);
        dlg.setVisible(true);
        if (dlg.wasConfirmed()) {
            DataManager.save(appData);
            refreshAll();
        }
    }

    private void onAddExpense() {
        boolean yearly = navPanel.isYearlyMode();
        if (yearly) {
            JOptionPane.showMessageDialog(this,
                "Switch to Monthly view to add expenses.",
                "Monthly View Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        MonthData md = appData.getOrCreateMonth(currentYear, currentMonth);
        AddExpenseDialog dlg = new AddExpenseDialog(this, appData, md, null, null);
        dlg.setVisible(true);
        if (dlg.wasConfirmed()) {
            DataManager.save(appData);
            refreshAll();
        }
    }

    private void onHistory() {
        boolean yearly = navPanel.isYearlyMode();
        MonthData md = yearly ? null : appData.getOrCreateMonth(currentYear, currentMonth);
        TransactionHistoryDialog dlg = new TransactionHistoryDialog(
            this, appData, md, currentYear, currentMonth, yearly);
        dlg.setVisible(true);
        DataManager.save(appData);
        refreshAll();
    }

    private void onSettings() {
        SettingsDialog dlg = new SettingsDialog(this, appData);
        dlg.setVisible(true);
        DataManager.save(appData);
        // Check if data was replaced by snapshot restore
        AppData newData = dlg.getResultData();
        if (newData != null) {
            this.appData = newData;
            DataManager.save(appData);
        }
        refreshAll();
    }

    public AppData getAppData() { return appData; }
    public void setAppData(AppData data) { this.appData = data; }
}
