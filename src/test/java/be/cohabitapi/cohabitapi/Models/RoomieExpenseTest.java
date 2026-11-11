package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.ExpenseDAO;
import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.EXCEPTION.ForbiddenRoleException;
import be.cohabitapi.cohabitapi.EXCEPTION.NotInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import be.cohabitapi.cohabitapi.EXCEPTION.ExpenseNotFoundException;
import be.cohabitapi.cohabitapi.EXCEPTION.NotCreditorException;
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoomieExpenseTest {

    private static final String PASSWORD = "Test1234!";

    @Mock
    private ExpenseDAO expenseDAO;
    @Mock
    private PersonDAO personDAO;
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

    // Hashing a password is slow, so we create the roomies only once
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


    // ------------------------- findAcceptedMember --------------------------------------------------

    @Test
    void findAcceptedMember_acceptedRoomie_givesHimBack() {
        when(personDAO.findByEmail("me@mail.com")).thenReturn(me);
        when(roomShareDAO.findWithRoomies(any())).thenReturn(roomshare);

        assertSame(me, Roomie.findAcceptedMember("me@mail.com", personDAO, roomShareDAO));
    }

    @Test
    void findAcceptedMember_emailIsNormalizedBeforeTheSearch() {
        when(personDAO.findByEmail("me@mail.com")).thenReturn(me);
        when(roomShareDAO.findWithRoomies(any())).thenReturn(roomshare);

        assertSame(me, Roomie.findAcceptedMember("  ME@Mail.com ", personDAO, roomShareDAO));
        verify(personDAO).findByEmail("me@mail.com");
    }

    @Test
    void findAcceptedMember_emptyEmail_throwsExceptionWithoutSearching() {
        assertThrows(IllegalArgumentException.class,
                () -> Roomie.findAcceptedMember("   ", personDAO, roomShareDAO));

        verify(personDAO, never()).findByEmail(any());
    }

    @Test
    void findAcceptedMember_unknownEmail_throwsUserNotFoundException() {
        when(personDAO.findByEmail("nobody@mail.com")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> Roomie.findAcceptedMember("nobody@mail.com", personDAO, roomShareDAO));
    }

    @Test
    void findAcceptedMember_anOwner_throwsForbiddenRoleException() {
        when(personDAO.findByEmail("owner@mail.com")).thenReturn(owner);

        assertThrows(ForbiddenRoleException.class,
                () -> Roomie.findAcceptedMember("owner@mail.com", personDAO, roomShareDAO));
    }

    @Test
    void findAcceptedMember_pendingRoomie_throwsNotInRoomshareException() {
        Roomie pending = pendingRoomie();
        when(personDAO.findByEmail("pending@mail.com")).thenReturn(pending);

        assertThrows(NotInRoomshareException.class,
                () -> Roomie.findAcceptedMember("pending@mail.com", personDAO, roomShareDAO));
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
                1, List.of(1, 2, 3), expenseDAO);

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
        me.addExpense("Groceries", 45.0, "2026-10-20", "food", 1, List.of(1, 2, 3), expenseDAO);

        verify(expenseDAO, times(1)).createAll(savedDebts.capture());
        assertEquals(2, savedDebts.getValue().size());
    }

    @Test
    void addExpense_putsTheDebtsInTheListsOfTheRoomies() {
        me.addExpense("Groceries", 45.0, "2026-10-20", "food", 1, List.of(1, 2, 3), expenseDAO);

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
                3, List.of(1, 2, 3), expenseDAO);

        assertEquals(2, debts.size());
        assertEquals(334, debts.get(0).getSumInCents());
        assertEquals(333, debts.get(1).getSumInCents());
    }

    @Test
    void addExpense_duplicateRoomies_areCountedOnce() {
        // "second" is in the list twice, but the split is still between 2 roomies: 10 euros => 5 each
        List<Expense> debts = me.addExpense("Taxi", 10.0, "2026-10-20", "other",
                1, List.of(1, 2, 2), expenseDAO);

        assertEquals(1, debts.size());
        assertEquals(5.0, debts.get(0).getSum(), 0.001);
    }

    @Test
    void addExpense_someoneElsePaid_theDebtGoesToThePayer() {
        // I add an expense that the second roomie paid for me and the third roomie
        List<Expense> debts = me.addExpense("Cinema", 20.0, "2026-10-20", "leisure",
                2, List.of(1, 2, 3), expenseDAO);

        assertEquals(2, debts.size());
        assertSame(second, debts.get(0).getCreditor());
        assertSame(me, debts.get(0).getDebtor());
        assertSame(second, debts.get(1).getCreditor());
        assertSame(third, debts.get(1).getDebtor());
        assertEquals(2, second.getExpensesToReceive().size());
        assertTrue(me.getExpensesToReceive().isEmpty());
    }


    // ------------------------- addExpense : what the user sent is not valid --------------------------------------------------

    @Test
    void addExpense_onlyThePayerInTheSplit_throwsExceptionAndSavesNothing() {
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 45.0, "2026-10-20", "food", 1, List.of(1), expenseDAO));

        verify(expenseDAO, never()).createAll(anyList());
    }

    @Test
    void addExpense_sharerNotInTheRoomshare_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 45.0, "2026-10-20", "food", 1, List.of(1, 99), expenseDAO));

        verify(expenseDAO, never()).createAll(anyList());
    }

    @Test
    void addExpense_payerNotInTheRoomshareOrMissing_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 45.0, "2026-10-20", "food", 99, List.of(1, 2), expenseDAO));
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 45.0, "2026-10-20", "food", null, List.of(1, 2), expenseDAO));

        verify(expenseDAO, never()).createAll(anyList());
    }

    @Test
    void addExpense_emptyOrNullSplit_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 45.0, "2026-10-20", "food", 1, List.of(), expenseDAO));
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 45.0, "2026-10-20", "food", 1, null, expenseDAO));
        // Arrays.asList accepts null, List.of does not
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 45.0, "2026-10-20", "food", 1, Arrays.asList(1, null), expenseDAO));
    }

    @Test
    void addExpense_invalidAmount_throwsException() {
        List<Integer> sharers = List.of(1, 2);

        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", null, "2026-10-20", "food", 1, sharers, expenseDAO));
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 0.0, "2026-10-20", "food", 1, sharers, expenseDAO));
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", -3.0, "2026-10-20", "food", 1, sharers, expenseDAO));
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 100001.0, "2026-10-20", "food", 1, sharers, expenseDAO));
    }

    @Test
    void addExpense_invalidDescription_throwsExceptionAndSavesNothing() {
        List<Integer> sharers = List.of(1, 2);

        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense(null, 45.0, "2026-10-20", "food", 1, sharers, expenseDAO));
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("  ", 45.0, "2026-10-20", "food", 1, sharers, expenseDAO));
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("a".repeat(51), 45.0, "2026-10-20", "food", 1, sharers, expenseDAO));

        verify(expenseDAO, never()).createAll(anyList());
    }

    @Test
    void addExpense_invalidDate_throwsException() {
        List<Integer> sharers = List.of(1, 2);

        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 45.0, null, "food", 1, sharers, expenseDAO));
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 45.0, "20/10/2026", "food", 1, sharers, expenseDAO));
    }

    @Test
    void addExpense_invalidCategory_throwsException() {
        List<Integer> sharers = List.of(1, 2);

        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 45.0, "2026-10-20", "unknown", 1, sharers, expenseDAO));
        assertThrows(IllegalArgumentException.class,
                () -> me.addExpense("Groceries", 45.0, "2026-10-20", null, 1, sharers, expenseDAO));
    }


    // ------------------------- addExpense : who is calling --------------------------------------------------

    @Test
    void addExpense_roomieNotAcceptedYet_throwsNotInRoomshareException() {
        Roomie pending = pendingRoomie();

        assertThrows(NotInRoomshareException.class,
                () -> pending.addExpense("Groceries", 45.0, "2026-10-20", "food", 1, List.of(1, 2), expenseDAO));

        verify(expenseDAO, never()).createAll(anyList());
    }

    @Test
    void addExpense_roomieWithoutRoomshare_throwsNotInRoomshareException() {
        Roomie alone = outsider();

        assertThrows(NotInRoomshareException.class,
                () -> alone.addExpense("Groceries", 45.0, "2026-10-20", "food", 1, List.of(1, 2), expenseDAO));
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


    // ------------------------- Refunds --------------------------------------------------

    @Test
    void suggestRefunds_eachDebtorPaysTheCreditor() {
        // me +20, second -10, third -10
        debt(10, second, me);
        debt(10, third, me);

        List<Refund> refunds = me.suggestRefunds();

        assertEquals(2, refunds.size());
        for (Refund refund : refunds) {
            assertSame(me, refund.getTo());
            assertEquals(10.0, refund.getAmount(), 0.001);
        }
    }

    @Test
    void suggestRefunds_theBiggestDebtorPaysFirst() {
        // me +20, second -15, third -5
        debt(15, second, me);
        debt(5, third, me);

        List<Refund> refunds = me.suggestRefunds();

        assertEquals(2, refunds.size());
        assertSame(second, refunds.get(0).getFrom());
        assertEquals(15.0, refunds.get(0).getAmount(), 0.001);
        assertSame(third, refunds.get(1).getFrom());
        assertEquals(5.0, refunds.get(1).getAmount(), 0.001);
    }

    @Test
    void suggestRefunds_oneDebtorPaysSeveralCreditors() {
        // me +5, second +10, third -15: the third roomie pays the 2 others
        debt(5, third, me);
        debt(10, third, second);

        List<Refund> refunds = me.suggestRefunds();

        assertEquals(2, refunds.size());
        assertSame(second, refunds.get(0).getTo());   // the one who is owed the most is paid first
        assertEquals(10.0, refunds.get(0).getAmount(), 0.001);
        assertSame(me, refunds.get(1).getTo());
        assertEquals(5.0, refunds.get(1).getAmount(), 0.001);
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


    // ------------------------- Settle an expense --------------------------------------------------

    // The creditor (the one who is owed) can settle: the expense is settled AND saved
    @Test
    void settleExpense_creditor_settlesTheExpenseAndSavesIt() {
        // The second roomie owes 10 euros to me
        Expense expense = debt(10, second, me);
        when(expenseDAO.findById(1)).thenReturn(expense);

        // I say that he paid
        me.settleExpense(1, expenseDAO);

        // The expense is settled and the DAO was asked to save it
        assertTrue(expense.isSettled());
        verify(expenseDAO).update(expense);
    }

    // The debtor cannot say "I paid" himself: refused, and nothing is saved
    @Test
    void settleExpense_debtor_isRefusedAndNothingIsSaved() {
        Expense expense = debt(10, second, me);
        when(expenseDAO.findById(1)).thenReturn(expense);

        assertThrows(NotCreditorException.class, () -> second.settleExpense(1, expenseDAO));

        // The expense is still pending and the DAO was never asked to save anything
        assertFalse(expense.isSettled());
        verify(expenseDAO, never()).update(any());
    }

    // Someone who is not in the expense at all cannot settle it either
    @Test
    void settleExpense_thirdRoomie_isRefused() {
        Expense expense = debt(10, second, me);
        when(expenseDAO.findById(1)).thenReturn(expense);

        assertThrows(NotCreditorException.class, () -> third.settleExpense(1, expenseDAO));

        assertFalse(expense.isSettled());
        verify(expenseDAO, never()).update(any());
    }

    // An id that does not exist gives a "not found" error
    @Test
    void settleExpense_unknownExpense_throwsNotFound() {
        when(expenseDAO.findById(99)).thenReturn(null);

        assertThrows(ExpenseNotFoundException.class, () -> me.settleExpense(99, expenseDAO));

        verify(expenseDAO, never()).update(any());
    }

    // An expense that is already settled cannot be settled again, and it is not saved a second time
    @Test
    void settleExpense_alreadySettled_throwsException() {
        Expense expense = debt(10, second, me);
        expense.settle();
        when(expenseDAO.findById(1)).thenReturn(expense);

        assertThrows(IllegalArgumentException.class, () -> me.settleExpense(1, expenseDAO));

        verify(expenseDAO, never()).update(any());
    }

    // What the user sees: once settled, the debt disappears from the balances AND from the suggested refunds
    @Test
    void settle_removesTheDebtFromTheBalancesAndTheRefunds() {
        Expense expense = debt(10, second, me);

        // Before: I am owed 10, the second roomie owes 10 and a refund is suggested
        assertEquals(1000L, me.calculateBalanceInCents());
        assertEquals(-1000L, second.calculateBalanceInCents());
        assertEquals(1, me.suggestRefunds().size());

        expense.settle();

        // After: nobody owes anything anymore
        assertEquals(0L, me.calculateBalanceInCents());
        assertEquals(0L, second.calculateBalanceInCents());
        assertTrue(me.suggestRefunds().isEmpty());
    }

}