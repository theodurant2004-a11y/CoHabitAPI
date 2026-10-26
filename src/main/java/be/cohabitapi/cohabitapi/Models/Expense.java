package be.cohabitapi.cohabitapi.Models;

import jakarta.persistence.*;

import java.time.LocalDate;

// One thing we need to make clear is that one Expense is a debt: we can see it as : a roomie (the debtor) owes a sum to the roomie who paid (the creditor)
// Ex :groceries of 45 euros paid by Nora and shared by Arnaud, Manoah and Hom nix
// 2 rows: Arnaud owes 15 to Nora and Hom nix owes 15 to Nora (Nora doesnt owe money to herself)
@Entity
@Table(name = "Expense")
public class Expense {

    //   Attributes and JPA Annotations

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_id")
    private Integer id;

    // The description typed by the roomie
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    // The share of the debtor
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

    // The code of the category, in ExpenseCategory : the enum
    @Column(name = "type", length = 50)
    private String type;

    // The roomie who owes the sum
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Roomie debtor;

    // The person who is owed the sum (the one who paid)
    @ManyToOne
    @JoinColumn(name = "user_id_1")
    private Person creditor;


    // Getters :
    // No setters: an expense is only created by the methods of this class

    public Integer getId() { return id; }
    public String getName() { return name; }
    public Double getSum() { return sum; }
    public LocalDate getCreationDate() { return creationDate; }
    public LocalDate getDueDate() { return dueDate; }
    public boolean isSettled() { return settled; }
    public String getType() { return type; }
    public Roomie getDebtor() { return debtor; }
    public Person getCreditor() { return creditor; }

    // The sum in cents, for ex 4.5 euros => 450
    public long getSumInCents() {
        return Math.round(sum * 100);
    }

    // The category of this expense
    public ExpenseCategory getCategory() {
        ExpenseCategory category = ExpenseCategory.findByCode(type);
        return category == null ? ExpenseCategory.OTHER : category;
    }


    // Constructors

    // Not public because5 only Hibernate needs it to rebuild an object from the db
    protected Expense() {
    }

    // This constructor checks everything, so an invalid expense can never exist
    // And an new expense is always pending
    public Expense(String name, double sum, LocalDate creationDate, String type, Roomie debtor, Person creditor) {
        this.name = normalizeName(name);

        if (!(sum > 0))
            throw new IllegalArgumentException("The amount is too small to be split.");
        this.sum = sum;

        if (creationDate == null)
            throw new IllegalArgumentException("The date is required.");
        this.creationDate = creationDate;

        this.type = ExpenseCategory.fromCode(type).getCode();

        if (debtor == null || creditor == null)
            throw new IllegalArgumentException("The debtor and the creditor are required.");
        this.debtor = debtor;
        this.creditor = creditor;
        this.settled = false;
    }


    // Utility methods
    // Checks the description, required and 50 characters max. And removes the spaces around it
    private static String normalizeName(String description) {
        if (description == null || description.trim().isEmpty())
            throw new IllegalArgumentException("The description is required.");

        String trimmed = description.trim();

        if (trimmed.length() > 50)
            throw new IllegalArgumentException("The description cannot be longer than 50 characters.");

        return trimmed;
    }
}