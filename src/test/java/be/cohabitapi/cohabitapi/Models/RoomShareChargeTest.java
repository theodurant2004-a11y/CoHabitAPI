package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.ExpenseDAO;
import be.cohabitapi.cohabitapi.EXCEPTION.ChargeAlreadyExistsException;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

// These tests check the charges of the owner: publish a rent or a bill, read the list and confirm a payment
// The roomshare, the owner, the roomies and the charges are real objects, only the DAO is a mock
@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoomShareChargeTest {

    private static final String PASSWORD = "Test1234!";

    @Mock
    private ExpenseDAO expenseDAO;
    @Captor
    private ArgumentCaptor<List<Expense>> savedParts;

    private Owner owner;
    private RoomShare roomshare;

    // The 2 roomies of the roomshare, in this order
    private Roomie nora;
    private Roomie arnaud;

    // A roomie of another roomshare
    private Roomie stranger;

    // Hashing a password is slow, so we create the persons only once
    @BeforeAll
    void createRoomshares() {
        owner = new Owner("Doe", "Alice", "owner@mail.com", PASSWORD);
        roomshare = new RoomShare("ABC123", "Belleville", owner);

        // This constructor adds the roomie to the roomshare and makes him ACCEPTED
        nora = new Roomie("Lfarh", "Nora", "nora@mail.com", PASSWORD, roomshare);
        arnaud = new Roomie("J", "Arnaud", "arnaud@mail.com", PASSWORD, roomshare);

        Owner otherOwner = new Owner("Smith", "John", "other@mail.com", PASSWORD);
        RoomShare otherRoomshare = new RoomShare("XYZ789", "Elsewhere", otherOwner);
        stranger = new Roomie("Doe", "Jane", "stranger@mail.com", PASSWORD, otherRoomshare);
    }

    @BeforeEach
    void clearExpenses() {
        for (Roomie roomie : List.of(nora, arnaud, stranger)) {
            roomie.getExpensesToPay().clear();
        }
        owner.getExpensesToReceive().clear();
    }


    // ------------------------- Helpers --------------------------------------------------

    private Charge rent(String dueDate) {
        return new Charge(null, 850.0, dueDate, "rent", "monthly", List.of(nora, arnaud));
    }

    private Charge bill(String name, String dueDate) {
        return new Charge(name, 90.0, dueDate, "bills", "once", List.of(nora, arnaud));
    }


    // ------------------------- addCharge --------------------------------------------------

    @Test
    void addCharge_savesAllThePartsInOneCall() {
        // Act
        roomshare.addCharge(rent("2026-12-05"), expenseDAO);

        // Assert: one call for the two parts (one transaction)
        verify(expenseDAO).createAll(savedParts.capture());
        assertEquals(2, savedParts.getValue().size());
        assertEquals(425.0, savedParts.getValue().get(0).getSum(), 0.001);
        assertEquals(425.0, savedParts.getValue().get(1).getSum(), 0.001);
    }

    @Test
    void addCharge_theOwnerIsTheCreditorOfEveryPart() {
        Charge charge = rent("2026-12-05");

        roomshare.addCharge(charge, expenseDAO);

        for (Expense part : charge.getParts()) {
            assertEquals(owner, part.getCreditor());
        }
        assertEquals(charge.getParts(), owner.getExpensesToReceive());
        assertEquals(850.0, owner.calculateToReceive());
    }

    @Test
    void addCharge_sameChargeTwice_throwsChargeAlreadyExistsExceptionAndSavesOnlyTheFirst() {
        roomshare.addCharge(rent("2026-12-05"), expenseDAO);

        // Same name, same category, same due date
        assertThrows(ChargeAlreadyExistsException.class,
                () -> roomshare.addCharge(rent("2026-12-05"), expenseDAO));

        verify(expenseDAO, times(1)).createAll(anyList());
        assertEquals(2, owner.getExpensesToReceive().size());
    }

    @Test
    void addCharge_sameNameWithAnotherDueDate_isAccepted() {
        roomshare.addCharge(rent("2026-12-05"), expenseDAO);
        roomshare.addCharge(rent("2027-01-05"), expenseDAO);

        verify(expenseDAO, times(2)).createAll(anyList());
    }

    @Test
    void addCharge_roomieOfAnotherRoomshare_throwsExceptionAndSavesNothing() {
        Charge charge = new Charge("Internet", 40.0, "2026-11-28", "bills", "once", List.of(nora, stranger));

        assertThrows(IllegalArgumentException.class, () -> roomshare.addCharge(charge, expenseDAO));

        verify(expenseDAO, never()).createAll(anyList());
        assertTrue(owner.getExpensesToReceive().isEmpty());
    }

    @Test
    void addCharge_nullCharge_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> roomshare.addCharge(null, expenseDAO));

        verify(expenseDAO, never()).createAll(anyList());
    }


    // ------------------------- findCharges --------------------------------------------------

    @Test
    void findCharges_noCharge_givesAnEmptyList() {
        assertTrue(roomshare.findCharges().isEmpty());
    }

    @Test
    void findCharges_groupsThePartsOfTheSameCharge() {
        // Arrange: 2 charges of 2 parts each = 4 expenses in the list of the owner
        roomshare.addCharge(rent("2026-12-05"), expenseDAO);
        roomshare.addCharge(bill("Electricity", "2026-11-28"), expenseDAO);

        // Act
        List<Charge> charges = roomshare.findCharges();

        // Assert
        assertEquals(2, charges.size());
        for (Charge charge : charges) {
            assertEquals(2, charge.getParts().size());
        }
    }

    @Test
    void findCharges_theTotalIsTheSumOfTheParts() {
        roomshare.addCharge(bill("Electricity", "2026-11-28"), expenseDAO);

        Charge found = roomshare.findCharges().get(0);

        assertEquals("Electricity", found.getName());
        assertEquals(ExpenseCategory.BILLS, found.getCategory());
        assertEquals(90.0, found.getTotal());
    }

    @Test
    void findCharges_sameDueDateButAnotherName_areTwoCharges() {
        roomshare.addCharge(bill("Electricity", "2026-11-28"), expenseDAO);
        roomshare.addCharge(bill("Internet", "2026-11-28"), expenseDAO);

        assertEquals(2, roomshare.findCharges().size());
    }

    @Test
    void findCharges_theLastDueDateIsFirst() {
        roomshare.addCharge(rent("2026-11-05"), expenseDAO);
        roomshare.addCharge(rent("2027-01-05"), expenseDAO);
        roomshare.addCharge(rent("2026-12-05"), expenseDAO);

        List<Charge> charges = roomshare.findCharges();

        assertEquals("2027-01-05", charges.get(0).getDueDate().toString());
        assertEquals("2026-12-05", charges.get(1).getDueDate().toString());
        assertEquals("2026-11-05", charges.get(2).getDueDate().toString());
    }


    // ------------------------- settleExpense (the owner confirms a payment) --------------------------------------------------

    @Test
    void settleExpense_owner_settlesThePartAndSavesIt() {
        // Arrange
        Charge charge = rent("2026-12-05");
        roomshare.addCharge(charge, expenseDAO);
        Expense partOfNora = charge.getParts().get(0);
        when(expenseDAO.findById(1)).thenReturn(partOfNora);

        // Act
        owner.settleExpense(1, expenseDAO);

        // Assert: only the part of Nora is settled, Arnaud still owes his part
        assertTrue(partOfNora.isSettled());
        assertFalse(charge.getParts().get(1).isSettled());
        verify(expenseDAO).update(partOfNora);
        assertEquals(425.0, owner.calculateToReceive());
    }

    @Test
    void settleExpense_roomieOnAPartOfTheOwner_isRefusedAndNothingIsSaved() {
        Charge charge = rent("2026-12-05");
        roomshare.addCharge(charge, expenseDAO);
        Expense partOfNora = charge.getParts().get(0);
        when(expenseDAO.findById(1)).thenReturn(partOfNora);

        // Nora owes this part: she cannot say herself that she paid
        assertThrows(NotCreditorException.class, () -> nora.settleExpense(1, expenseDAO));

        assertFalse(partOfNora.isSettled());
        verify(expenseDAO, never()).update(any());
    }

    @Test
    void settleExpense_unknownPart_throwsExpenseNotFoundException() {
        // The mock answers null: no expense has this id
        assertThrows(ExpenseNotFoundException.class, () -> owner.settleExpense(99, expenseDAO));

        verify(expenseDAO, never()).update(any());
    }
}
