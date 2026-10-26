package be.cohabitapi.cohabitapi.Models;

import java.util.Locale;

// This enum contains the categories of an expense
// The code of the category is saved in the column type of the table Expense
// The icons are written with unicode escapes (uXXXX) so the file works with any encoding
public enum ExpenseCategory {
    FOOD("food", "Food", "\uD83D\uDED2"),
    RENT("rent", "Rent", "\uD83C\uDFE0"),
    BILLS("bills", "Bills", "\uD83D\uDCA1"),
    HOUSEHOLD("household", "Household", "\uD83E\uDDFD"),
    LEISURE("leisure", "Leisure", "\uD83C\uDF89"),
    OTHER("other", "Other", "\uD83D\uDCE6");

    private final String code;   // what we save in the db and what the front sends
    private final String label;  // the name displayed in the form
    private final String icon;   // the icon displayed in the list

    // The constructor of an enum is called once for each value above
    ExpenseCategory(String code, String label, String icon) {
        this.code = code;
        this.label = label;
        this.icon = icon;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
    public String getIcon() { return icon; }

    // This method is used to find a category with its code
    // It returns null if there is none, we use it to read the db, so it never crashes
    public static ExpenseCategory findByCode(String code) {
        ExpenseCategory found = null;

        if (code != null) {
            String clean = code.trim().toLowerCase(Locale.ROOT);
            for (ExpenseCategory category : values()) {
                if (category.code.equals(clean)) {
                    found = category;
                }
            }
        }

        return found;
    }

    // Same thing but for the data sent by the user: an unknown code is refused
    // The exception becomes a 400  in the API
    public static ExpenseCategory fromCode(String code) {
        ExpenseCategory category = findByCode(code);

        if (category == null)
            throw new IllegalArgumentException("The category is not valid.");

        return category;
    }
}