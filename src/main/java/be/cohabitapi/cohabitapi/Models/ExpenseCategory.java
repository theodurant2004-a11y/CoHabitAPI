package be.cohabitapi.cohabitapi.Models;

import java.util.Locale;

// This enum contains the categories of an expense
// The code of the category is saved in the column type of the table Expense
// The icon of each category is chosen by the front, the model only knows the code and the label
public enum ExpenseCategory {
    FOOD("food", "Food"),
    RENT("rent", "Rent"),
    BILLS("bills", "Bills"),
    HOUSEHOLD("household", "Household"),
    LEISURE("leisure", "Leisure"),
    OTHER("other", "Other");

    private final String code;   // what we save in the db and what the front sends
    private final String label;  // the name displayed in the form

    // The constructor of an enum is called once for each value above
    ExpenseCategory(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    // This method is used to find a category with its code
    // It returns null if there is none, we use it to read the db like this it never crashes
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