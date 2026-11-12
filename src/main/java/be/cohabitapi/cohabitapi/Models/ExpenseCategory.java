package be.cohabitapi.cohabitapi.Models;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

// This enum contains the categories of an expense (same idea as MembershipStatus and TaskStatus)
// The icon of each category is chosen by the front, the model only knows the code and the label
public enum ExpenseCategory {
    FOOD("food", "Food"),
    RENT("rent", "Rent"),
    BILLS("bills", "Bills"),
    HOUSEHOLD("household", "Household"),
    LEISURE("leisure", "Leisure"),
    OTHER("other", "Other");

    private final String code;   // what is saved in the column type of the table Expense, and what the front sends
    private final String label;  // the name displayed in the form

    // The constructor of an enum is called once for each value above
    ExpenseCategory(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    // Gives the category that has this code: "food" gives FOOD
    // It is static because we only have a text here: there is no category yet to call the method on
    // An unknown code is refused: the exception becomes a 400 in the API
    public static ExpenseCategory fromCode(String code) {
        ExpenseCategory found = null;

        if (code != null) {
            // Locale.ROOT makes the result the same on every computer
            String clean = code.trim().toLowerCase(Locale.ROOT);
            for (ExpenseCategory category : values()) {
                if (category.code.equals(clean)) {
                    found = category;
                }
            }
        }

        if (found == null)
            throw new IllegalArgumentException("The category is not valid.");

        return found;
    }

    // Technical helper: tells JPA how to store this enum in the column type (a text)
    // Thanks to it, Expense has a real ExpenseCategory attribute and not a String
    // It lives inside the enum, like the converters of MembershipStatus and TaskStatus
    @Converter
    public static class DbConverter implements AttributeConverter<ExpenseCategory, String> {

        // Java -> db: called by JPA when it saves an expense (FOOD becomes "food")
        @Override
        public String convertToDatabaseColumn(ExpenseCategory category) {
            // null stays null
            return category == null ? null : category.getCode();
        }

        // db -> Java: called by JPA when it loads an expense ("food" becomes FOOD)
        @Override
        public ExpenseCategory convertToEntityAttribute(String code) {
            ExpenseCategory category = null;

            // null stays null: Expense.getType answers OTHER for an expense without category
            if (code != null) {
                try {
                    category = ExpenseCategory.fromCode(code);
                } catch (IllegalArgumentException e) {
                    // An old row with a type that is not in the enum must not crash the whole page
                    category = OTHER;
                }
            }

            return category;
        }
    }
}