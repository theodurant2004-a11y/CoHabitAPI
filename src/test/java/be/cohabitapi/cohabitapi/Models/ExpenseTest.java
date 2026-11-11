package be.cohabitapi.cohabitapi.Models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

// These tests check the Expense object: the constructor, the setters and their checks
// The calculations (split, balances, refunds) are tested in RoomieExpenseTest
class ExpenseTest {

    // Fake roomies: an Expense only needs to keep them
    private Roomie me;
    private Roomie second;

    @BeforeEach
    void setUp() {
        me = mock(Roomie.class);
        second = mock(Roomie.class);
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


    // ------------------------- Setters --------------------------------------------------

    @Test
    void setName_removesTheSpacesAround() {
        Expense expense = new Expense("Groceries", 10, LocalDate.now(), "food", second, me);

        expense.setName("  Pizza  ");

        assertEquals("Pizza", expense.getName());
    }

    @Test
    void setSum_nanOrNegative_throwsException() {
        Expense expense = new Expense("Groceries", 10, LocalDate.now(), "food", second, me);

        assertThrows(IllegalArgumentException.class, () -> expense.setSum(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> expense.setSum(-1));
        // The old value did not change
        assertEquals(10.0, expense.getSum(), 0.001);
    }

    @Test
    void setSettled_changesTheStatus() {
        Expense expense = new Expense("Groceries", 10, LocalDate.now(), "food", second, me);

        expense.setSettled(true);

        assertTrue(expense.isSettled());
    }

    @Test
    void setDueDate_canBeSetOrEmpty() {
        Expense expense = new Expense("Groceries", 10, LocalDate.now(), "food", second, me);
        assertNull(expense.getDueDate());

        expense.setDueDate(LocalDate.of(2026, 12, 1));

        assertEquals(LocalDate.of(2026, 12, 1), expense.getDueDate());
    }


    // ------------------------- Getters --------------------------------------------------

    @Test
    void getSumInCents_roundsCorrectly() {
        Expense expense = new Expense("Groceries", 3.34, LocalDate.now(), "food", second, me);

        assertEquals(334, expense.getSumInCents());
    }

    @Test
    void getCategory_noTypeInTheDb_givesOther() {
        // The empty constructor is the one Hibernate uses: here the type is not set
        Expense expense = new Expense();

        assertEquals(ExpenseCategory.OTHER, expense.getCategory());
    }

    // ------------------------- Settle --------------------------------------------------

    @Test
    void settle_pendingExpense_becomesSettled() {
        Expense expense = new Expense("Groceries", 10, LocalDate.now(), "food", second, me);
        assertFalse(expense.isSettled());

        expense.settle();

        assertTrue(expense.isSettled());
    }

    @Test
    void settle_alreadySettled_throwsException() {
        Expense expense = new Expense("Groceries", 10, LocalDate.now(), "food", second, me);
        expense.settle();

        // A settled expense cant be settled a second time
        assertThrows(IllegalArgumentException.class, expense::settle);
        assertTrue(expense.isSettled());
    }
}