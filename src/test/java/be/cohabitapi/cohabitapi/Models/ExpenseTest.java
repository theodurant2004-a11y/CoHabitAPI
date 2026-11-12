package be.cohabitapi.cohabitapi.Models;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// These tests check the Expense object alone: the constructors, the setters and their checks, split and settle
// What happens between an expense and the roomies (the lists, the balances, the refunds) is tested in RoomieExpenseTest
class ExpenseTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 20);

    // A valid expense of "sum" euros, with the texts the front sends
    private Expense expense(double sum) {
        return new Expense("Groceries", sum, "2026-10-20", "food");
    }


    // ------------------------- Constructors --------------------------------------------------

    @Test
    void constructor_validValues_createsAPendingExpense() {
        Expense expense = new Expense("  Groceries ", 15.5, " 2026-10-20 ", " FOOD ");

        assertEquals("Groceries", expense.getName());        // the spaces are removed
        assertEquals(15.5, expense.getSum(), 0.001);
        assertEquals(1550, expense.getSumInCents());
        assertEquals(DATE, expense.getCreationDate());
        assertEquals(ExpenseCategory.FOOD, expense.getType());
        assertFalse(expense.isSettled());                     // a new expense is always pending
        // Nobody has it in his list yet, so it has no debtor and no creditor
        assertNull(expense.getDebtor());
        assertNull(expense.getCreditor());
    }

    @Test
    void constructor_blankName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new Expense("   ", 10.0, "2026-10-20", "food"));
        assertThrows(IllegalArgumentException.class, () -> new Expense(null, 10.0, "2026-10-20", "food"));
    }

    @Test
    void constructor_nameOf51Characters_throwsException() {
        String tooLong = "a".repeat(51);

        assertThrows(IllegalArgumentException.class, () -> new Expense(tooLong, 10.0, "2026-10-20", "food"));
    }

    @Test
    void constructor_nameOf50Characters_isAccepted() {
        String name = "a".repeat(50);

        assertEquals(name, new Expense(name, 10.0, "2026-10-20", "food").getName());
    }

    @Test
    void constructor_invalidSum_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new Expense("Groceries", 0.0, "2026-10-20", "food"));
        assertThrows(IllegalArgumentException.class, () -> new Expense("Groceries", -5.0, "2026-10-20", "food"));
        assertThrows(IllegalArgumentException.class, () -> new Expense("Groceries", 100001.0, "2026-10-20", "food"));
        // The front sent no amount at all
        assertThrows(IllegalArgumentException.class, () -> new Expense("Groceries", null, "2026-10-20", "food"));
    }

    @Test
    void constructor_missingDateOrTextThatIsNotADate_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new Expense("Groceries", 10.0, "20/10/2026", "food"));
        // A day that does not exist
        assertThrows(IllegalArgumentException.class, () -> new Expense("Groceries", 10.0, "2026-02-30", "food"));
        assertThrows(IllegalArgumentException.class, () -> new Expense("Groceries", 10.0, "   ", "food"));
        assertThrows(IllegalArgumentException.class, () -> new Expense("Groceries", 10.0, null, "food"));
    }

    @Test
    void constructor_missingOrUnknownCategory_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new Expense("Groceries", 10.0, "2026-10-20", "unknown"));
        assertThrows(IllegalArgumentException.class, () -> new Expense("Groceries", 10.0, "2026-10-20", null));
    }


    // ------------------------- Setters --------------------------------------------------

    @Test
    void setName_removesTheSpacesAround() {
        Expense expense = expense(10);

        expense.setName("  Pizza  ");

        assertEquals("Pizza", expense.getName());
    }

    @Test
    void setSum_nanOrNegative_throwsExceptionAndKeepsTheOldValue() {
        Expense expense = expense(10);

        assertThrows(IllegalArgumentException.class, () -> expense.setSum(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> expense.setSum(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> expense.setSum(-1.0));
        // The old value did not change
        assertEquals(10.0, expense.getSum(), 0.001);
    }

    @Test
    void setDate_null_throwsExceptionAndKeepsTheOldDate() {
        Expense expense = expense(10);

        assertThrows(IllegalArgumentException.class, () -> expense.setCreationDate((LocalDate) null));

        assertEquals(DATE, expense.getCreationDate());
    }

    @Test
    void setType_null_throwsException() {
        Expense expense = expense(10);

        assertThrows(IllegalArgumentException.class, () -> expense.setType((ExpenseCategory) null));
    }

    @Test
    void setType_changesTheCategory() {
        Expense expense = expense(10);

        expense.setType(ExpenseCategory.RENT);

        assertEquals(ExpenseCategory.RENT, expense.getType());
    }

    @Test
    void setDueDate_canBeSetOrEmpty() {
        Expense expense = expense(10);
        assertNull(expense.getDueDate());

        expense.setDueDate(LocalDate.of(2026, 12, 1));

        assertEquals(LocalDate.of(2026, 12, 1), expense.getDueDate());
    }


    // ------------------------- Getters --------------------------------------------------

    @Test
    void getSumInCents_roundsCorrectly() {
        assertEquals(334, expense(3.34).getSumInCents());
    }

    @Test
    void getPendingSumInCents_countsNothingOnceTheExpenseIsSettled() {
        Expense expense = expense(10);
        assertEquals(1000, expense.getPendingSumInCents());

        expense.settle();

        // The expense knows that it must not be counted in the totals anymore
        assertEquals(0, expense.getPendingSumInCents());
        // Its own sum does not change
        assertEquals(1000, expense.getSumInCents());
    }

    @Test
    void getType_noTypeInTheDb_givesOther() {
        // The empty constructor is the one Hibernate uses: here the type is not set
        Expense expense = new Expense();

        assertEquals(ExpenseCategory.OTHER, expense.getType());
    }


    // ------------------------- Split --------------------------------------------------

    @Test
    void split_inThreeParts_givesThreeNewExpensesWithTheSameValues() {
        Expense whole = expense(45);
        whole.setDueDate(LocalDate.of(2026, 12, 1));

        List<Expense> parts = whole.split(3);

        assertEquals(3, parts.size());
        for (Expense part : parts) {
            assertEquals(15.0, part.getSum(), 0.001);
            assertEquals("Groceries", part.getName());
            assertEquals(DATE, part.getCreationDate());
            assertEquals(LocalDate.of(2026, 12, 1), part.getDueDate());
            assertEquals(ExpenseCategory.FOOD, part.getType());
            assertFalse(part.isSettled());
            assertNull(part.getDebtor());   // the roomies receive their part after
        }
    }

    @Test
    void split_remainderInCents_goesToTheFirstPartsAndTheTotalStaysExact() {
        // 10 euros in 3 parts = 3.34 + 3.33 + 3.33
        List<Expense> parts = expense(10).split(3);

        assertEquals(334, parts.get(0).getSumInCents());
        assertEquals(333, parts.get(1).getSumInCents());
        assertEquals(333, parts.get(2).getSumInCents());
    }

    @Test
    void split_inOnePart_givesTheWholeSum() {
        List<Expense> parts = expense(12.5).split(1);

        assertEquals(1, parts.size());
        assertEquals(12.5, parts.get(0).getSum(), 0.001);
    }

    @Test
    void split_doesNotChangeTheExpenseItself() {
        Expense whole = expense(45);

        whole.split(3);

        assertEquals(45.0, whole.getSum(), 0.001);
    }

    @Test
    void split_zeroPart_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> expense(45).split(0));
    }

    @Test
    void split_amountTooSmall_throwsException() {
        // 2 cents cannot be divided in 3 parts of at least 1 cent
        assertThrows(IllegalArgumentException.class, () -> expense(0.02).split(3));
    }


    // ------------------------- Settle --------------------------------------------------

    @Test
    void settle_pendingExpense_becomesSettled() {
        Expense expense = expense(10);
        assertFalse(expense.isSettled());

        expense.settle();

        assertTrue(expense.isSettled());
    }

    @Test
    void settle_alreadySettled_throwsException() {
        Expense expense = expense(10);
        expense.settle();

        // A settled expense cant be settled a second time
        assertThrows(IllegalArgumentException.class, expense::settle);
        assertTrue(expense.isSettled());
    }
}