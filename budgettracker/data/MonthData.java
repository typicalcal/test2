package budgettracker.data;

import java.util.ArrayList;
import java.util.List;

public class MonthData {
    private int year;
    private int month;
    private List<Category> categories;
    private List<IncomeTransaction> incomeTransactions;
    private List<ExpenseTransaction> expenseTransactions;

    public MonthData() {
        this.categories = new ArrayList<>();
        this.incomeTransactions = new ArrayList<>();
        this.expenseTransactions = new ArrayList<>();
    }

    public MonthData(int year, int month) {
        this();
        this.year = year;
        this.month = month;
    }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public List<Category> getCategories() { return categories; }
    public void setCategories(List<Category> categories) { this.categories = categories; }
    public List<IncomeTransaction> getIncomeTransactions() { return incomeTransactions; }
    public void setIncomeTransactions(List<IncomeTransaction> incomeTransactions) { this.incomeTransactions = incomeTransactions; }
    public List<ExpenseTransaction> getExpenseTransactions() { return expenseTransactions; }
    public void setExpenseTransactions(List<ExpenseTransaction> expenseTransactions) { this.expenseTransactions = expenseTransactions; }

    public Category getCategoryByName(String name) {
        return categories.stream().filter(c -> c.getName().equals(name)).findFirst().orElse(null);
    }

    public double getTotalIncome() {
        return incomeTransactions.stream().mapToDouble(IncomeTransaction::getAmount).sum();
    }

    public double getTotalAllocated() {
        return categories.stream().mapToDouble(Category::getAllocatedBudget).sum();
    }

    public double getTotalSpent() {
        return expenseTransactions.stream().mapToDouble(ExpenseTransaction::getAmount).sum();
    }

    public double getSpentForCategory(String categoryName) {
        return expenseTransactions.stream()
            .filter(e -> e.getCategoryName().equals(categoryName))
            .mapToDouble(ExpenseTransaction::getAmount).sum();
    }
}
