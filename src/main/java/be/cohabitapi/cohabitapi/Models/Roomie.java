package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.ExpenseDAO;
import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.EXCEPTION.AlreadyInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.ForbiddenRoleException;
import be.cohabitapi.cohabitapi.EXCEPTION.InvalidInvitationKeyException;
import be.cohabitapi.cohabitapi.EXCEPTION.NotInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "Roomie")
@PrimaryKeyJoinColumn(name = "user_id", referencedColumnName = "user_id")
public class Roomie extends Person {

    // The colors given to the roomies, in the order of the list
    private static final String[] COLORS = {"#c17f2e", "#2e8b57", "#6a5acd", "#d1495b", "#00798c"};

    // ------------------------- Attributes and JPA Annotations --------------------------------------------------

    @ManyToOne
    @JoinColumn(name = "roomshare_id")
    private RoomShare roomshare;

    // A new roomie has no roomshare, so he starts with NONE
    @Convert(converter = MembershipStatus.DbConverter.class)
    @Column(name = "roomie_status", nullable = false)
    private MembershipStatus statusMember = MembershipStatus.NONE;

    // The expenses this roomie has to pay
    @OneToMany(mappedBy = "debtor")
    private List<Expense> expensesToPay = new ArrayList<>();

    // ------------------------- Getter and Setter --------------------------------------------------

    public RoomShare getRoomshare() {
        return roomshare;
    }

    // Not public because nobody outside the model must use this setter
    void setRoomshare(RoomShare roomshare){
        Objects.requireNonNull(roomshare);
        this.roomshare = roomshare;
    }

    // No setter for the status: it only changes through the methods
    public MembershipStatus getStatusMember() {
        return statusMember;
    }

    public List<Expense> getExpensesToPay() {
        return expensesToPay;
    }

    // ------------------------- Constructors --------------------------------------------------

    protected Roomie() {
    }

    public Roomie(String lastName, String firstName, String email, String password) {
        super(lastName, firstName, email, password);
    }

    // Same as the constructor above but with the possibility to join directly a room share
    public Roomie(String lastName, String firstName, String email, String password, RoomShare roomShare) {
        super(lastName, firstName, email, password);
        roomShare.addRoomie(this);
        this.statusMember = MembershipStatus.ACCEPTED;
    }

    // ------------------------- Utility methods --------------------------------------------------

    // A roomie can use the expenses only if the owner accepted him
    public void checkIsAccepted() {
        if (roomshare == null || statusMember != MembershipStatus.ACCEPTED)
            throw new NotInRoomshareException();
    }

    public void sendJoinRequest(RoomShare roomshare) {
        if (roomshare == null)
            throw new IllegalArgumentException("The roomshare cannot be null");

        if (this.roomshare != null || this.statusMember != MembershipStatus.NONE)
            throw new AlreadyInRoomshareException();

        this.roomshare = roomshare;
        this.statusMember = MembershipStatus.PENDING;
    }

    public static Roomie requestToJoin(String email, String invitationKey,
                                       PersonDAO personDAO, RoomShareDAO roomShareDAO) {
        String cleanKey = normalizeInvitationKey(invitationKey);
        String cleanEmail = Person.normalizeEmail(email);

        Person person = Person.findByEmail(cleanEmail, personDAO);

        if (person == null)
            throw new UserNotFoundException();

        if (!(person instanceof Roomie))
            throw new ForbiddenRoleException();

        Roomie roomie = (Roomie) person;
        RoomShare found = roomShareDAO.findByInvitationKey(cleanKey);

        if (found == null)
            throw new InvalidInvitationKeyException();

        roomie.sendJoinRequest(found);
        personDAO.update(roomie);

        return roomie;
    }

    private static String normalizeInvitationKey(String key) {
        if (key == null)
            throw new IllegalArgumentException("The invitation key is required.");

        String clean = key.trim().toUpperCase(Locale.ROOT);

        if (!clean.matches("[A-Z0-9]{6}"))
            throw new IllegalArgumentException("The invitation key must contain exactly 6 letters or digits.");

        return clean;
    }

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

    public Roomie loadRoomshare(RoomShareDAO dao) {
        RoomShare loaded = dao.findWithRoomies(roomshare.getId());
        return loaded.findRoomie(getIdPerson());
    }

    public List<Expense> addExpense(String description, Double amount, String date, String category,
                                    Integer paidById, List<Integer> sharedWithIds, ExpenseDAO expenseDAO) {
        checkIsAccepted();

        long totalCents = toCents(amount);
        LocalDate cleanDate = parseDate(date);

        Roomie payer = roomshare.findRoomie(paidById);
        List<Roomie> ordered = findSharers(sharedWithIds);

        long baseCents = totalCents / ordered.size();
        long remainder = totalCents % ordered.size();

        List<Expense> debts = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            long shareCents = baseCents + (i < remainder ? 1 : 0);
            Roomie sharer = ordered.get(i);

            if (!sharer.equals(payer)) {
                debts.add(new Expense(description, shareCents / 100.0, cleanDate, category, sharer, payer));
            }
        }

        if (debts.isEmpty())
            throw new IllegalArgumentException("Choose at least one other roomie than the one who paid.");

        expenseDAO.createAll(debts);

        for (Expense debt : debts) {
            debt.getDebtor().addExpenseToPay(debt);
            payer.addExpenseToReceive(debt);
        }

        return debts;
    }

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

    public double calculateToPay() {
        return sumInCents(expensesToPay) / 100.0;
    }

    public static Roomie checkRightRoomie(PersonDAO dao, String roomieEmail) {
        String cleanEmail = normalizeEmail(roomieEmail);
        Person roomie = Person.findByEmail(cleanEmail, dao);

        if(roomie == null)
            throw new UserNotFoundException();

        if(!(roomie instanceof Roomie))
            throw new ForbiddenRoleException();

        return (Roomie)roomie;
    }

    public static Roomie checkRightRoomie(PersonDAO dao, int userId) {
        Person roomie = Person.findById(dao, userId);

        if(!(roomie instanceof Roomie))
            throw new ForbiddenRoleException();

        return (Roomie)roomie;
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

    public int calculatePercent() {
        long biggest = 0;
        for (Roomie roomie : roomshare.getAcceptedRoomies()) {
            biggest = Math.max(biggest, Math.abs(roomie.calculateBalanceInCents()));
        }

        if (biggest == 0)
            return 0;

        return (int) Math.round(Math.abs(calculateBalanceInCents()) * 100.0 / biggest);
    }

    public String chooseColor() {
        int position = roomshare.getAcceptedRoomies().indexOf(this);

        if (position < 0)
            position = 0;

        return COLORS[position % COLORS.length];
    }

    public List<Refund> suggestRefunds() {
        List<Roomie> members = roomshare.getAcceptedRoomies();

        Map<Roomie, Long> left = new HashMap<>();
        for (Roomie member : members) {
            left.put(member, member.calculateBalanceInCents());
        }

        List<Refund> refunds = new ArrayList<>();

        while (true) {
            Roomie debtor = null;
            Roomie creditor = null;
            for (Roomie member : members) {
                long cents = left.get(member);
                if (cents < 0 && (debtor == null || cents < left.get(debtor)))
                    debtor = member;
                if (cents > 0 && (creditor == null || cents > left.get(creditor)))
                    creditor = member;
            }

            if (debtor == null || creditor == null)
                break;

            long amount = Math.min(-left.get(debtor), left.get(creditor));
            refunds.add(new Refund(debtor, creditor, amount / 100.0));

            left.put(debtor, left.get(debtor) + amount);
            left.put(creditor, left.get(creditor) - amount);
        }

        return refunds;
    }

    private long toCents(Double amount) {
        if (amount == null || amount.isNaN() || amount.isInfinite() || amount <= 0)
            throw new IllegalArgumentException("The amount must be greater than 0.");
        if (amount > 100000)
            throw new IllegalArgumentException("The amount is too high.");

        return Math.round(amount * 100);
    }

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