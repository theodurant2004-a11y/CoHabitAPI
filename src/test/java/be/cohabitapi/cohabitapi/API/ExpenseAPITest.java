package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.ExpenseDAO;
import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DTO.CreateExpenseRequest;
import be.cohabitapi.cohabitapi.DTO.ExpensesResponse;
import be.cohabitapi.cohabitapi.Models.Expense;
import be.cohabitapi.cohabitapi.Models.MembershipStatus;
import be.cohabitapi.cohabitapi.Models.Owner;
import be.cohabitapi.cohabitapi.Models.RoomShare;
import be.cohabitapi.cohabitapi.Models.Roomie;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

// These tests check that ExpenseAPI answers with the right HTTP code
// The business logic is tested in ExpenseTest, here we only check the translation model => HTTP
// We use mocks for the DAOs and for the roomies, so no database is needed
@ExtendWith(MockitoExtension.class)
class ExpenseAPITest {

    @Mock
    private PersonDAO personDAO;
    @Mock
    private ExpenseDAO expenseDAO;
    @InjectMocks
    private ExpenseAPI api;

    // The 3 roomies of the roomshare. "me" is the roomie who is signed in
    private Roomie me;
    private Roomie second;
    private Roomie third;

    // This method runs before each test: it prepares a roomshare of 3 accepted roomies
    @BeforeEach
    void setUp() {
        me = roomie(1, "Test", "Roomie");
        second = roomie(2, "Nora", "Lfarh");
        third = roomie(3, "Alice", "Martin");

        RoomShare roomshare = mock(RoomShare.class);
        // lenient() because some tests do not use all these answers
        lenient().when(roomshare.getId()).thenReturn(166);
        lenient().when(me.getStatusMember()).thenReturn(MembershipStatus.ACCEPTED);
        lenient().when(me.getRoomshare()).thenReturn(roomshare);
        lenient().when(me.findRoomies(personDAO)).thenReturn(List.of(me, second, third));
        lenient().when(personDAO.findByEmail("me@mail.com")).thenReturn(me);
    }


    // ------------------------- Helpers --------------------------------------------------

    // Creates a fake roomie that only knows his id and his name
    private Roomie roomie(int id, String firstName, String lastName) {
        Roomie roomie = mock(Roomie.class);
        lenient().when(roomie.getIdPerson()).thenReturn(id);
        lenient().when(roomie.getFirstName()).thenReturn(firstName);
        lenient().when(roomie.getLastName()).thenReturn(lastName);
        return roomie;
    }

    // A valid request: "me" paid 45 euros shared with the 2 others
    private CreateExpenseRequest validRequest() {
        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setRoomieEmail("me@mail.com");
        req.setDescription("Groceries");
        req.setAmount(45.0);
        req.setDate("2026-10-29");
        req.setPaidById(1);
        req.setCategory("food");
        req.setSharedWithIds(List.of(1, 2, 3));
        return req;
    }


    // ------------------------- GET : see my expenses --------------------------------------------------

    @Test
    void getExpenses_roomieWithDebts_returns200WithTheData() {
        // the second roomie owes 15 euros to me
        Expense debt = new Expense("Groceries", 15.0, LocalDate.of(2026, 10, 29), "food", second, me);
        when(expenseDAO.findByRoomshare(166)).thenReturn(List.of(debt));

        Response response = api.getExpenses("me@mail.com");

        assertEquals(200, response.getStatus());
        assertInstanceOf(ExpensesResponse.class, response.getEntity());

        ExpensesResponse body = (ExpensesResponse) response.getEntity();
        assertEquals(1, body.currentMemberId());
        assertEquals(3, body.members().size());
        assertEquals(15.0, body.summary().toReceive(), 0.001);
        assertEquals(0.0, body.summary().toPay(), 0.001);
        assertEquals(1, body.expenses().size());
        assertEquals("Nora Lfarh", body.expenses().get(0).debtor());
        assertEquals("Test Roomie", body.expenses().get(0).creditor());
        assertEquals("pending", body.expenses().get(0).status());
        assertEquals("2026-10-29", body.expenses().get(0).date());
        assertEquals(1, body.refunds().size());
        assertFalse(body.categories().isEmpty());
    }

    @Test
    void getExpenses_noDebt_returns200WithEmptyLists() {
        when(expenseDAO.findByRoomshare(166)).thenReturn(List.of());

        Response response = api.getExpenses("me@mail.com");

        assertEquals(200, response.getStatus());
        ExpensesResponse body = (ExpensesResponse) response.getEntity();
        assertTrue(body.expenses().isEmpty());
        assertTrue(body.refunds().isEmpty());
        assertEquals(0.0, body.summary().toPay(), 0.001);
        assertEquals(0.0, body.summary().toReceive(), 0.001);
    }

    @Test
    void getExpenses_emailIsEmpty_returns400() {
        assertEquals(400, api.getExpenses(null).getStatus());
        assertEquals(400, api.getExpenses("   ").getStatus());
    }

    @Test
    void getExpenses_unknownEmail_returns401() {
        when(personDAO.findByEmail("nobody@mail.com")).thenReturn(null);

        assertEquals(401, api.getExpenses("nobody@mail.com").getStatus());
    }

    @Test
    void getExpenses_anOwner_returns403() {
        // an Owner is a Person but not a Roomie
        when(personDAO.findByEmail("owner@mail.com")).thenReturn(mock(Owner.class));

        assertEquals(403, api.getExpenses("owner@mail.com").getStatus());
    }

    @Test
    void getExpenses_roomieNotAcceptedYet_returns403() {
        when(me.getStatusMember()).thenReturn(MembershipStatus.PENDING);

        assertEquals(403, api.getExpenses("me@mail.com").getStatus());
    }

    @Test
    void getExpenses_roomieWithoutRoomshare_returns403() {
        when(me.getRoomshare()).thenReturn(null);

        assertEquals(403, api.getExpenses("me@mail.com").getStatus());
    }

    @Test
    void getExpenses_unexpectedError_returns500() {
        when(expenseDAO.findByRoomshare(166)).thenThrow(new RuntimeException("database down"));

        assertEquals(500, api.getExpenses("me@mail.com").getStatus());
    }


    // ------------------------- POST : add an expense --------------------------------------------------

    @Test
    void createExpense_validRequest_returns201AndSavesTheDebts() {
        Response response = api.createExpense(validRequest());

        assertEquals(201, response.getStatus());
        // 45 euros shared by 3 and paid by me: 2 debts are saved in one call
        verify(expenseDAO, times(1)).createAll(anyList());
    }

    @Test
    void createExpense_noBody_returns400() {
        assertEquals(400, api.createExpense(null).getStatus());

        verify(expenseDAO, never()).createAll(anyList());
    }

    @Test
    void createExpense_invalidAmount_returns400() {
        CreateExpenseRequest req = validRequest();
        req.setAmount(0.0);

        assertEquals(400, api.createExpense(req).getStatus());
        verify(expenseDAO, never()).createAll(anyList());
    }

    @Test
    void createExpense_emptyDescription_returns400() {
        CreateExpenseRequest req = validRequest();
        req.setDescription("  ");

        assertEquals(400, api.createExpense(req).getStatus());
    }

    @Test
    void createExpense_roomieOfAnotherRoomshare_returns400() {
        CreateExpenseRequest req = validRequest();
        req.setSharedWithIds(List.of(1, 99));

        assertEquals(400, api.createExpense(req).getStatus());
        verify(expenseDAO, never()).createAll(anyList());
    }

    @Test
    void createExpense_unknownEmail_returns401() {
        CreateExpenseRequest req = validRequest();
        req.setRoomieEmail("nobody@mail.com");
        when(personDAO.findByEmail("nobody@mail.com")).thenReturn(null);

        assertEquals(401, api.createExpense(req).getStatus());
    }

    @Test
    void createExpense_anOwner_returns403() {
        CreateExpenseRequest req = validRequest();
        req.setRoomieEmail("owner@mail.com");
        when(personDAO.findByEmail("owner@mail.com")).thenReturn(mock(Owner.class));

        assertEquals(403, api.createExpense(req).getStatus());
    }

    @Test
    void createExpense_roomieNotAcceptedYet_returns403() {
        when(me.getStatusMember()).thenReturn(MembershipStatus.PENDING);

        assertEquals(403, api.createExpense(validRequest()).getStatus());
        verify(expenseDAO, never()).createAll(anyList());
    }

    @Test
    void createExpense_unexpectedError_returns500() {
        doThrow(new RuntimeException("database down")).when(expenseDAO).createAll(anyList());

        assertEquals(500, api.createExpense(validRequest()).getStatus());
    }
}