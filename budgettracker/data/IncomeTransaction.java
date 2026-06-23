package budgettracker.data;

public class IncomeTransaction {
    private String id;
    private double amount;
    private String source;
    private String date; // ISO format: yyyy-MM-dd

    public IncomeTransaction() {}

    public IncomeTransaction(String id, double amount, String source, String date) {
        this.id = id;
        this.amount = amount;
        this.source = source;
        this.date = date;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getYear() {
        return Integer.parseInt(date.substring(0, 4));
    }

    public int getMonth() {
        return Integer.parseInt(date.substring(5, 7));
    }
}
