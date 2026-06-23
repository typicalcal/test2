package budgettracker.data;

public class Category {
    private String name;
    private double allocatedBudget;
    private boolean isProtected;
    private boolean isTemplateCategory;

    public Category() {}

    public Category(String name, double allocatedBudget, boolean isProtected, boolean isTemplateCategory) {
        this.name = name;
        this.allocatedBudget = allocatedBudget;
        this.isProtected = isProtected;
        this.isTemplateCategory = isTemplateCategory;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getAllocatedBudget() { return allocatedBudget; }
    public void setAllocatedBudget(double allocatedBudget) { this.allocatedBudget = allocatedBudget; }
    public boolean isProtected() { return isProtected; }
    public void setProtected(boolean isProtected) { this.isProtected = isProtected; }
    public boolean isTemplateCategory() { return isTemplateCategory; }
    public void setTemplateCategory(boolean isTemplateCategory) { this.isTemplateCategory = isTemplateCategory; }
}
