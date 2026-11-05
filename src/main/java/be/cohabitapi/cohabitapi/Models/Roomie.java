package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.ExpenseDAO;
import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;

import be.cohabitapi.cohabitapi.EXCEPTION.*;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "Roomie")
@PrimaryKeyJoinColumn(name = "user_id", referencedColumnName = "user_id")
public class Roomie extends Person {

    // The colors given to the roomies, in the order of the list
    private static final String[] COLORS = {"#c17f2e", "#2e8b57", "#6a5acd", "#d1495b", "#00798c"};

    @ManyToOne
    @JoinColumn(name = "roomshare_id")
    private RoomShare roomshare;

    // A new roomie has no roomshare, so he starts with NONE
    @Convert(converter = MembershipStatus.DbConverter.class)
    @Column(name = "roomie_status", nullable = false)
    private MembershipStatus statusMember = MembershipStatus.NONE;

    // The tasks of this roomie (association with Task in the class diagram)
    // mappedBy: the Task_Roomie table is managed by the "roomies" attribute of Task, this side only reads it
    // No cascade: it is an association, not a composition (a task does not live inside a roomie)
    @ManyToMany(mappedBy = "roomies")
    private Set<Task> tasks = new HashSet<>();

    // The expenses this roomie has to pay
    @OneToMany(mappedBy = "debtor")
    private List<Expense> expensesToPay = new ArrayList<>();

    public RoomShare getRoomshare() {
        return roomshare;
    }

    // Not public because nobody outside the model must use this setter
    void setRoomshare(RoomShare roomshare) {
        this.roomshare = roomshare;
    }

    // No setter for the status: it only changes through the methods
    public MembershipStatus getStatusMember() {
        return statusMember;
    }

    // Read only: a task is assigned with Task.assignRoomie (which checks the rules), never from here
    public Set<Task> getTasks(){
        return Collections.unmodifiableSet(tasks);
    }

    public List<Expense> getExpensesToPay() {
        return expensesToPay;
    }

    protected Roomie() {
    }

    public Roomie(String lastName, String firstName, String email, String password) {
        super(lastName, firstName, email, password);
    }

    // Same as the constructor above but with the possibility to join directly a room share
    public Roomie(String lastName, String firstName, String email, String password, RoomShare roomshare) {
        super(lastName, firstName, email, password);
        roomshare.addRoomie(this); // Add the roomie to the room share list but add the roomshare to the roomie too
        // He joins directly, so he is an accepted member
        this.statusMember = MembershipStatus.ACCEPTED;
    }

    //  ROLES

    // A roomie can use the expenses only if the owner accepted him
    public void checkIsAccepted() {
        if (roomshare == null || statusMember != MembershipStatus.ACCEPTED)
            throw new NotInRoomshareException();
    }

    //  JOIN A ROOMSHARE

    // The roomie asks to join a roomshare and waits for the owner's answer but a roomie can have only one roomshare
    // The roomies list of the roomshare is not used on purpose: it is lazy and the roomshare
    // was loaded by another EntityManager, so touching the list would throw an Exception
    public void sendJoinRequest(RoomShare roomshare) {
        if (roomshare == null)
            throw new IllegalArgumentException("The roomshare cannot be null");

        // Already in a roomshare, or a request is already waiting: refuse and the API turns it into a 409
        if (this.roomshare != null || this.statusMember != MembershipStatus.NONE)
            throw new AlreadyInRoomshareException();

        this.roomshare = roomshare;
        this.statusMember = MembershipStatus.PENDING;
    }

    // The whole "join a roomshare" use case
    public static Roomie requestToJoin(String email, String invitationKey,
                                       PersonDAO personDAO, RoomShareDAO roomShareDAO) {
        String cleanKey = normalizeInvitationKey(invitationKey);
        String cleanEmail = Person.normalizeEmail(email);

        Person person = Person.findByEmail(cleanEmail, personDAO);

        // Nobody has this email (the API answers 401)
        if (person == null)
            throw new UserNotFoundException();

        // An owner is a Person but not a Roomie: only roomies can join a roomshare (403)
        if (!(person instanceof Roomie))
            throw new ForbiddenRoleException();

        Roomie roomie = (Roomie) person;

        RoomShare found = roomShareDAO.findByInvitationKey(cleanKey);

        // No roomshare for this key (404)
        if (found == null)
            throw new InvalidInvitationKeyException();

        roomie.sendJoinRequest(found);

        personDAO.update(roomie);

        return roomie;
    }

    // Cleans the invitation key and checks that it is valid
    private static String normalizeInvitationKey(String key) {
        if (key == null)
            throw new IllegalArgumentException("The invitation key is required.");

        String clean = key.trim().toUpperCase(Locale.ROOT);

        // 6 characters, only letters A-Z and digits 0-9
        if (!clean.matches("[A-Z0-9]{6}"))
            throw new IllegalArgumentException("The invitation key must contain exactly 6 letters or digits.");

        return clean;
    }

    // TASKS

    // True if this roomie is an accepted member of this roomshare
    // The roomie knows his status and his roomshare, so he answers himself (Task does not look inside him)
    // We compare the ids: the two roomshares can be two different instances of the same roomshare
    public boolean isAcceptedMemberOf(RoomShare roomshare) {
        return roomshare != null
                && this.statusMember == MembershipStatus.ACCEPTED
                && this.roomshare != null
                && this.roomshare.getId().equals(roomshare.getId());
    }

    // Finds a roomie by his email and checks that he is an accepted member of a roomshare
    // Same idea as Owner.checkRightOwner, used by every task use case (add, list, update, delete)
    public static Roomie checkAcceptedRoomie(String email, PersonDAO personDAO) {
        // Same cleaning as the login (400 if empty)
        String cleanEmail = Person.normalizeEmail(email);
        Person person = Person.findByEmail(cleanEmail, personDAO);

        // Nobody has this email (the API answers 401)
        if (person == null)
            throw new UserNotFoundException();

        // An owner cannot manage tasks (403)
        if (!(person instanceof Roomie))
            throw new ForbiddenRoleException();

        Roomie roomie = (Roomie) person;

        // No roomshare yet, or the join request is still pending (403)
        if (!roomie.isAcceptedMemberOf(roomie.getRoomshare()))
            throw new NotInRoomshareException();

        return roomie;
    }

    //  EXPENSES

    // Finds the accepted roomie behind this email, with his roomshare loaded
    public static Roomie findAcceptedMember(String email, PersonDAO personDAO, RoomShareDAO roomShareDAO) {
        String cleanEmail = normalizeEmail(email);
        Person person = Person.findByEmail(cleanEmail, personDAO);

        if (person == null)
            throw new UserNotFoundException();

        if (!(person instanceof Roomie))
            throw new ForbiddenRoleException();

        Roomie roomie = (Roomie) person;
        roomie.checkIsAccepted();

        return roomie.loadRoomshare(roomShareDAO);
    }

    // Gives back this roomie but with his roomshare loaded: its roomies and all their expenses
    public Roomie loadRoomshare(RoomShareDAO dao) {
        RoomShare loaded = dao.findWithRoomies(roomshare.getId());
        return loaded.findRoomie(getIdPerson());
    }

    // Adds an expense to my roomshare: one debt is created for each roomie of the split, except the payer
    public List<Expense> addExpense(String description, Double amount, String date, String category,
                                    Integer paidById, List<Integer> sharedWithIds, ExpenseDAO expenseDAO) {
        checkIsAccepted();

        long totalCents = toCents(amount);
        LocalDate cleanDate = parseDate(date);

        // findRoomie refuses an id that is not in my roomshare
        Roomie payer = roomshare.findRoomie(paidById);
        List<Roomie> ordered = findSharers(sharedWithIds);

        // Split in cents
        long baseCents = totalCents / ordered.size(); // each roomie's basic share
        long remainder = totalCents % ordered.size(); // cents left after the division

        List<Expense> debts = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            // The first roomies pay 1 cent more, so the total is exact
            long shareCents = baseCents + (i < remainder ? 1 : 0);
            Roomie sharer = ordered.get(i);

            // The payer does not owe money to himself
            if (!sharer.equals(payer)) {
                debts.add(new Expense(description, shareCents / 100.0, cleanDate, category, sharer, payer));
            }
        }

        if (debts.isEmpty())
            throw new IllegalArgumentException("Choose at least one other roomie than the one who paid.");

        // Everything is saved in one transaction
        expenseDAO.createAll(debts);

        // The new debts go in the lists of the debtors and of the payer
        for (Expense debt : debts) {
            debt.getDebtor().addExpenseToPay(debt);
            payer.addExpenseToReceive(debt);
        }

        return debts;
    }

    // The roomies of the split, without duplicates and in the order of the roomshare (so the split is always the same)
    private List<Roomie> findSharers(List<Integer> sharedWithIds) {
        if (sharedWithIds == null || sharedWithIds.isEmpty())
            throw new IllegalArgumentException("Choose at least one roomie to split with.");

        Set<Roomie> sharers = new HashSet<>();
        for (Integer id : sharedWithIds) {
            sharers.add(roomshare.findRoomie(id));
        }

        List<Roomie> ordered = roomshare.getAcceptedRoomies();
        ordered.retainAll(sharers);
        return ordered;
    }

    public void addExpenseToPay(Expense expense) {
        if (expense == null)
            throw new IllegalArgumentException("The expense cannot be null");
        expensesToPay.add(expense);
    }

    // All the expenses that concern me, the newest first
    public List<Expense> findMyExpenses() {
        List<Expense> mine = new ArrayList<>(expensesToPay);
        mine.addAll(getExpensesToReceive());

        mine.sort((a, b) -> {
            int byDate = b.getCreationDate().compareTo(a.getCreationDate());
            if (byDate != 0)
                return byDate;
            return b.getId().compareTo(a.getId());
        });
        return mine;
    }

    // What I have to pay
    public double calculateToPay() {
        return sumInCents(expensesToPay) / 100.0;
    }

    // Positive if I am owed money, negative if I owe money
    @Override
    public double calculateBalance() {
        return calculateBalanceInCents() / 100.0;
    }

    // What I receive minus what I pay, in cents
    public long calculateBalanceInCents() {
        return sumInCents(getExpensesToReceive()) - sumInCents(expensesToPay);
    }

    // The width of my bar in percent: the biggest balance of the roomshare gets 100%
    public int calculatePercent() {
        long biggest = 0;
        for (Roomie roomie : roomshare.getAcceptedRoomies()) {
            biggest = Math.max(biggest, Math.abs(roomie.calculateBalanceInCents()));
        }

        if (biggest == 0)
            return 0;

        return (int) Math.round(Math.abs(calculateBalanceInCents()) * 100.0 / biggest);
    }

    // My color depends on my position in the list of my roomshare
    public String chooseColor() {
        int position = roomshare.getAcceptedRoomies().indexOf(this);

        // indexOf gives -1 if I am not in the list, so I get the first color
        if (position < 0)
            position = 0;

        // It goes back to the start when there are more roomies than colors
        return COLORS[position % COLORS.length];
    }

    // The roomie who owes the most pays the roomie who is owed the most
    public List<Refund> suggestRefunds() {
        List<Roomie> members = roomshare.getAcceptedRoomies();

        // Each roomie with his balance
        Map<Roomie, Long> left = new HashMap<>();
        for (Roomie member : members) {
            left.put(member, member.calculateBalanceInCents());
        }

        List<Refund> refunds = new ArrayList<>();

        while (true) {
            // Find the biggest debtor and the biggest creditor
            Roomie debtor = null;
            Roomie creditor = null;
            for (Roomie member : members) {
                long cents = left.get(member);
                if (cents < 0 && (debtor == null || cents < left.get(debtor)))
                    debtor = member;
                if (cents > 0 && (creditor == null || cents > left.get(creditor)))
                    creditor = member;
            }

            // Nobody owes anything anymore
            if (debtor == null || creditor == null)
                break;

            // The refund is the smallest of the two amounts
            long amount = Math.min(-left.get(debtor), left.get(creditor));
            refunds.add(new Refund(debtor, creditor, amount / 100.0));

            left.put(debtor, left.get(debtor) + amount);
            left.put(creditor, left.get(creditor) - amount);
        }

        return refunds;
    }

    // Checks the amount and gives it in cents
    private long toCents(Double amount) {
        if (amount == null || amount.isNaN() || amount.isInfinite() || amount <= 0)
            throw new IllegalArgumentException("The amount must be greater than 0.");
        if (amount > 100000)
            throw new IllegalArgumentException("The amount is too high.");

        return Math.round(amount * 100);
    }

    // Transforms the text "2026-10-20" into a date
    private LocalDate parseDate(String date) {
        if (date == null || date.trim().isEmpty())
            throw new IllegalArgumentException("The date is required.");

        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("The date must look like 2026-10-20.");
        }
    }
}
