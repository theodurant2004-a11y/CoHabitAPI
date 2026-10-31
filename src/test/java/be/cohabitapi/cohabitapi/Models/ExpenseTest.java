package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.ExpenseDAO;
import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.EXCEPTION.ForbiddenRoleException;
import be.cohabitapi.cohabitapi.EXCEPTION.NotInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

// These tests check the business logic of the expenses (split, balances, refunds)
// We use mocks for the DAOs and for the roomies, so no database is needed
@ExtendWith(MockitoExtension.class)
class ExpenseTest {

    @Mock
    private PersonDAO personDAO;
    @Mock
    private ExpenseDAO expenseDAO;
    @Captor
    private ArgumentCaptor<List<Expense>> savedDebts;

    // The 3 roomies of the roomshare. "me" is the roomie who is signed in
    private Roomie me;
    private Roomie second;
    private Roomie third;

    // This method runs before each test: it prepares a roomshare of 3 accepted roomies
    @BeforeEach
    void setUp() {
        me = roomie(1);
        second = roomie(2);
        third = roomie(3);

        // lenient() because some tests do not use all these answers
        lenient().when(me.getStatusMember()).thenReturn(MembershipStatus.ACCEPTED);
        lenient().when(me.getRoomshare()).thenReturn(mock(RoomShare.class));
        lenient().when(me.findRoomies(personDAO)).thenReturn(List.of(me, second, third));
        lenient().when(personDAO.findByEmail("me@mail.com")).thenReturn(me);
    }


    // ------------------------- Helpers --------------------------------------------------

    // Creates a fake roomie that only knows his id
    private Roomie roomie(int id) {
        Roomie roomie = mock(Roomie.class);
        lenient().when(roomie.getIdPerson()).thenReturn(id);
        return roomie;
    }

    // Calls addExpense for the roomie "me" with the values we give
    private List<Expense> add(String description, Double amount, String date, Integer paidById,
                              String category, List<Integer> sharedWithIds) {
        return Expense.addExpense("me@mail.com", description, amount, date, paidById, category,
                sharedWithIds, personDAO, expenseDAO);
    }

    // A debt of "sum" euros: the debtor owes it to the creditor
    private Expense debt(double sum, Roomie debtor, Roomie creditor) {
        return new Expense("Groceries", sum, LocalDate.of(2026, 10, 20), "food", debtor, creditor);
    }

    // There is no setter to settle an expense, so we change the attribute with reflection (only for the tests)
    private void settle(Expense expense) throws Exception {
        Field field = Expense.class.getDeclaredField("settled");
        field.setAccessible(true);
        field.set(expense, true);
    }


    // ------------------------- Constructor --------------------------------------------------

    @Test
    void constructor_validValues_createsAPendingDebt() {
        Expense expense = new Expense("  Groceries ", 15.5, LocalDate.of(2026, 10, 20), "food", second, me);

        assertEquals("Groceries", expense.getName());      // the spaces are removed
        assertEquals(15.5, expense.getSum(), 0.001);
        assertEquals(1550, expense.getSumInCents());
        assertEquals("food", expense.getType());
        assertEquals(ExpenseCategory.FOOD, expense.getCategory());
        assertFalse(expense.isSettled());                    // a new expense is always pending
        assertSame(second, expense.getDebtor());
        assertSame(me, expense.getCreditor());
    }

    @Test
    void constructor_blankName_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Expense("   ", 10, LocalDate.now(), "food", second, me));
    }

    @Test
    void constructor_nameOf51Characters_throwsException() {
        String tooLong = "a".repeat(51);

        assertThrows(IllegalArgumentException.class,
                () -> new Expense(tooLong, 10, LocalDate.now(), "food", second, me));
    }

    @Test
    void constructor_nameOf50Characters_isAccepted() {
        String name = "a".repeat(50);

        assertEquals(name, new Expense(name, 10, LocalDate.now(), "food", second, me).getName());
    }

    @Test
    void constructor_sumZeroOrNegative_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Expense("Groceries", 0, LocalDate.now(), "food", second, me));
        assertThrows(IllegalArgumentException.class,
                () -> new Expense("Groceries", -5, LocalDate.now(), "food", second, me));
    }

    @Test
    void constructor_nullDate_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Expense("Groceries", 10, null, "food", second, me));
    }

    @Test
    void constructor_unknownCategory_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Expense("Groceries", 10, LocalDate.now(), "unknown", second, me));
    }

    @Test
    void constructor_nullDebtorOrCreditor_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Expense("Groceries", 10, LocalDate.now(), "food", null, me));
        assertThrows(IllegalArgumentException.class,
                () -> new Expense("Groceries", 10, LocalDate.now(), "food", second, null));
    }


    // ------------------------- addExpense : the split --------------------------------------------------

    @Test
    void addExpense_sharedByThree_createsTwoDebtsOfTheSameSum() {
        // I paid 45 euros for the 3 of us: the 2 others owe me 15 each
        List<Expense> debts = add("Groceries", 45.0, "2026-10-20", 1, "food", List.of(1, 2, 3));

        assertEquals(2, debts.size());
        assertEquals(15.0, debts.get(0).getSum(), 0.001);
        assertEquals(15.0, debts.get(1).getSum(), 0.001);
        assertSame(second, debts.get(0).getDebtor());
        assertSame(third, debts.get(1).getDebtor());
        assertSame(me, debts.get(0).getCreditor());
        assertSame(me, debts.get(1).getCreditor());
        assertEquals(LocalDate.of(2026, 10, 20), debts.get(0).getCreationDate());
    }

    @Test
    void addExpense_savesAllTheDebtsInOneCall() {
        add("Groceries", 45.0, "2026-10-20", 1, "food", List.of(1, 2, 3));

        verify(expenseDAO, times(1)).createAll(savedDebts.capture());
        assertEquals(2, savedDebts.getValue().size());
    }

    @Test
    void addExpense_remainderInCents_isGivenToTheFirstRoomiesAndTheTotalStaysExact() {
        // 10 euros between 3 = 3.34 + 3.33 + 3.33. The third roomie paid, so only the 2 first ones owe money
        List<Expense> debts = add("Pizza", 10.0, "2026-10-20", 3, "food", List.of(1, 2, 3));

        assertEquals(2, debts.size());
        assertEquals(334, debts.get(0).getSumInCents());
        assertEquals(333, debts.get(1).getSumInCents());
    }

    @Test
    void addExpense_duplicateIds_areCountedOnce() {
        // the id 2 is sent twice, but the split is still between 2 roomies: 10 euros => 5 each
        List<Expense> debts = add("Taxi", 10.0, "2026-10-20", 1, "other", List.of(1, 2, 2));

        assertEquals(1, debts.size());
        assertEquals(5.0, debts.get(0).getSum(), 0.001);
    }

    @Test
    void addExpense_someoneElsePaid_theDebtGoesToThePayer() {
        // the roomie 2 paid for me and the roomie 3
        List<Expense> debts = add("Cinema", 20.0, "2026-10-20", 2, "leisure", List.of(1, 2, 3));

        assertEquals(2, debts.size());
        assertSame(second, debts.get(0).getCreditor());
        assertSame(me, debts.get(0).getDebtor());
        assertSame(third, debts.get(1).getDebtor());
    }


    // ------------------------- addExpense : what the user sent is not valid --------------------------------------------------

    @Test
    void addExpense_onlyThePayerInTheSplit_throwsExceptionAndSavesNothing() {
        assertThrows(IllegalArgumentException.class,
                () -> add("Groceries", 45.0, "2026-10-20", 1, "food", List.of(1)));

        verify(expenseDAO, never()).createAll(anyList());
    }

    @Test
    void addExpense_payerNotInTheRoomshare_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> add("Groceries", 45.0, "2026-10-20", 99, "food", List.of(1, 2)));

        verify(expenseDAO, never()).createAll(anyList());
    }

    @Test
    void addExpense_sharerNotInTheRoomshare_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> add("Groceries", 45.0, "2026-10-20", 1, "food", List.of(1, 99)));

        verify(expenseDAO, never()).createAll(anyList());
    }

    @Test
    void addExpense_noPayer_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> add("Groceries", 45.0, "2026-10-20", null, "food", List.of(1, 2)));
    }

    @Test
    void addExpense_emptyOrNullSplit_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> add("Groceries", 45.0, "2026-10-20", 1, "food", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> add("Groceries", 45.0, "2026-10-20", 1, "food", null));
    }

    @Test
    void addExpense_invalidAmount_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> add("Groceries", null, "2026-10-20", 1, "food", List.of(1, 2)));
        assertThrows(IllegalArgumentException.class,
                () -> add("Groceries", 0.0, "2026-10-20", 1, "food", List.of(1, 2)));
        assertThrows(IllegalArgumentException.class,
                () -> add("Groceries", -3.0, "2026-10-20", 1, "food", List.of(1, 2)));
        assertThrows(IllegalArgumentException.class,
                () -> add("Groceries", 100001.0, "2026-10-20", 1, "food", List.of(1, 2)));
    }

    @Test
    void addExpense_invalidDescription_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> add(null, 45.0, "2026-10-20", 1, "food", List.of(1, 2)));
        assertThrows(IllegalArgumentException.class,
                () -> add("  ", 45.0, "2026-10-20", 1, "food", List.of(1, 2)));
        assertThrows(IllegalArgumentException.class,
                () -> add("a".repeat(51), 45.0, "2026-10-20", 1, "food", List.of(1, 2)));
    }

    @Test
    void addExpense_invalidDate_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> add("Groceries", 45.0, null, 1, "food", List.of(1, 2)));
        assertThrows(IllegalArgumentException.class,
                () -> add("Groceries", 45.0, "20/10/2026", 1, "food", List.of(1, 2)));
    }

    @Test
    void addExpense_invalidCategory_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> add("Groceries", 45.0, "2026-10-20", 1, "unknown", List.of(1, 2)));
        assertThrows(IllegalArgumentException.class,
                () -> add("Groceries", 45.0, "2026-10-20", 1, null, List.of(1, 2)));
    }


    // ------------------------- addExpense : who is calling --------------------------------------------------

    @Test
    void addExpense_unknownEmail_throwsUserNotFoundException() {
        assertThrows(UserNotFoundException.class,
                () -> Expense.addExpense("nobody@mail.com", "Groceries", 45.0, "2026-10-20", 1, "food",
                        List.of(1, 2), personDAO, expenseDAO));

        verify(expenseDAO, never()).createAll(anyList());
    }

    @Test
    void addExpense_anOwner_throwsForbiddenRoleException() {
        // an Owner is a Person but not a Roomie
        Owner owner = mock(Owner.class);
        when(personDAO.findByEmail("owner@mail.com")).thenReturn(owner);

        assertThrows(ForbiddenRoleException.class,
                () -> Expense.addExpense("owner@mail.com", "Groceries", 45.0, "2026-10-20", 1, "food",
                        List.of(1, 2), personDAO, expenseDAO));
    }

    @Test
    void addExpense_roomieNotAcceptedYet_throwsNotInRoomshareException() {
        when(me.getStatusMember()).thenReturn(MembershipStatus.PENDING);

        assertThrows(NotInRoomshareException.class,
                () -> add("Groceries", 45.0, "2026-10-20", 1, "food", List.of(1, 2)));

        verify(expenseDAO, never()).createAll(anyList());
    }

    @Test
    void addExpense_roomieWithoutRoomshare_throwsNotInRoomshareException() {
        when(me.getRoomshare()).thenReturn(null);

        assertThrows(NotInRoomshareException.class,
                () -> add("Groceries", 45.0, "2026-10-20", 1, "food", List.of(1, 2)));
    }


    // ------------------------- Totals --------------------------------------------------

    @Test
    void keepMine_keepsOnlyTheDebtsThatConcernMe() {
        Expense iOwe = debt(10, me, second);
        Expense iAmOwed = debt(10, third, me);
        Expense notMine = debt(10, second, third);

        List<Expense> mine = Expense.keepMine(me, List.of(iOwe, iAmOwed, notMine));

        assertEquals(2, mine.size());
        assertTrue(mine.contains(iOwe));
        assertTrue(mine.contains(iAmOwed));
        assertFalse(mine.contains(notMine));
    }

    @Test
    void calculateToPay_addsWhatIOwe() {
        List<Expense> all = List.of(debt(10.5, me, second), debt(4.25, me, third), debt(7, second, me));

        assertEquals(14.75, Expense.calculateToPay(me, all), 0.001);
    }

    @Test
    void calculateToReceive_addsWhatTheOthersOweMe() {
        List<Expense> all = List.of(debt(10.5, me, second), debt(4.25, second, me), debt(7, third, me));

        assertEquals(11.25, Expense.calculateToReceive(me, all), 0.001);
    }

    @Test
    void totals_ignoreTheSettledDebts() throws Exception {
        Expense settled = debt(10, me, second);
        settle(settled);
        List<Expense> all = List.of(settled, debt(5, me, third), debt(8, second, me));

        assertEquals(5.0, Expense.calculateToPay(me, all), 0.001);
        assertEquals(8.0, Expense.calculateToReceive(me, all), 0.001);
    }

    @Test
    void totals_noDebt_giveZero() {
        assertEquals(0.0, Expense.calculateToPay(me, List.of()), 0.001);
        assertEquals(0.0, Expense.calculateToReceive(me, List.of()), 0.001);
    }


    // ------------------------- Balances --------------------------------------------------

    @Test
    void calculateBalances_payerIsOwedAndTheOthersOwe() {
        // I paid 30 for the 3 of us: the 2 others owe me 10 each
        List<Expense> all = List.of(debt(10, second, me), debt(10, third, me));

        Map<Integer, Long> balances = Expense.calculateBalances(List.of(me, second, third), all);

        assertEquals(2000L, balances.get(1));
        assertEquals(-1000L, balances.get(2));
        assertEquals(-1000L, balances.get(3));
    }

    @Test
    void calculateBalances_theTotalIsAlwaysZero() {
        List<Expense> all = List.of(debt(10.33, second, me), debt(3.34, third, me), debt(7.1, me, second));

        Map<Integer, Long> balances = Expense.calculateBalances(List.of(me, second, third), all);

        long total = 0;
        for (long cents : balances.values()) {
            total += cents;
        }
        assertEquals(0L, total);
    }

    @Test
    void calculateBalances_ignoresTheSettledDebts() throws Exception {
        Expense settled = debt(10, second, me);
        settle(settled);

        Map<Integer, Long> balances = Expense.calculateBalances(List.of(me, second, third), List.of(settled));

        assertEquals(0L, balances.get(1));
        assertEquals(0L, balances.get(2));
    }

    @Test
    void calculateBalances_everybodyStartsAtZero() {
        Map<Integer, Long> balances = Expense.calculateBalances(List.of(me, second, third), List.of());

        assertEquals(3, balances.size());
        assertEquals(0L, balances.get(1));
    }


    // ------------------------- Refunds --------------------------------------------------

    @Test
    void suggestRefunds_eachDebtorPaysTheCreditor() {
        // me +20, second -10, third -10
        Map<Integer, Long> balances = Map.of(1, 2000L, 2, -1000L, 3, -1000L);

        List<Expense> refunds = Expense.suggestRefunds(List.of(me, second, third), balances);

        assertEquals(2, refunds.size());
        for (Expense refund : refunds) {
            assertSame(me, refund.getCreditor());
            assertEquals(10.0, refund.getSum(), 0.001);
        }
    }

    @Test
    void suggestRefunds_theBiggestDebtorPaysFirst() {
        // me +20, second -15, third -5
        Map<Integer, Long> balances = Map.of(1, 2000L, 2, -1500L, 3, -500L);

        List<Expense> refunds = Expense.suggestRefunds(List.of(me, second, third), balances);

        assertEquals(2, refunds.size());
        assertSame(second, refunds.get(0).getDebtor());
        assertEquals(15.0, refunds.get(0).getSum(), 0.001);
        assertSame(third, refunds.get(1).getDebtor());
        assertEquals(5.0, refunds.get(1).getSum(), 0.001);
    }

    @Test
    void suggestRefunds_oneDebtorPaysSeveralCreditors() {
        // me +5, second +10, third -15: the third roomie pays the 2 others
        Map<Integer, Long> balances = Map.of(1, 500L, 2, 1000L, 3, -1500L);

        List<Expense> refunds = Expense.suggestRefunds(List.of(me, second, third), balances);

        assertEquals(2, refunds.size());
        assertSame(second, refunds.get(0).getCreditor());   // the one who is owed the most is paid first
        assertEquals(10.0, refunds.get(0).getSum(), 0.001);
        assertSame(me, refunds.get(1).getCreditor());
        assertEquals(5.0, refunds.get(1).getSum(), 0.001);
    }

    @Test
    void suggestRefunds_everythingIsBalanced_noRefund() {
        Map<Integer, Long> balances = Map.of(1, 0L, 2, 0L, 3, 0L);

        assertTrue(Expense.suggestRefunds(List.of(me, second, third), balances).isEmpty());
    }
}