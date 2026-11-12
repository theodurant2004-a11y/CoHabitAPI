package be.cohabitapi.cohabitapi.Models;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// These tests check the rules of a charge (a rent or a bill of the owner) when it is created from the form
// No DAO here: a charge is never saved itself, RoomShareChargeTest checks what is saved
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChargeTest {

    private static final String PASSWORD = "Test1234!";
    private static final String DUE_DATE = "2026-11-28";

    private RoomShare roomshare;

    // The 3 roomies of the roomshare, in this order
    private Roomie nora;
    private Roomie arnaud;
    private Roomie manoah;

    // Hashing a password is slow, so we create the roomies only once
    @BeforeAll
    void createRoomshare() {
        Owner owner = new Owner("Doe", "Alice", "owner@mail.com", PASSWORD);
        roomshare = new RoomShare("ABC123", "Belleville", owner);

        // This constructor adds the roomie to the roomshare and makes him ACCEPTED
        nora = new Roomie("Lfarh", "Nora", "nora@mail.com", PASSWORD, roomshare);
        arnaud = new Roomie("J", "Arnaud", "arnaud@mail.com", PASSWORD, roomshare);
        manoah = new Roomie("Carlier", "Manoah", "manoah@mail.com", PASSWORD, roomshare);
    }

    @BeforeEach
    void clearExpenses() {
        for (Roomie roomie : List.of(nora, arnaud, manoah)) {
            roomie.getExpensesToPay().clear();
        }
    }


    // ------------------------- Helpers --------------------------------------------------

    // A bill published once, shared between these roomies
    private Charge bill(Double total, Roomie... sharers) {
        return new Charge("Electricity", total, DUE_DATE, "bills", "once", Arrays.asList(sharers));
    }


    // ------------------------- The parts --------------------------------------------------

    @Test
    void newCharge_sharedByTwoRoomies_createsOnePartForEachRoomie() {
        // Act
        Charge charge = bill(90.0, nora, arnaud);

        // Assert
        assertEquals(2, charge.getParts().size());
        assertEquals(45.0, charge.getParts().get(0).getSum(), 0.001);
        assertEquals(45.0, charge.getParts().get(1).getSum(), 0.001);
        assertEquals(nora, charge.getParts().get(0).getDebtor());
        assertEquals(arnaud, charge.getParts().get(1).getDebtor());
    }

    @Test
    void newCharge_putsThePartsInTheListsOfTheRoomies() {
        Charge charge = bill(90.0, nora, arnaud);

        assertEquals(List.of(charge.getParts().get(0)), nora.getExpensesToPay());
        assertEquals(List.of(charge.getParts().get(1)), arnaud.getExpensesToPay());
        assertTrue(manoah.getExpensesToPay().isEmpty());
    }

    @Test
    void newCharge_thePartsKeepTheNameTheCategoryAndTheDueDate() {
        Charge charge = bill(90.0, nora);
        Expense part = charge.getParts().get(0);

        assertEquals("Electricity", part.getName());
        assertEquals(ExpenseCategory.BILLS, part.getType());
        assertEquals(LocalDate.of(2026, 11, 28), part.getDueDate());
        assertEquals(LocalDate.now(), part.getCreationDate());
        assertFalse(part.isSettled());
    }

    @Test
    void newCharge_remainderInCents_isGivenToTheFirstRoomiesAndTheTotalStaysExact() {
        // 100.00 / 3 = 33.34 + 33.33 + 33.33
        Charge charge = bill(100.0, nora, arnaud, manoah);

        assertEquals(33.34, charge.getParts().get(0).getSum(), 0.001);
        assertEquals(33.33, charge.getParts().get(1).getSum(), 0.001);
        assertEquals(33.33, charge.getParts().get(2).getSum(), 0.001);
        assertEquals(100.0, charge.getTotal());
    }

    @Test
    void newCharge_sameRoomieTwice_isCountedOnce() {
        Charge charge = bill(90.0, nora, nora, arnaud);

        assertEquals(2, charge.getParts().size());
        assertEquals(1, nora.getExpensesToPay().size());
    }

    @Test
    void newCharge_noRoomie_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> bill(90.0));
        assertThrows(IllegalArgumentException.class,
                () -> new Charge("Electricity", 90.0, DUE_DATE, "bills", "once", null));
    }

    @Test
    void newCharge_nullRoomieInTheList_throwsException() {
        List<Roomie> sharers = new ArrayList<>();
        sharers.add(nora);
        sharers.add(null);

        assertThrows(IllegalArgumentException.class,
                () -> new Charge("Electricity", 90.0, DUE_DATE, "bills", "once", sharers));
    }

    @Test
    void newCharge_amountTooSmallToBeSplit_throwsException() {
        // 1 cent cannot be shared between 2 roomies
        assertThrows(IllegalArgumentException.class, () -> bill(0.01, nora, arnaud));
    }


    // ------------------------- The name and the amount --------------------------------------------------

    @Test
    void newCharge_rent_isNamedByTheModel() {
        // The form of the rent has no title
        Charge rent = new Charge(null, 850.0, DUE_DATE, "rent", "monthly", List.of(nora));

        assertEquals("Rent", rent.getName());
        assertEquals(ExpenseCategory.RENT, rent.getCategory());
    }

    @Test
    void newCharge_billWithoutName_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Charge(null, 90.0, DUE_DATE, "bills", "once", List.of(nora)));
        assertThrows(IllegalArgumentException.class,
                () -> new Charge("   ", 90.0, DUE_DATE, "bills", "once", List.of(nora)));
    }

    @Test
    void newCharge_amountMissingOrNotPositive_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> bill(null, nora));
        assertThrows(IllegalArgumentException.class, () -> bill(0.0, nora));
        assertThrows(IllegalArgumentException.class, () -> bill(-5.0, nora));
    }

    @Test
    void newCharge_refused_givesNoPartToTheRoomies() {
        // The amount is checked before the roomies receive their part
        assertThrows(IllegalArgumentException.class, () -> bill(0.0, nora, arnaud));

        assertTrue(nora.getExpensesToPay().isEmpty());
        assertTrue(arnaud.getExpensesToPay().isEmpty());
    }


    // ------------------------- The due date --------------------------------------------------

    @Test
    void newCharge_dueDateMissing_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Charge("Electricity", 90.0, null, "bills", "once", List.of(nora)));
        assertThrows(IllegalArgumentException.class,
                () -> new Charge("Electricity", 90.0, "  ", "bills", "once", List.of(nora)));
    }

    @Test
    void newCharge_dueDateThatIsNotADate_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Charge("Electricity", 90.0, "28/11/2026", "bills", "once", List.of(nora)));
    }


    // ------------------------- The category and the recurrence --------------------------------------------------

    @Test
    void newCharge_categoryThatIsNotARentOrABill_throwsException() {
        // "food" exists, but it is a category for the expenses between the roomies
        assertThrows(IllegalArgumentException.class,
                () -> new Charge("Pizza", 40.0, DUE_DATE, "food", "once", List.of(nora)));
        assertThrows(IllegalArgumentException.class,
                () -> new Charge("Pizza", 40.0, DUE_DATE, "unknown", "once", List.of(nora)));
        assertThrows(IllegalArgumentException.class,
                () -> new Charge("Pizza", 40.0, DUE_DATE, null, "once", List.of(nora)));
    }

    @Test
    void newCharge_recurrenceCode_becomesTheRecurrence() {
        assertEquals(Recurrence.ONCE,
                new Charge(null, 850.0, "2026-11-01", "rent", "once", List.of(nora)).getRecurrence());
        assertEquals(Recurrence.MONTHLY,
                new Charge(null, 850.0, "2026-11-02", "rent", "monthly", List.of(nora)).getRecurrence());
        // The codes are cleaned like the codes of the categories
        assertEquals(Recurrence.YEARLY,
                new Charge(null, 850.0, "2026-11-03", "rent", "  YEARLY ", List.of(nora)).getRecurrence());
    }

    @Test
    void newCharge_noRecurrence_isPublishedOnce() {
        // The form of a bill has no recurrence
        assertEquals(Recurrence.ONCE,
                new Charge("Internet", 40.0, DUE_DATE, "bills", null, List.of(nora)).getRecurrence());
        assertEquals(Recurrence.ONCE,
                new Charge("Water", 40.0, DUE_DATE, "bills", "  ", List.of(nora)).getRecurrence());
    }

    @Test
    void newCharge_unknownRecurrence_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Charge(null, 850.0, DUE_DATE, "rent", "weekly", List.of(nora)));
    }


    // ------------------------- equals and matches --------------------------------------------------

    @Test
    void equals_sameNameCategoryAndDueDate_isTheSameCharge() {
        // The amount and the roomies are not part of the identity of a charge
        assertEquals(bill(90.0, nora), bill(120.0, arnaud));
    }

    @Test
    void equals_anotherDueDateOrAnotherName_isAnotherCharge() {
        Charge december = new Charge("Electricity", 90.0, "2026-12-28", "bills", "once", List.of(nora));
        Charge internet = new Charge("Internet", 90.0, DUE_DATE, "bills", "once", List.of(nora));

        assertNotEquals(bill(90.0, arnaud), december);
        assertNotEquals(bill(90.0, manoah), internet);
    }

    @Test
    void matches_onlyThePartsOfTheCharge() {
        Charge electricity = bill(90.0, nora);
        Charge internet = new Charge("Internet", 40.0, DUE_DATE, "bills", "once", List.of(arnaud));

        assertTrue(electricity.matches(electricity.getParts().get(0)));
        assertFalse(electricity.matches(internet.getParts().get(0)));
        assertFalse(electricity.matches(null));
    }
}
