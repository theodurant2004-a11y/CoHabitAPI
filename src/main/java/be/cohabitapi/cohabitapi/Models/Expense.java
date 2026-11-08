package be.cohabitapi.cohabitapi.Models;

import jakarta.persistence.*;

import java.time.LocalDate;

// One Expense is a debt: a roomie (the debtor) owes a sum to the person who paid (the creditor)
// Ex: groceries of 45 euros paid by Nora and shared with Arnaud and Manoah
// 2 rows: Arnaud owes 15 to Nora and Manoah owes 15 to Nora (Nora doesn't owe money to herself)
@Entity
@Table(name = "Expense")
public class Expense {

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

    // The code of the category (see ExpenseCategory)
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


    // Getters
    public Integer getId() { return id; }
    public String getName() { return name; }
    public Double getSum() { return sum; }
    public LocalDate getCreationDate() { return creationDate; }
    public LocalDate getDueDate() { return dueDate; }
    public boolean isSettled() { return settled; }
    public String getType() { return type; }
    public Roomie getDebtor() { return debtor; }
    public Person getCreditor() { return creditor; }

    // Setters: the checks are here
    public void setName(String name) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("The description is required.");

        String trimmed = name.trim();
        if (trimmed.length() > 50)
            throw new IllegalArgumentException("The description cannot be longer than 50 characters.");

        this.name = trimmed;
    }

    public void setSum(double sum) {
        if (!(sum > 0))
            throw new IllegalArgumentException("The amount is too small to be split.");
        this.sum = sum;
    }

    public void setCreationDate(LocalDate creationDate) {
        if (creationDate == null)
            throw new IllegalArgumentException("The date is required.");
        this.creationDate = creationDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public void setSettled(boolean settled) {
        this.settled = settled;
    }

    public void setType(String type) {
        // fromCode throws an IllegalArgumentException if the code does not exist
        this.type = ExpenseCategory.fromCode(type).getCode();
    }

    public void setDebtor(Roomie debtor) {
        if (debtor == null)
            throw new IllegalArgumentException("The debtor and the creditor are required.");
        this.debtor = debtor;
    }

    public void setCreditor(Person creditor) {
        if (creditor == null)
            throw new IllegalArgumentException("The debtor and the creditor are required.");
        this.creditor = creditor;
    }

    // The sum in cents, for ex 4.5 euros => 450
    public long getSumInCents() {
        return Math.round(sum * 100);
    }

    // The category of this expense
    public ExpenseCategory getCategory() {
        ExpenseCategory category = ExpenseCategory.findByCode(type);
        return category == null ? ExpenseCategory.OTHER : category;
    }


    // ---------------------- Constructors --------------------------

    // Only for Hibernate
    protected Expense() {
    }

    // The checks are in the setters. A new expense is always pending
    public Expense(String name, double sum, LocalDate creationDate, String type, Roomie debtor, Person creditor) {
        setName(name);
        setSum(sum);
        setCreationDate(creationDate);
        setType(type);
        setDebtor(debtor);
        setCreditor(creditor);
        setSettled(false);
    }

    // Marks this debt as paid. A settled expense cannot be settled again
    public void settle() {
        if (settled)
            throw new IllegalArgumentException("This expense is already settled.");
        settled = true;
    }
}