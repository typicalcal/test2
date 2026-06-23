package budgettracker.data;

public class CategoryTemplate {
    private String name;
    private double defaultBudget;
    private boolean isProtected;

    public CategoryTemplate() {}

    public CategoryTemplate(String name, double defaultBudget, boolean isProtected) {
        this.name = name;
        this.defaultBudget = defaultBudget;
        this.isProtected = isProtected;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getDefaultBudget() { return defaultBudget; }
    public void setDefaultBudget(double defaultBudget) { this.defaultBudget = defaultBudget; }
    public boolean isProtected() { return isProtected; }
    public void setProtected(boolean isProtected) { this.isProtected = isProtected; }
}
