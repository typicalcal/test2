package budgettracker.data;

public class ExpenseTransaction {
    private String id;
    private double amount;
    private String categoryName;
    private String date; // ISO format: yyyy-MM-dd
    private String description;

    public ExpenseTransaction() {}

    public ExpenseTransaction(String id, double amount, String categoryName, String date, String description) {
        this.id = id;
        this.amount = amount;
        this.categoryName = categoryName;
        this.date = date;
        this.description = description;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getYear() {
        return Integer.parseInt(date.substring(0, 4));
    }

    public int getMonth() {
        return Integer.parseInt(date.substring(5, 7));
    }
}
