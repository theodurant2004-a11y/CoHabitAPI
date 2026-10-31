package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.ExpenseDAO;
import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;


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

    // add an expense

    // This method is the whole "add an expense" use case, hte API only calls this method
    // It creates one debt for each roomie of the split (except the one who paid) and saves them together
    public static List<Expense> addExpense(String roomieEmail, String description, Double amount, String date,
                                           Integer paidById, String category, List<Integer> sharedWithIds,
                                           PersonDAO personDAO, ExpenseDAO expenseDAO) {
        // 1. Weclean and check what the user sent,each method throws an IllegalArgumentException
        String cleanName = normalizeName(description);
        long totalCents = toCents(amount);
        LocalDate cleanDate = parseDate(date);
        String cleanType = ExpenseCategory.fromCode(category).getCode();

        if (paidById == null)
            throw new IllegalArgumentException("The roomie who paid is required.");
        // we look for a null id with a stream because contains(null) crashes on an immutable list (List.of)
        if (sharedWithIds == null || sharedWithIds.isEmpty() || sharedWithIds.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Choose at least one roomie to split with.");

        // 2. we find the roomie who is signed in and check that he is a member of a roomshare
        Roomie me = Roomie.findAcceptedMember(roomieEmail, personDAO);

        // 3. The roomies of his roomshare, the one who paid and the ones of the split must be in it
        List<Roomie> members = me.findRoomies(personDAO);
        Roomie payer = findById(members, paidById);

        // A TreeSet removes the duplicates and sorts the ids so the split is always the same
        List<Roomie> sharers = new ArrayList<>();
        for (Integer id : new TreeSet<>(sharedWithIds)) {
            sharers.add(findById(members, id));
        }

        // 4. we divide the total in cents. 1000 cents between 3 = 334 + 333 + 333:
        // the first roomies of the list get one more cent until nothing is left, so the total is exact
        int count = sharers.size();
        long baseCents = totalCents / count;
        long remainder = totalCents % count;

        List<Expense> debts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            long shareCents = baseCents + (i < remainder ? 1 : 0);
            Roomie sharer = sharers.get(i);

            // The roomie who paid does not owe money to himself so no debt for him
            if (!sharer.getIdPerson().equals(payer.getIdPerson())) {
                debts.add(new Expense(cleanName, shareCents / 100.0, cleanDate, cleanType, sharer, payer));
            }
        }

        if (debts.isEmpty())
            throw new IllegalArgumentException("Choose at least one other roomie than the one who paid.");

        // 5. Save all the debts in one transaction: everything is saved, or nothing
        expenseDAO.createAll(debts);

        return debts;
    }

    // see my expenses
    // This method is used to get all the debts of the roomshare of a roomie
    public static List<Expense> findAllOfRoomshare(Roomie me, ExpenseDAO expenseDAO) {
        return expenseDAO.findByRoomshare(me.getRoomshare().getId());
    }

    // This method keeps only the debts that concern me
    public static List<Expense> keepMine(Roomie me, List<Expense> all) {
        List<Expense> mine = new ArrayList<>();

        for (Expense expense : all) {
            if (expense.concerns(me))
                mine.add(expense);
        }

        return mine;
    }

    // The total i have to pay so its only the debts not settled count
    public static double calculateToPay(Roomie me, List<Expense> all) {
        long cents = 0;

        for (Expense expense : all) {
            if (!expense.settled && expense.debtor.getIdPerson().equals(me.getIdPerson()))
                cents += expense.getSumInCents();
        }

        return cents / 100.0;
    }

    // The total the others have to give me so only the debts not settled count
    public static double calculateToReceive(Roomie me, List<Expense> all) {
        long cents = 0;

        for (Expense expense : all) {
            if (!expense.settled && expense.creditor != null
                    && expense.creditor.getIdPerson().equals(me.getIdPerson()))
                cents += expense.getSumInCents();
        }

        return cents / 100.0;
    }

    // This method gives the balance of each roomie in cents
    // if its positive => he is owed money and if its negative => he owes money
    public static Map<Integer, Long> calculateBalances(List<Roomie> roomies, List<Expense> all) {
        Map<Integer, Long> balances = new LinkedHashMap<>();

        // Everybody starts with 0
        for (Roomie roomie : roomies) {
            balances.put(roomie.getIdPerson(), 0L);
        }

        for (Expense expense : all) {
            if (!expense.settled) {
                long cents = expense.getSumInCents();
                Integer debtorId = expense.debtor.getIdPerson();

                // The debtor loses the sum
                if (balances.containsKey(debtorId))
                    balances.merge(debtorId, -cents, Long::sum);

                // and the creditor gets it
                if (expense.creditor != null && balances.containsKey(expense.creditor.getIdPerson()))
                    balances.merge(expense.creditor.getIdPerson(), cents, Long::sum);
            }
        }

        return balances;
    }

    // This method suggests a few refunds to settle everything : the roomie who owes the most pays
    // A refund is an Expense that is NOT saved in the db
    public static List<Expense> suggestRefunds(List<Roomie> roomies, Map<Integer, Long> balances) {
        List<Roomie> debtors = new ArrayList<>();
        List<Roomie> creditors = new ArrayList<>();

        // We separate the roomies who owe money from the ones who are owed money
        for (Roomie roomie : roomies) {
            long cents = balances.get(roomie.getIdPerson());
            if (cents < 0) debtors.add(roomie);
            else if (cents > 0) creditors.add(roomie);
        }

        // The one who owes the most is first, and the one who is owed the most is first
        debtors.sort(Comparator.comparingLong(roomie -> balances.get(roomie.getIdPerson())));
        creditors.sort(Comparator.comparingLong((Roomie roomie) -> balances.get(roomie.getIdPerson())).reversed());

        // What is left to pay/to receive so we work on a copy, the real balances do not change
        Map<Integer, Long> left = new HashMap<>(balances);
        List<Expense> refunds = new ArrayList<>();
        int d = 0;
        int c = 0;

        while (d < debtors.size() && c < creditors.size()) {
            Roomie debtor = debtors.get(d);
            Roomie creditor = creditors.get(c);

            // The refund is the smallest of the two: what the debtor owes, what the creditor is owed
            long amount = Math.min(-left.get(debtor.getIdPerson()), left.get(creditor.getIdPerson()));

            refunds.add(new Expense("Refund", amount / 100.0, LocalDate.now(),
                    ExpenseCategory.OTHER.getCode(), debtor, creditor));

            left.put(debtor.getIdPerson(), left.get(debtor.getIdPerson()) + amount);
            left.put(creditor.getIdPerson(), left.get(creditor.getIdPerson()) - amount);

            // A roomie with nothing left to pay or to receive is doneand we go to the next one
            if (left.get(debtor.getIdPerson()) == 0) d++;
            if (left.get(creditor.getIdPerson()) == 0) c++;
        }

        return refunds;
    }

    // Utility methods :

    // True if I am the debtor or the creditor of this debt
    private boolean concerns(Roomie me) {
        boolean iAmDebtor = debtor.getIdPerson().equals(me.getIdPerson());
        boolean iAmCreditor = creditor != null && creditor.getIdPerson().equals(me.getIdPerson());
        return iAmDebtor || iAmCreditor;
    }

    // Checks the description, required and 50 characters max. And removes the spaces around it
    private static String normalizeName(String description) {
        if (description == null || description.trim().isEmpty())
            throw new IllegalArgumentException("The description is required.");

        String trimmed = description.trim();

        if (trimmed.length() > 50)
            throw new IllegalArgumentException("The description cannot be longer than 50 characters.");

        return trimmed;
    }


    // Searches a roomie in the list with his id. If he is not there, he is not in my roomshare (400)
    private static Roomie findById(List<Roomie> members, Integer id) {
        for (Roomie roomie : members) {
            if (roomie.getIdPerson().equals(id))
                return roomie;
        }
        throw new IllegalArgumentException("A roomie of the list does not belong to your roomshare.");
    }

    // Checks the amount and gives it in cents
    private static long toCents(Double amount) {
        if (amount == null || amount.isNaN() || amount.isInfinite() || amount <= 0)
            throw new IllegalArgumentException("The amount must be greater than 0.");
        if (amount > 100000)
            throw new IllegalArgumentException("The amount is too high.");

        return Math.round(amount * 100);
    }

    // Transforms the text "2026-10-20" into a date. the format is the one of the input type="date"
    private static LocalDate parseDate(String date) {
        if (date == null || date.trim().isEmpty())
            throw new IllegalArgumentException("The date is required.");

        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("The date must look like 2026-10-20.");
        }
    }
}