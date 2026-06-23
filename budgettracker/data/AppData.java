package budgettracker.data;

import java.util.*;

public class AppData {
    private AppSettings settings;
    private Map<String, MonthData> months; // key: "YYYY-MM"
    private List<CategoryTemplate> categoryTemplates;
    private Map<String, String> categoryColors; // categoryName -> hexColor

    public AppData() {
        this.settings = new AppSettings();
        this.months = new LinkedHashMap<>();
        this.categoryTemplates = new ArrayList<>();
        this.categoryColors = new LinkedHashMap<>();
        initializeDefaults();
    }

    private void initializeDefaults() {
        // Default category templates
        String[] names = {"Housing", "Savings", "Food", "Personal Expenses", "Transportation", "Miscellaneous"};
        double[] budgets = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        String[] colors = {"#4A90E2", "#2ECC71", "#F39C12", "#9B59B6", "#E74C3C", "#95A5A6"};
        boolean[] protectedFlags = {false, false, false, false, false, true};

        for (int i = 0; i < names.length; i++) {
            categoryTemplates.add(new CategoryTemplate(names[i], budgets[i], protectedFlags[i]));
            categoryColors.put(names[i], colors[i]);
        }
    }

    public static String monthKey(int year, int month) {
        return String.format("%04d-%02d", year, month);
    }

    public MonthData getOrCreateMonth(int year, int month) {
        String key = monthKey(year, month);
        if (!months.containsKey(key)) {
            MonthData md = new MonthData(year, month);
            // Populate from template
            for (CategoryTemplate t : categoryTemplates) {
                md.getCategories().add(new Category(t.getName(), t.getDefaultBudget(), t.isProtected(), true));
            }
            months.put(key, md);
        }
        return months.get(key);
    }

    public MonthData getMonth(int year, int month) {
        return months.get(monthKey(year, month));
    }

    public boolean monthExists(int year, int month) {
        return months.containsKey(monthKey(year, month));
    }

    public String getCategoryColor(String categoryName) {
        return categoryColors.getOrDefault(categoryName, "#95A5A6");
    }

    public void setCategoryColor(String categoryName, String hexColor) {
        categoryColors.put(categoryName, hexColor);
    }

    public void renameCategoryColor(String oldName, String newName) {
        String color = categoryColors.remove(oldName);
        if (color != null) categoryColors.put(newName, color);
    }

    public String assignNextColor(String categoryName) {
        String[] palette = {
            "#4A90E2","#2ECC71","#F39C12","#9B59B6","#E74C3C","#95A5A6",
            "#1ABC9C","#E67E22","#3498DB","#16A085","#8E44AD","#D35400",
            "#27AE60","#2980B9","#C0392B","#7F8C8D","#F1C40F","#E91E63",
            "#00BCD4","#FF5722"
        };
        Set<String> used = new HashSet<>(categoryColors.values());
        for (String c : palette) {
            if (!used.contains(c)) {
                categoryColors.put(categoryName, c);
                return c;
            }
        }
        // All used, pick random
        String color = palette[categoryColors.size() % palette.length];
        categoryColors.put(categoryName, color);
        return color;
    }

    public CategoryTemplate getTemplate(String name) {
        return categoryTemplates.stream().filter(t -> t.getName().equals(name)).findFirst().orElse(null);
    }

    public boolean isInTemplate(String name) {
        return getTemplate(name) != null;
    }

    // Getters / Setters
    public AppSettings getSettings() { return settings; }
    public void setSettings(AppSettings settings) { this.settings = settings; }
    public Map<String, MonthData> getMonths() { return months; }
    public void setMonths(Map<String, MonthData> months) { this.months = months; }
    public List<CategoryTemplate> getCategoryTemplates() { return categoryTemplates; }
    public void setCategoryTemplates(List<CategoryTemplate> categoryTemplates) { this.categoryTemplates = categoryTemplates; }
    public Map<String, String> getCategoryColors() { return categoryColors; }
    public void setCategoryColors(Map<String, String> categoryColors) { this.categoryColors = categoryColors; }

    // Yearly aggregation helper
    public Map<String, double[]> getYearlyAggregation(int year) {
        // Returns map: categoryName -> [totalAllocated, totalSpent]
        Map<String, double[]> result = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            MonthData md = getMonth(year, m);
            if (md == null) continue;
            for (Category c : md.getCategories()) {
                double[] arr = result.computeIfAbsent(c.getName(), k -> new double[2]);
                arr[0] += c.getAllocatedBudget();
                arr[1] += md.getSpentForCategory(c.getName());
            }
        }
        return result;
    }

    public double getYearlyIncome(int year) {
        double total = 0;
        for (int m = 1; m <= 12; m++) {
            MonthData md = getMonth(year, m);
            if (md != null) total += md.getTotalIncome();
        }
        return total;
    }
}
