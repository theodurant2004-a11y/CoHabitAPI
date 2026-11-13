package be.cohabitapi.cohabitapi.Models;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// A charge is a rent or a bill that the owner publishes for his roomies
// Ex: a rent of 850 euros for Nora and Arnaud => one charge with 2 parts of 425 euros
// Each part is an Expense: the roomie is its debtor and the owner is its creditor
// Only the parts are saved in the db. The charge itself is never saved (like Refund):
// it is the object that keeps the parts of the same rent or bill together
public class Charge {

    // ------------------------- Attributes --------------------------------------------------

    private String name;
    private LocalDate dueDate;

    // RENT or BILLS only: the other categories are for the expenses between the roomies
    private ExpenseCategory category;

    // TODO : the recurrence is checked but not saved yet, the table Expense has no column for it
    // So it is null for a charge that was read from the db
    private Recurrence recurrence;

    // One expense for each roomie who has to pay
    private List<Expense> parts = new ArrayList<>();


    // ------------------------- Constructors --------------------------------------------------

    // NEW CHARGE : the charge as the owner typed it in the form, shared between the roomies of the list
    // The texts of the front ("2026-11-28", "rent", "monthly") become objects in the setters, which check them
    // The charge is divided here, so a charge always has its parts
    public Charge(String name, Double total, String dueDate, String category, String recurrence, List<Roomie> sharers) {
        setCategory(category);
        setRecurrence(recurrence);
        setDueDate(dueDate);

        if (sharers == null || sharers.isEmpty())
            throw new IllegalArgumentException("Choose at least one roomie.");

        // A roomie who is twice in the list is counted once
        List<Roomie> debtors = new ArrayList<>();
        for (Roomie sharer : sharers) {
            if (sharer == null)
                throw new IllegalArgumentException("A roomie is required.");
            if (!debtors.contains(sharer))
                debtors.add(sharer);
        }

        // The whole charge is an expense that is not shared yet
        // Its own setters check the name and the amount, so these rules are written only once (in Expense)
        Expense whole = new Expense(chooseName(name), total, LocalDate.now(), this.category);
        whole.setDueDate(this.dueDate);
        this.name = whole.getName();

        // The expense divides its own sum (the total of the parts is exactly the amount typed)
        // Each roomie receives his part: he becomes its debtor
        List<Expense> split = whole.split(debtors.size());
        for (int i = 0; i < debtors.size(); i++) {
            Expense part = split.get(i);

            debtors.get(i).addExpenseToPay(part);
            parts.add(part);
        }
    }

    // CHARGE READ FROM THE DB : it starts with one of its parts, the others are added with addPart
    // Not public because only RoomShare.findCharges puts the parts of the db back together
    Charge(Expense firstPart) {
        if (firstPart == null)
            throw new IllegalArgumentException("The part cannot be null");

        this.name = firstPart.getName();
        this.dueDate = firstPart.getDueDate();
        this.category = firstPart.getType();
        parts.add(firstPart);
    }


    // ------------------------- Getters and Setters --------------------------------------------------

    public String getName() {
        return name;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    // The due date is required for a charge: the roomies must know when they have to pay
    // The front sends a text: "2026-11-28" becomes a LocalDate
    private void setDueDate(String dueDate) {
        if (dueDate == null || dueDate.trim().isEmpty())
            throw new IllegalArgumentException("The due date is required.");

        try {
            // LocalDate.parse reads the ISO format YYYY-MM-DD
            this.dueDate = LocalDate.parse(dueDate.trim());
        } catch (DateTimeParseException e) {
            // We turn it into an IllegalArgumentException so the API answers 400 with a clear message
            throw new IllegalArgumentException("The due date must look like 2026-10-20.");
        }
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    // The code "rent" becomes the category RENT
    private void setCategory(String code) {
        // fromCode throws an IllegalArgumentException if the code does not exist
        ExpenseCategory found = ExpenseCategory.fromCode(code);

        if (found != ExpenseCategory.RENT && found != ExpenseCategory.BILLS)
            throw new IllegalArgumentException("A charge must be a rent or a bill.");

        this.category = found;
    }

    public Recurrence getRecurrence() {
        return recurrence;
    }

    // The code "monthly" becomes the recurrence MONTHLY
    // The form of a bill has no recurrence: when nothing is sent, the charge is published once
    private void setRecurrence(String code) {
        if (code == null || code.trim().isEmpty()) {
            this.recurrence = Recurrence.ONCE;
        } else {
            // fromCode throws an IllegalArgumentException if the code does not exist
            this.recurrence = Recurrence.fromCode(code);
        }
    }

    // Read only: the parts are created by the constructor or added with addPart, never from outside
    public List<Expense> getParts() {
        return Collections.unmodifiableList(parts);
    }


    // ------------------------- Business methods --------------------------------------------------

    // A rent has no title in the form, so the model names it. A bill keeps the title typed by the owner
    private String chooseName(String name) {
        return category == ExpenseCategory.RENT ? "Rent" : name;
    }

    // The total is not an attribute: it is the sum of the parts
    // We add in cents (whole numbers) because the decimals give rounding problems
    public double getTotal() {
        long cents = 0;

        for (Expense part : parts) {
            cents += part.getSumInCents();
        }
        return cents / 100.0;
    }

    // True if this expense is a part of this charge
    // The parts have no link between them in the db: we recognize them because
    // they have the same name, the same category and the same due date
    public boolean matches(Expense part) {
        return part != null
                && Objects.equals(name, part.getName())
                && category == part.getType()
                && Objects.equals(dueDate, part.getDueDate());
    }

    // Adds a part that was read from the db to this charge
    // Not public, same reason as the constructor above
    void addPart(Expense part) {
        if (!matches(part))
            throw new IllegalArgumentException("This expense is not a part of this charge");

        parts.add(part);
    }


    // ------------------------- OVERLOADS --------------------------------------------------

    // A charge is identified by its name, its category and its due date (the same three values as matches)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Charge charge = (Charge) o;
        return Objects.equals(this.name, charge.name)
                && this.category == charge.category
                && Objects.equals(this.dueDate, charge.dueDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, category, dueDate);
    }
}
