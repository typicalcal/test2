package budgettracker.util;

import budgettracker.data.*;
import java.util.*;

public class BudgetCalculator {

    /** Monthly summary values */
    public static class MonthlySummary {
        public double totalIncome;
        public double totalAllocated;
        public double totalSpent;
        public double remaining; // income - totalAllocated

        public MonthlySummary(double income, double allocated, double spent) {
            this.totalIncome = income;
            this.totalAllocated = allocated;
            this.totalSpent = spent;
            this.remaining = income - allocated;
        }
    }

    /** Yearly summary values */
    public static class YearlySummary {
        public double totalIncome;
        public double totalAllocated;
        public double totalSpent;
        public double remaining;
        public Map<String, double[]> categoryTotals; // name -> [allocated, spent]

        public YearlySummary(double income, double allocated, double spent, Map<String, double[]> cats) {
            this.totalIncome = income;
            this.totalAllocated = allocated;
            this.totalSpent = spent;
            this.remaining = income - allocated;
            this.categoryTotals = cats;
        }
    }

    public static MonthlySummary computeMonthly(MonthData md) {
        if (md == null) return new MonthlySummary(0, 0, 0);
        return new MonthlySummary(md.getTotalIncome(), md.getTotalAllocated(), md.getTotalSpent());
    }

    public static YearlySummary computeYearly(AppData data, int year) {
        Map<String, double[]> cats = data.getYearlyAggregation(year);
        double income = data.getYearlyIncome(year);
        double allocated = cats.values().stream().mapToDouble(a -> a[0]).sum();
        double spent = cats.values().stream().mapToDouble(a -> a[1]).sum();
        return new YearlySummary(income, allocated, spent, cats);
    }

    public static double getSpentForCategory(MonthData md, String categoryName) {
        if (md == null) return 0;
        return md.getSpentForCategory(categoryName);
    }

    public static double getRemainingForCategory(MonthData md, String categoryName) {
        if (md == null) return 0;
        Category cat = md.getCategoryByName(categoryName);
        if (cat == null) return 0;
        return cat.getAllocatedBudget() - md.getSpentForCategory(categoryName);
    }
}
