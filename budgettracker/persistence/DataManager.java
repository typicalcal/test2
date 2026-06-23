package budgettracker.persistence;

import budgettracker.data.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * JSON persistence — no external libraries required.
 */
public class DataManager {
    private static final String DATA_DIR  = System.getProperty("user.home") + File.separator + ".budgettracker";
    private static final String DATA_FILE = DATA_DIR + File.separator + "data.json";

    public static String getDataDir()  { return DATA_DIR; }
    public static String getDataFile() { return DATA_FILE; }

    // ─── Save / Load ─────────────────────────────────────────────────────────

    public static void save(AppData data) {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            Files.writeString(Paths.get(DATA_FILE), toJson(data));
        } catch (IOException e) {
            System.err.println("Save failed: " + e.getMessage());
        }
    }

    public static AppData load() {
        Path p = Paths.get(DATA_FILE);
        if (!Files.exists(p)) return new AppData();
        try {
            return fromJson(Files.readString(p));
        } catch (Exception e) {
            System.err.println("Load failed: " + e.getMessage());
            return new AppData();
        }
    }

    // ─── Public encode/decode (used by SnapshotManager) ─────────────────────

    public static String toJson(AppData d) {
        return encodeAppData(d);
    }

    public static AppData fromJson(String json) {
        try {
            Object parsed = new JsonParser(json).parse();
            if (!(parsed instanceof Map)) return new AppData();
            return decodeAppData(cast(parsed));
        } catch (Exception e) {
            System.err.println("JSON parse error: " + e.getMessage());
            return new AppData();
        }
    }

    // ─── Encoders ────────────────────────────────────────────────────────────

    private static String encodeAppData(AppData d) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("\"settings\":").append(encodeSettings(d.getSettings())).append(",\n");
        sb.append("\"categoryTemplates\":").append(encodeTemplateList(d.getCategoryTemplates())).append(",\n");
        sb.append("\"categoryColors\":").append(encodeStringMap(d.getCategoryColors())).append(",\n");
        sb.append("\"months\":").append(encodeMonthMap(d.getMonths())).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private static String encodeSettings(AppSettings s) {
        return "{\"currency\":" + q(s.getCurrency()) +
               ",\"availableCurrencies\":" + encodeStringList(s.getAvailableCurrencies()) + "}";
    }

    private static String encodeTemplateList(List<CategoryTemplate> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            CategoryTemplate t = list.get(i);
            sb.append("{\"name\":").append(q(t.getName()))
              .append(",\"defaultBudget\":").append(t.getDefaultBudget())
              .append(",\"isProtected\":").append(t.isProtected()).append("}");
        }
        return sb.append("]").toString();
    }

    private static String encodeMonthMap(Map<String, MonthData> months) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, MonthData> e : months.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append(q(e.getKey())).append(":").append(encodeMonthData(e.getValue()));
        }
        return sb.append("}").toString();
    }

    private static String encodeMonthData(MonthData md) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"year\":").append(md.getYear()).append(",");
        sb.append("\"month\":").append(md.getMonth()).append(",");
        sb.append("\"categories\":").append(encodeCategoryList(md.getCategories())).append(",");
        sb.append("\"incomeTransactions\":").append(encodeIncomeList(md.getIncomeTransactions())).append(",");
        sb.append("\"expenseTransactions\":").append(encodeExpenseList(md.getExpenseTransactions()));
        return sb.append("}").toString();
    }

    private static String encodeCategoryList(List<Category> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            Category c = list.get(i);
            sb.append("{\"name\":").append(q(c.getName()))
              .append(",\"allocatedBudget\":").append(c.getAllocatedBudget())
              .append(",\"isProtected\":").append(c.isProtected())
              .append(",\"isTemplateCategory\":").append(c.isTemplateCategory()).append("}");
        }
        return sb.append("]").toString();
    }

    private static String encodeIncomeList(List<IncomeTransaction> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            IncomeTransaction t = list.get(i);
            sb.append("{\"id\":").append(q(t.getId()))
              .append(",\"amount\":").append(t.getAmount())
              .append(",\"source\":").append(q(t.getSource()))
              .append(",\"date\":").append(q(t.getDate())).append("}");
        }
        return sb.append("]").toString();
    }

    private static String encodeExpenseList(List<ExpenseTransaction> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            ExpenseTransaction t = list.get(i);
            sb.append("{\"id\":").append(q(t.getId()))
              .append(",\"amount\":").append(t.getAmount())
              .append(",\"categoryName\":").append(q(t.getCategoryName()))
              .append(",\"date\":").append(q(t.getDate()))
              .append(",\"description\":").append(
                  t.getDescription() == null ? "null" : q(t.getDescription()))
              .append("}");
        }
        return sb.append("]").toString();
    }

    private static String encodeStringList(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(q(list.get(i)));
        }
        return sb.append("]").toString();
    }

    private static String encodeStringMap(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append(q(e.getKey())).append(":").append(q(e.getValue()));
        }
        return sb.append("}").toString();
    }

    /** Quote and escape a string as a JSON value. */
    private static String q(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:   sb.append(c);      break;
            }
        }
        return sb.append("\"").toString();
    }

    // ─── Decoders ────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static AppData decodeAppData(Map<String, Object> m) {
        AppData d = new AppData();
        if (m == null) return d;

        if (m.containsKey("settings"))
            d.setSettings(decodeSettings(cast(m.get("settings"))));

        if (m.containsKey("categoryTemplates"))
            d.setCategoryTemplates(decodeTemplateList((List<Object>) m.get("categoryTemplates")));

        if (m.containsKey("categoryColors"))
            d.setCategoryColors(decodeStringMap(cast(m.get("categoryColors"))));

        if (m.containsKey("months")) {
            Map<String, Object> rawMonths = cast(m.get("months"));
            Map<String, MonthData> months = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : rawMonths.entrySet())
                months.put(e.getKey(), decodeMonthData(cast(e.getValue())));
            d.setMonths(months);
        }
        return d;
    }

    private static AppSettings decodeSettings(Map<String, Object> m) {
        AppSettings s = new AppSettings();
        if (m == null) return s;
        if (m.containsKey("currency")) s.setCurrency((String) m.get("currency"));
        return s;
    }

    @SuppressWarnings("unchecked")
    private static List<CategoryTemplate> decodeTemplateList(List<Object> raw) {
        List<CategoryTemplate> list = new ArrayList<>();
        if (raw == null) return list;
        for (Object o : raw) {
            Map<String, Object> m = cast(o);
            CategoryTemplate t = new CategoryTemplate();
            if (m.containsKey("name"))          t.setName((String) m.get("name"));
            if (m.containsKey("defaultBudget")) t.setDefaultBudget(toDouble(m.get("defaultBudget")));
            if (m.containsKey("isProtected"))   t.setProtected(toBoolean(m.get("isProtected")));
            list.add(t);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private static MonthData decodeMonthData(Map<String, Object> m) {
        MonthData md = new MonthData();
        if (m == null) return md;
        if (m.containsKey("year"))  md.setYear(toInt(m.get("year")));
        if (m.containsKey("month")) md.setMonth(toInt(m.get("month")));
        if (m.containsKey("categories"))
            md.setCategories(decodeCategoryList((List<Object>) m.get("categories")));
        if (m.containsKey("incomeTransactions"))
            md.setIncomeTransactions(decodeIncomeList((List<Object>) m.get("incomeTransactions")));
        if (m.containsKey("expenseTransactions"))
            md.setExpenseTransactions(decodeExpenseList((List<Object>) m.get("expenseTransactions")));
        return md;
    }

    @SuppressWarnings("unchecked")
    private static List<Category> decodeCategoryList(List<Object> raw) {
        List<Category> list = new ArrayList<>();
        if (raw == null) return list;
        for (Object o : raw) {
            Map<String, Object> m = cast(o);
            Category c = new Category();
            if (m.containsKey("name"))               c.setName((String) m.get("name"));
            if (m.containsKey("allocatedBudget"))    c.setAllocatedBudget(toDouble(m.get("allocatedBudget")));
            if (m.containsKey("isProtected"))        c.setProtected(toBoolean(m.get("isProtected")));
            if (m.containsKey("isTemplateCategory")) c.setTemplateCategory(toBoolean(m.get("isTemplateCategory")));
            list.add(c);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private static List<IncomeTransaction> decodeIncomeList(List<Object> raw) {
        List<IncomeTransaction> list = new ArrayList<>();
        if (raw == null) return list;
        for (Object o : raw) {
            Map<String, Object> m = cast(o);
            IncomeTransaction t = new IncomeTransaction();
            if (m.containsKey("id"))     t.setId((String) m.get("id"));
            if (m.containsKey("amount")) t.setAmount(toDouble(m.get("amount")));
            if (m.containsKey("source")) t.setSource((String) m.get("source"));
            if (m.containsKey("date"))   t.setDate((String) m.get("date"));
            list.add(t);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private static List<ExpenseTransaction> decodeExpenseList(List<Object> raw) {
        List<ExpenseTransaction> list = new ArrayList<>();
        if (raw == null) return list;
        for (Object o : raw) {
            Map<String, Object> m = cast(o);
            ExpenseTransaction t = new ExpenseTransaction();
            if (m.containsKey("id"))           t.setId((String) m.get("id"));
            if (m.containsKey("amount"))       t.setAmount(toDouble(m.get("amount")));
            if (m.containsKey("categoryName")) t.setCategoryName((String) m.get("categoryName"));
            if (m.containsKey("date"))         t.setDate((String) m.get("date"));
            if (m.containsKey("description") && m.get("description") != null)
                t.setDescription((String) m.get("description"));
            list.add(t);
        }
        return list;
    }

    private static Map<String, String> decodeStringMap(Map<String, Object> raw) {
        Map<String, String> map = new LinkedHashMap<>();
        if (raw == null) return map;
        for (Map.Entry<String, Object> e : raw.entrySet())
            if (e.getValue() != null) map.put(e.getKey(), e.getValue().toString());
        return map;
    }

    // ─── Type coercions ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<>();
    }

    private static double toDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0.0; }
    }

    private static int toInt(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return 0; }
    }

    private static boolean toBoolean(Object o) {
        if (o instanceof Boolean) return (Boolean) o;
        return "true".equalsIgnoreCase(o.toString());
    }

    // ─── Minimal recursive-descent JSON parser ───────────────────────────────

    static class JsonParser {
        private final String src;
        private int pos = 0;

        JsonParser(String src) { this.src = src; }

        Object parse() {
            skipWs();
            if (pos >= src.length()) return null;
            char c = src.charAt(pos);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't') { pos += 4; return Boolean.TRUE; }
            if (c == 'f') { pos += 5; return Boolean.FALSE; }
            if (c == 'n') { pos += 4; return null; }
            return parseNumber();
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // '{'
            skipWs();
            while (pos < src.length() && src.charAt(pos) != '}') {
                skipWs();
                if (src.charAt(pos) != '"') break;
                String key = parseString();
                skipWs();
                if (pos < src.length() && src.charAt(pos) == ':') pos++;
                skipWs();
                Object val = parse();
                map.put(key, val);
                skipWs();
                if (pos < src.length() && src.charAt(pos) == ',') pos++;
                skipWs();
            }
            if (pos < src.length()) pos++; // '}'
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++; // '['
            skipWs();
            while (pos < src.length() && src.charAt(pos) != ']') {
                list.add(parse());
                skipWs();
                if (pos < src.length() && src.charAt(pos) == ',') pos++;
                skipWs();
            }
            if (pos < src.length()) pos++; // ']'
            return list;
        }

        private String parseString() {
            pos++; // opening '"'
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') break;
                if (c == '\\' && pos < src.length()) {
                    char esc = src.charAt(pos++);
                    switch (esc) {
                        case '"':  sb.append('"');  break;
                        case '\\': sb.append('\\'); break;
                        case 'n':  sb.append('\n'); break;
                        case 'r':  sb.append('\r'); break;
                        case 't':  sb.append('\t'); break;
                        default:   sb.append(esc);  break;
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Number parseNumber() {
            int start = pos;
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) break;
                pos++;
            }
            String num = src.substring(start, pos).trim();
            try {
                if (num.contains(".") || num.contains("E") || num.contains("e"))
                    return Double.parseDouble(num);
                return Long.parseLong(num);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        private void skipWs() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
        }
    }
}
