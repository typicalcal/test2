package budgettracker.data;

import java.util.Arrays;
import java.util.List;

public class AppSettings {
    private String currency;
    private List<String> availableCurrencies;

    public AppSettings() {
        this.currency = "USD";
        this.availableCurrencies = Arrays.asList(
            "USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY",
            "SEK", "NOK", "DKK", "NZD", "MXN", "SGD", "HKD", "KRW",
            "BRL", "INR", "ZAR", "AED"
        );
    }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public List<String> getAvailableCurrencies() { return availableCurrencies; }

    public String getCurrencySymbol() {
        switch (currency) {
            case "USD": return "$";
            case "EUR": return "€";
            case "GBP": return "£";
            case "JPY": return "¥";
            case "CAD": return "CA$";
            case "AUD": return "A$";
            case "CHF": return "Fr";
            case "CNY": return "¥";
            case "SEK": return "kr";
            case "NOK": return "kr";
            case "DKK": return "kr";
            case "NZD": return "NZ$";
            case "MXN": return "Mex$";
            case "SGD": return "S$";
            case "HKD": return "HK$";
            case "KRW": return "₩";
            case "BRL": return "R$";
            case "INR": return "₹";
            case "ZAR": return "R";
            case "AED": return "د.إ";
            default: return currency + " ";
        }
    }
}
