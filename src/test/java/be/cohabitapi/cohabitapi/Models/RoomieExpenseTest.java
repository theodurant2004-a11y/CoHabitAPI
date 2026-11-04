package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.ExpenseDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.EXCEPTION.ForbiddenRoleException;
import be.cohabitapi.cohabitapi.EXCEPTION.NotInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.UnauthorizedUserException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

// These tests check the business logic of the expenses (split, balances, refunds)
// The roomshare, the roomies and the expenses are real objects, only the DAOs are mocks
@ExtendWith(MockitoExtension.class)
// PER_CLASS: the same test object is used for all the tests, so @BeforeAll does not need to be static
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoomieExpenseTest {

    private static final String PASSWORD = "Test1234!";

    @Mock
    private ExpenseDAO expenseDAO;
    @Mock
    private RoomShareDAO roomShareDAO;
    @Captor
    private ArgumentCaptor<List<Expense>> savedDebts;

    private Owner owner;
    private RoomShare roomshare;

    // The 3 roomies of the roomshare, in this order
    private Roomie me;
    private Roomie second;
    private Roomie third;

    // Runs one time: hashing a password is slow, so we create the roomies only once
    @BeforeAll
    void createRoomshare() {
        owner = new Owner("Doe", "Alice", "owner@mail.com", PASSWORD);
        roomshare = new RoomShare("ABC123", "Belleville", owner);

        // This constructor adds the roomie to the roomshare and makes him ACCEPTED
        me = new Roomie("Roomie", "Test", "me@mail.com", PASSWORD, roomshare);
        second = new Roomie("Lfarh", "Nora", "second@mail.com", PASSWORD, roomshare);
        third = new Roomie("Martin", "Alice", "third@mail.com", PASSWORD, roomshare);

        me.setIdPerson(1);
        second.setIdPerson(2);
        third.setIdPerson(3);
    }

    // Runs before each test: nobody has expenses at the start
    @BeforeEach
    void clearExpenses() {
        for (Roomie roomie : List.of(me, second, third)) {
            roomie.getExpensesToPay().clear();
            roomie.getExpensesToReceive().clear();
        }
        owner.getExpensesToReceive().clear();
    }


    // ------------------------- Helpers --------------------------------------------------

    // A debt of "sum" euros: the debtor owes it to the creditor. It is put in the two lists
    private Expense debt(double sum, Roomie debtor, Person creditor, LocalDate date) {
        Expense expense = new Expense("Groceries", sum, date, "food", debtor, creditor);
        debtor.addExpenseToPay(expense);
        creditor.addExpenseToReceive(expense);
        return expense;
    }

    private Expense debt(double sum, Roomie debtor, Person creditor) {
        return debt(sum, debtor, creditor, LocalDate.of(2026, 10, 20));
    }

    // A roomie who is not in the roomshare
    private Roomie outsider() {
        return new Roomie("Out", "Sider", "outsider@mail.com", PASSWORD);
    }

    // A roomie who sent a join request but is not accepted yet
    private Roomie pendingRoomie() {
        Roomie pending = new Roomie("Ding", "Pen", "pending@mail.com", PASSWORD);
        pending.sendJoinRequest(roomshare);
        return pending;
    }


    // ------------------------- Roles --------------------------------------------------

    @Test
    void toRoomie_aRoomie_returnsHimself() {
        assertSame(me, me.toRoomie());
    }

    @Test
    void toOwner_aRoomie_throwsUnauthorizedUserException() {
        assertThrows(UnauthorizedUserException.class, me::toOwner);
    }

    @Test
    void toOwner_anOwner_returnsHimself() {
        assertSame(owner, owner.toOwner());
    }

    @Test
    void toRoomie_anOwner_throwsForbiddenRoleException() {
        assertThrows(ForbiddenRoleException.class, owner::toRoomie);
    }

    @Test
    void checkIsAccepted_acceptedRoomie_doesNotThrow() {
        assertDoesNotThrow(me::checkIsAccepted);
    }

    @Test
    void checkIsAccepted_pendingRoomie_throwsNotInRoomshareException() {
        assertThrows(NotInRoomshareException.class, () -> pendingRoomie().checkIsAccepted());
    }

    @Test
    void checkIsAccepted_roomieWithoutRoomshare_throwsNotInRoomshareException() {
        assertThrows(NotInRoomshareException.class, () -> outsider().checkIsAccepted());
    }


    // ------------------------- loadRoomshare --------------------------------------------------

    @Test
    void loadRoomshare_givesBackMeInTheLoadedRoomshare() {
        // The DAO answers with the roomshare that is already complete
        when(roomShareDAO.findWithRoomies(any())).thenReturn(roomshare);

        assertSame(me, me.loadRoomshare(roomShareDAO));
    }


    // ------------------------- addExpense : the split --------------------------------------------------

    @Test
    void addExpense_sharedByThree_createsTwoDebtsOfTheSameSum() {
        // I paid 45 euros for the 3 of us: the 2 others owe me 15 each
        List<Expense> debts = me.addExpense("Groceries", 45.0, "2026-10-20", "food",
                List.of(me, second, third), expenseDAO);

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
        me.addExpense("Groceries", 45.0, "2026-10-20", "food", List.of(me, second, third), expenseDAO);

        verify(expenseDAO, times(1)).createAll(savedDebts.capture());
        assertEquals(2, savedDebts.getValue().size());
    }

    @Test
    void addExpense_putsTheDebtsInTheListsOfTheRoomies() {
        me.addExpense("Groceries", 45.0, "2026-10-20", "food", List.of(me, second, third), expenseDAO);

        // I have 2 expenses to receive and the others have 1 expense to pay each
        assertEquals(2, me.getExpensesToReceive().size());
        assertEquals(1, second.getExpensesToPay().size());
        assertEquals(1, third.getExpensesToPay().size());
        assertTrue(me.getExpensesToPay().isEmpty());
    }

    @Test
    void addExpense_remainderInCents_isGivenToTheFirstRoomiesAndTheTotalStaysExact() {
        // 10 euros between 3 = 3.34 + 3.33 + 3.33. The third roomie paid, so only the 2 first ones owe money
        List<Expense> debts = third.addExpense("Pizza", 10.0, "2026-10-20", "food",
                List.of(me, second, third), expenseDAO);

        assertEquals(2, debts.size());
        assertEquals(334, debts.get(0).getSumInCents());
        assertEquals(333, debts.get(1).getSumInCents());
    }

    @Test
    void addExpense_duplicateRoomies_areCountedOnce() {
        // "second" is in the list twice, but the split is still between 2 roomies: 10 euros => 5 each
        List<Expense> debts = me.addExpense("Taxi", 10.0, "2026-10-20", "other",
                List.of(me, second, second), expenseDAO);

        assertEquals(1, debts.size());
        assertEquals(5.0, debts.get(0).getSum(), 0.001);
    }

    @Test
    void addExpense_someoneElsePaid_theDebtGoesToThePayer() {
        // The second roomie paid for me and the third roomie
        List<Expense> debts = second.addExpense("Cinema", 20.0, "2026-10-20", "leisure",
                List.of(me, second, third), expenseDAO);

        assertEquals(2, debts.size());
        assertSame(second, debts.get(0).getCreditor());
        assertSame(me, debts.get(0).getDebtor());
        assertSame(second, debts.get(1).getCreditor());
        assertSame(third, debts.get(1).getDebtor());
    }


    // ------------------------- addExpense : what the user sent is not valid --------------------------------------------------

    @Test
    void addExpense_onlyThePayerInTheSplit_throwsExceptionAndSavesNothing() {
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 45.0, "2026-10-20", "food", List.of(me), expenseDAO));

        verify(expenseDAO, never()).createAll(anyList());
    }

    @Test
    void addExpense_sharerNotInTheRoomshare_throwsException() {
        Roomie stranger = outsider();

        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 45.0, "2026-10-20", "food", List.of(me, stranger), expenseDAO));

        verify(expenseDAO, never()).createAll(anyList());
    }

    @Test
    void addExpense_emptyOrNullSplit_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 45.0, "2026-10-20", "food", List.of(), expenseDAO));
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 45.0, "2026-10-20", "food", null, expenseDAO));
        // Arrays.asList accepts null, List.of does not
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 45.0, "2026-10-20", "food", Arrays.asList(me, null), expenseDAO));
    }

    @Test
    void addExpense_invalidAmount_throwsException() {
        List<Roomie> sharers = List.of(me, second);

        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", null, "2026-10-20", "food", sharers, expenseDAO));
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 0.0, "2026-10-20", "food", sharers, expenseDAO));
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", -3.0, "2026-10-20", "food", sharers, expenseDAO));
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 100001.0, "2026-10-20", "food", sharers, expenseDAO));
    }

    @Test
    void addExpense_invalidDescription_throwsExceptionAndSavesNothing() {
        List<Roomie> sharers = List.of(me, second);

        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense(null, 45.0, "2026-10-20", "food", sharers, expenseDAO));
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("  ", 45.0, "2026-10-20", "food", sharers, expenseDAO));
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("a".repeat(51), 45.0, "2026-10-20", "food", sharers, expenseDAO));

        verify(expenseDAO, never()).createAll(anyList());
    }

    @Test
    void addExpense_invalidDate_throwsException() {
        List<Roomie> sharers = List.of(me, second);

        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 45.0, null, "food", sharers, expenseDAO));
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 45.0, "20/10/2026", "food", sharers, expenseDAO));
    }

    @Test
    void addExpense_invalidCategory_throwsException() {
        List<Roomie> sharers = List.of(me, second);

        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 45.0, "2026-10-20", "unknown", sharers, expenseDAO));
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 45.0, "2026-10-20", null, sharers, expenseDAO));
    }


    // ------------------------- addExpense : who is calling --------------------------------------------------

    @Test
    void addExpense_roomieNotAcceptedYet_throwsNotInRoomshareException() {
        Roomie pending = pendingRoomie();

        assertThrows(NotInRoomshareException.class,
                () -> pending.addExpense("Groceries", 45.0, "2026-10-20", "food", List.of(me, second), expenseDAO));

        verify(expenseDAO, never()).createAll(anyList());
    }

    @Test
    void addExpense_roomieWithoutRoomshare_throwsNotInRoomshareException() {
        Roomie alone = outsider();

        assertThrows(NotInRoomshareException.class,
                () -> alone.addExpense("Groceries", 45.0, "2026-10-20", "food", List.of(me, second), expenseDAO));
    }


    // ------------------------- My expenses --------------------------------------------------

    @Test
    void findMyExpenses_keepsOnlyTheDebtsThatConcernMe() {
        Expense iOwe = debt(10, me, second, LocalDate.of(2026, 10, 1));
        Expense iAmOwed = debt(10, third, me, LocalDate.of(2026, 10, 10));
        Expense notMine = debt(10, second, third, LocalDate.of(2026, 10, 20));

        List<Expense> mine = me.findMyExpenses();

        assertEquals(2, mine.size());
        assertTrue(mine.contains(iOwe));
        assertTrue(mine.contains(iAmOwed));
        assertFalse(mine.contains(notMine));
    }

    @Test
    void findMyExpenses_theNewestIsFirst() {
        Expense old = debt(10, me, second, LocalDate.of(2026, 10, 1));
        Expense recent = debt(10, third, me, LocalDate.of(2026, 10, 20));

        List<Expense> mine = me.findMyExpenses();

        assertSame(recent, mine.get(0));
        assertSame(old, mine.get(1));
    }


    // ------------------------- Totals --------------------------------------------------

    @Test
    void calculateToPay_addsWhatIOwe() {
        debt(10.5, me, second);
        debt(4.25, me, third);
        debt(7, second, me);

        assertEquals(14.75, me.calculateToPay(), 0.001);
    }

    @Test
    void calculateToReceive_addsWhatTheOthersOweMe() {
        debt(10.5, me, second);
        debt(4.25, second, me);
        debt(7, third, me);

        assertEquals(11.25, me.calculateToReceive(), 0.001);
    }

    @Test
    void totals_ignoreTheSettledDebts() {
        Expense settled = debt(10, me, second);
        settled.setSettled(true);
        debt(5, me, third);
        debt(8, second, me);

        assertEquals(5.0, me.calculateToPay(), 0.001);
        assertEquals(8.0, me.calculateToReceive(), 0.001);
    }

    @Test
    void totals_noDebt_giveZero() {
        assertEquals(0.0, me.calculateToPay(), 0.001);
        assertEquals(0.0, me.calculateToReceive(), 0.001);
        assertFalse(me.hasToPay());
        assertFalse(me.hasToReceive());
    }

    @Test
    void hasToPayAndHasToReceive_trueWhenThereIsSomething() {
        debt(10, me, second);
        debt(5, third, me);

        assertTrue(me.hasToPay());
        assertTrue(me.hasToReceive());
    }


    // ------------------------- Balances --------------------------------------------------

    @Test
    void calculateBalance_payerIsOwedAndTheOthersOwe() {
        // I paid 30 for the 3 of us: the 2 others owe me 10 each
        debt(10, second, me);
        debt(10, third, me);

        assertEquals(2000L, me.calculateBalanceInCents());
        assertEquals(-1000L, second.calculateBalanceInCents());
        assertEquals(-1000L, third.calculateBalanceInCents());
        assertEquals(20.0, me.calculateBalance(), 0.001);
        assertEquals(-10.0, second.calculateBalance(), 0.001);
    }

    @Test
    void calculateBalance_theTotalOfTheRoomshareIsAlwaysZero() {
        debt(10.33, second, me);
        debt(3.34, third, me);
        debt(7.1, me, second);

        long total = me.calculateBalanceInCents()
                + second.calculateBalanceInCents()
                + third.calculateBalanceInCents();

        assertEquals(0L, total);
    }

    @Test
    void calculateBalance_ignoresTheSettledDebts() {
        Expense settled = debt(10, second, me);
        settled.setSettled(true);

        assertEquals(0L, me.calculateBalanceInCents());
        assertEquals(0L, second.calculateBalanceInCents());
    }

    @Test
    void calculateBalance_anOwnerOnlyHasWhatHeReceives() {
        // An owner never owes money
        debt(5, me, owner);

        assertEquals(5.0, owner.calculateBalance(), 0.001);
    }

    @Test
    void getBalanceStatus_creditorDebtorOrBalanced() {
        debt(10, second, me);

        assertEquals("creditor", me.getBalanceStatus());
        assertEquals("debtor", second.getBalanceStatus());
        assertEquals("balanced", third.getBalanceStatus());
    }

    @Test
    void getBalanceSign_negativeWhenTheRoomieOwesMoney() {
        debt(10, second, me);

        assertEquals("positive", me.getBalanceSign());
        assertEquals("negative", second.getBalanceSign());
        assertEquals("positive", third.getBalanceSign());
    }


    // ------------------------- Display --------------------------------------------------

    @Test
    void calculatePercent_theBiggestBalanceGets100AndTheOthersAreComparedToIt() {
        // me +20, second -10, third -10
        debt(10, second, me);
        debt(10, third, me);

        assertEquals(100, me.calculatePercent());
        assertEquals(50, second.calculatePercent());
    }

    @Test
    void calculatePercent_allBalancesAtZero_gives0() {
        assertEquals(0, me.calculatePercent());
    }

    @Test
    void chooseColor_eachRoomieOfTheListHasHisOwnColor() {
        assertNotEquals(me.chooseColor(), second.chooseColor());
        assertNotEquals(second.chooseColor(), third.chooseColor());
    }


    // ------------------------- Refunds --------------------------------------------------

    @Test
    void suggestRefunds_eachDebtorPaysTheCreditor() {
        // me +20, second -10, third -10
        debt(10, second, me);
        debt(10, third, me);

        List<Expense> refunds = me.suggestRefunds();

        assertEquals(2, refunds.size());
        for (Expense refund : refunds) {
            assertSame(me, refund.getCreditor());
            assertEquals(10.0, refund.getSum(), 0.001);
        }
    }

    @Test
    void suggestRefunds_theBiggestDebtorPaysFirst() {
        // me +20, second -15, third -5
        debt(15, second, me);
        debt(5, third, me);

        List<Expense> refunds = me.suggestRefunds();

        assertEquals(2, refunds.size());
        assertSame(second, refunds.get(0).getDebtor());
        assertEquals(15.0, refunds.get(0).getSum(), 0.001);
        assertSame(third, refunds.get(1).getDebtor());
        assertEquals(5.0, refunds.get(1).getSum(), 0.001);
    }

    @Test
    void suggestRefunds_oneDebtorPaysSeveralCreditors() {
        // me +5, second +10, third -15: the third roomie pays the 2 others
        debt(5, third, me);
        debt(10, third, second);

        List<Expense> refunds = me.suggestRefunds();

        assertEquals(2, refunds.size());
        assertSame(second, refunds.get(0).getCreditor());   // the one who is owed the most is paid first
        assertEquals(10.0, refunds.get(0).getSum(), 0.001);
        assertSame(me, refunds.get(1).getCreditor());
        assertEquals(5.0, refunds.get(1).getSum(), 0.001);
    }

    @Test
    void suggestRefunds_everythingIsBalanced_noRefund() {
        assertTrue(me.suggestRefunds().isEmpty());
    }

    @Test
    void suggestRefunds_neverSavesAnything() {
        debt(10, second, me);

        me.suggestRefunds();

        // A refund is only a suggestion: nothing goes to the db
        verifyNoInteractions(expenseDAO);
    }
}