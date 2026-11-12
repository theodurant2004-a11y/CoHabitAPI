package be.cohabitapi.cohabitapi.Models;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

// An expense is a sum of money that a roomie (the debtor) owes to the person who paid (the creditor)
// Ex: groceries of 45 euros paid by Nora and shared with Arnaud and Manoah
// => 2 expenses: Arnaud owes 15 to Nora and Manoah owes 15 to Nora (Nora doesn't owe money to herself)
@Entity
@Table(name = "Expense")
public class Expense {

    // ------------------------- Attributes and JPA Annotations --------------------------------------------------

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_id")
    private Integer id; // Integer because the id is null before the insert

    // The description typed by the roomie
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    // The amount of money
    @Column(name = "sum_", nullable = false)
    private Double sum;

    // The date of the purchase
    @Column(name = "creation_date", nullable = false)
    private LocalDate creationDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    // false = pending, true = settled
    @Column(name = "is_settled", nullable = false)
    private boolean settled;

    // The category of the expense: an object of the enum ExpenseCategory, not a text
    // The converter saves its code ("food") in the column type
    @Convert(converter = ExpenseCategory.DbConverter.class)
    @Column(name = "type", length = 50)
    private ExpenseCategory type;

    // The roomie who owes the sum
    // It is filled by Roomie.addExpenseToPay, when the expense enters the list of the roomie
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Roomie debtor;

    // The person who is owed the sum (the one who paid)
    // It is filled by Person.addExpenseToReceive, when the expense enters the list of the person
    @ManyToOne
    @JoinColumn(name = "user_id_1")
    private Person creditor;


    // ------------------------- Constructors --------------------------------------------------

    // Only for Hibernate
    protected Expense() {
    }

    // The checks are in the setters. A new expense is always pending
    // The date and the category arrive as texts from the front ("2026-10-20" and "food"):
    // the setters that receive a text turn them into objects and check them
    // It has no debtor and no creditor yet: the roomie and the person who add it to their list set them
    // (same idea as RoomShare.addRoomie, which sets the roomshare of the roomie)
    public Expense(String name, Double sum, String creationDate, String type) {
        setName(name);
        setSum(sum);
        setCreationDate(creationDate);
        setType(type);
    }

    // without writing them as texts just to read them again
    public Expense(String name, Double sum, LocalDate creationDate, ExpenseCategory type) {
        setName(name);
        setSum(sum);
        setCreationDate(creationDate);
        setType(type);
    }


    // ------------------------- Getters and Setters --------------------------------------------------

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // The description is required and maximum 50 characters (the size of the column)
    public void setName(String name) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("The description is required.");

        String trimmed = name.trim();
        if (trimmed.length() > 50)
            throw new IllegalArgumentException("The description cannot be longer than 50 characters.");

        this.name = trimmed;
    }

    public Double getSum() {
        return sum;
    }

    // The amount must be a real number greater than 0 (a missing amount, NaN and infinite are refused too)
    public void setSum(Double sum) {
        if (sum == null || sum.isNaN() || sum.isInfinite() || sum <= 0)
            throw new IllegalArgumentException("The amount must be greater than 0.");
        if (sum > 100000)
            throw new IllegalArgumentException("The amount is too high.");

        this.sum = sum;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    // The date of the purchase is required: an expense always happened on a day
    public void setCreationDate(LocalDate creationDate) {
        if (creationDate == null)
            throw new IllegalArgumentException("The date is required.");
        this.creationDate = creationDate;
    }

    // Same setter for the text sent by the front: "2026-10-20" becomes a LocalDate
    public void setCreationDate(String creationDate) {
        if (creationDate == null || creationDate.trim().isEmpty())
            throw new IllegalArgumentException("The date is required.");

        try {
            // LocalDate.parse reads the ISO format YYYY-MM-DD
            setCreationDate(LocalDate.parse(creationDate.trim()));
        } catch (DateTimeParseException e) {
            // We turn it into an IllegalArgumentException so the API answers 400 with a clear message
            throw new IllegalArgumentException("The date must look like 2026-10-20.");
        }
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    // The due date is optional (null = no deadline)
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    // No setter for settled: the status only changes with settle(), which checks the rule
    public boolean isSettled() {
        return settled;
    }

    // An expense without category in the db is shown as OTHER
    public ExpenseCategory getType() {
        return type == null ? ExpenseCategory.OTHER : type;
    }

    // The category is required: the form always sends one, and the list of the front shows it
    public void setType(ExpenseCategory type) {
        if (type == null)
            throw new IllegalArgumentException("The category is required.");
        this.type = type;
    }

    // Same setter for the text sent by the front: the code "food" becomes the category FOOD
    public void setType(String code) {
        // fromCode throws an IllegalArgumentException if the code does not exist
        setType(ExpenseCategory.fromCode(code));
    }

    // The roomie who owes the sum (null while nobody has put the expense in his list)
    public Roomie getDebtor() {
        return debtor;
    }

    // Not public because nobody outside the model must use this setter:
    // only Roomie.addExpenseToPay calls it, so the expense and the list of the roomie always say the same thing
    void setDebtor(Roomie debtor) {
        if (debtor == null)
            throw new IllegalArgumentException("The debtor is required.");
        this.debtor = debtor;
    }

    // The person who is owed the sum (null while nobody has put the expense in his list)
    public Person getCreditor() {
        return creditor;
    }

    // Not public, same reason: only Person.addExpenseToReceive calls it
    void setCreditor(Person creditor) {
        if (creditor == null)
            throw new IllegalArgumentException("The creditor is required.");
        this.creditor = creditor;
    }


    // ------------------------- Business methods --------------------------------------------------

    // The sum in cents, for ex 4.5 euros => 450
    // We calculate in cents (whole numbers) because the decimals give rounding problems
    public long getSumInCents() {
        return Math.round(sum * 100);
    }

    // What this expense still counts for in the totals and the balances
    // The expense knows if it is settled, so the rule "a settled expense counts for zero" is here
    public long getPendingSumInCents() {
        return settled ? 0 : getSumInCents();
    }

    // SPLIT : divides this expense in equal parts and gives one new expense for each part
    // The parts have the same name, dates and category, and no debtor yet: each roomie receives his part after
    // Ex: 10.00 euros in 3 parts = 3.34 + 3.33 + 3.33. The first parts get one more cent until nothing is left,
    // so the total of the parts is exactly the sum of this expense
    public List<Expense> split(int numberOfParts) {
        if (numberOfParts <= 0)
            throw new IllegalArgumentException("Choose at least one roomie to split with.");

        long totalCents = getSumInCents();

        // Each part must be at least 1 cent
        if (totalCents < numberOfParts)
            throw new IllegalArgumentException("The amount is too small to be split.");

        long baseCents = totalCents / numberOfParts;  // what every part contains
        long remainder = totalCents % numberOfParts;  // the cents that are left after the division

        List<Expense> parts = new ArrayList<>();
        for (int i = 0; i < numberOfParts; i++) {
            long partCents = baseCents + (i < remainder ? 1 : 0);

            Expense part = new Expense(name, partCents / 100.0, creationDate, getType());
            part.setDueDate(dueDate);
            parts.add(part);
        }

        return parts;
    }

    // SETTLE : marks this expense as paid. A settled expense cannot be settled again
    public void settle() {
        if (settled)
            throw new IllegalArgumentException("This expense is already settled.");
        settled = true;
    }


    // ------------------------- Overloads --------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Expense expense = (Expense) o;

        return id != null && id.equals(expense.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}