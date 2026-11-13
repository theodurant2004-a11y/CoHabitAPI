package be.cohabitapi.cohabitapi.Models;

import java.util.Locale;

// This enum contains how often a charge of the owner comes back (same idea as ExpenseCategory)
// ONCE = the charge is published one time, MONTHLY = every month, YEARLY = every year
public enum Recurrence {
    ONCE("once", "Once"),
    MONTHLY("monthly", "Every month"),
    YEARLY("yearly", "Every year");

    private final String code;   // what the front sends
    private final String label;  // the name displayed in the form

    // The constructor of an enum is called once for each value above
    Recurrence(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    // Gives the recurrence that has this code: "monthly" gives MONTHLY
    // It is static because we only have a text here: there is no recurrence yet to call the method on
    // An unknown code is refused: the exception becomes a 400 in the API
    public static Recurrence fromCode(String code) {
        Recurrence found = null;

        if (code != null) {
            // Locale.ROOT makes the result the same on every computer
            String clean = code.trim().toLowerCase(Locale.ROOT);
            for (Recurrence recurrence : values()) {
                if (recurrence.code.equals(clean)) {
                    found = recurrence;
                }
            }
        }

        if (found == null)
            throw new IllegalArgumentException("The recurrence is not valid.");

        return found;
    }
}
