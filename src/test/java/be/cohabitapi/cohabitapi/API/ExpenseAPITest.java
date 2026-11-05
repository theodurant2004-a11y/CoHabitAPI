package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.ExpenseDAO;
import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.DTO.CreateExpenseRequest;
import be.cohabitapi.cohabitapi.DTO.ExpensesResponse;
import be.cohabitapi.cohabitapi.Models.Expense;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

// These tests check that ExpenseAPI answers with the right HTTP code
// The business logic is tested in RoomieExpenseTest, here we only check the translation model => HTTP
// The DAOs are mocks (no database), but the roomshare, the roomies and the expenses are real objects
@ExtendWith(MockitoExtension.class)
class ExpenseAPITest {

    @Mock
    private PersonDAO personDAO;
    @Mock
    private ExpenseDAO expenseDAO;
    @Mock
    private RoomShareDAO roomShareDAO;
    @InjectMocks
    private ExpenseAPI api;

    private Owner owner;
    private RoomShare roomshare;

    // The 3 roomies of the roomshare. "me" is the roomie who is signed in
    private Roomie me;
    private Roomie second;
    private Roomie third;

    // A roomshare of 3 accepted roomies
    @BeforeEach
    void setUp() {
        owner = new Owner("Doe", "Alice", "owner@mail.com", "Test1234!");
        roomshare = new RoomShare("ABC123", "Belleville", owner);

        // This constructor adds the roomie to the roomshare and makes him ACCEPTED
        me = new Roomie("Roomie", "Test", "me@mail.com", "Test1234!", roomshare);
        second = new Roomie("Lfarh", "Nora", "second@mail.com", "Test1234!", roomshare);
        third = new Roomie("Martin", "Alice", "third@mail.com", "Test1234!", roomshare);

        // No database here, so we give the ids ourselves
        me.setIdPerson(1);
        second.setIdPerson(2);
        third.setIdPerson(3);

        // lenient() because some tests do not use all these answers
        lenient().when(personDAO.findByEmail("me@mail.com")).thenReturn(me);
        // The fake DAO gives back the roomshare that is already complete
        lenient().when(roomShareDAO.findWithRoomies(any())).thenReturn(roomshare);
    }


    // ------------------------- Helpers --------------------------------------------------

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

    // A roomie who sent a join request but is not accepted yet
    private Roomie pendingRoomie() {
        Roomie pending = new Roomie("Ding", "Pen", "pending@mail.com", "Test1234!");
        pending.sendJoinRequest(roomshare);
        return pending;
    }


    // ------------------------- GET : see my expenses --------------------------------------------------

    @Test
    void getExpenses_roomieWithDebts_returns200WithTheData() {
        // the second roomie owes 15 euros to me
        Expense debt = new Expense("Groceries", 15.0, LocalDate.of(2026, 10, 29), "food", second, me);
        second.addExpenseToPay(debt);
        me.addExpenseToReceive(debt);

        Response response = api.getExpenses("me@mail.com");

        assertEquals(200, response.getStatus());
        assertInstanceOf(ExpensesResponse.class, response.getEntity());

        ExpensesResponse body = (ExpensesResponse) response.getEntity();
        assertEquals(1, body.getCurrentMemberId());
        assertEquals(3, body.getMembers().size());
        assertEquals(15.0, body.getSummary().getToReceive(), 0.001);
        assertEquals(0.0, body.getSummary().getToPay(), 0.001);
        assertEquals(1, body.getExpenses().size());
        assertEquals(2, body.getExpenses().get(0).getDebtorId());
        assertEquals("Nora Lfarh", body.getExpenses().get(0).getDebtor());
        assertEquals(1, body.getExpenses().get(0).getCreditorId());
        assertEquals("Test Roomie", body.getExpenses().get(0).getCreditor());
        assertFalse(body.getExpenses().get(0).isSettled());
        assertEquals("2026-10-29", body.getExpenses().get(0).getDate());
        assertEquals(1, body.getRefunds().size());
        assertEquals("Nora Lfarh", body.getRefunds().get(0).getFrom());
        assertEquals("Test Roomie", body.getRefunds().get(0).getTo());
        // the bar and the colors are prepared by the model
        assertEquals(100, body.getBalances().get(0).getPercent());
        assertEquals(15.0, body.getBalances().get(0).getAmount(), 0.001);
        assertEquals(-15.0, body.getBalances().get(1).getAmount(), 0.001);

        // my balance for the home page: the second roomie owes me 15 euros
        assertEquals(15.0, body.getSummary().getBalance(), 0.001);
    }

    @Test
    void getExpenses_noDebt_returns200WithEmptyLists() {
        Response response = api.getExpenses("me@mail.com");

        assertEquals(200, response.getStatus());
        ExpensesResponse body = (ExpensesResponse) response.getEntity();
        assertTrue(body.getExpenses().isEmpty());
        assertTrue(body.getRefunds().isEmpty());
        assertEquals(0.0, body.getSummary().getToPay(), 0.001);
        assertEquals(0.0, body.getSummary().getToReceive(), 0.001);
    }

    @Test
    void getExpenses_emailIsEmpty_returns400() {
        // normalizeEmail refuses a null or blank email, like Owner.checkRightOwner
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
        when(personDAO.findByEmail("owner@mail.com")).thenReturn(owner);

        assertEquals(403, api.getExpenses("owner@mail.com").getStatus());
    }

    @Test
    void getExpenses_roomieNotAcceptedYet_returns403() {
        when(personDAO.findByEmail("pending@mail.com")).thenReturn(pendingRoomie());

        assertEquals(403, api.getExpenses("pending@mail.com").getStatus());
    }

    @Test
    void getExpenses_roomieWithoutRoomshare_returns403() {
        // This roomie never asked to join a roomshare
        Roomie alone = new Roomie("Alone", "Bob", "alone@mail.com", "Test1234!");
        when(personDAO.findByEmail("alone@mail.com")).thenReturn(alone);

        assertEquals(403, api.getExpenses("alone@mail.com").getStatus());
    }

    @Test
    void getExpenses_unexpectedError_returns500() {
        doThrow(new RuntimeException("database down")).when(roomShareDAO).findWithRoomies(any());

        assertEquals(500, api.getExpenses("me@mail.com").getStatus());
    }


    // ------------------------- POST : add an expense --------------------------------------------------

    @Test
    void createExpense_validRequest_returns201AndSavesTheDebts() {
        Response response = api.createExpense(validRequest());

        assertEquals(201, response.getStatus());
        // 45 euros shared by 3 and paid by me: 2 debts are saved in one call
        verify(expenseDAO, times(1)).createAll(anyList());
        // The 2 others owe 15 euros each to me
        assertEquals(30.0, me.calculateToReceive(), 0.001);
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
        verify(expenseDAO, never()).createAll(anyList());
    }

    @Test
    void createExpense_roomieOfAnotherRoomshare_returns400() {
        CreateExpenseRequest req = validRequest();
        req.setSharedWithIds(List.of(1, 99));

        assertEquals(400, api.createExpense(req).getStatus());
        verify(expenseDAO, never()).createAll(anyList());
    }

    @Test
    void createExpense_noSplit_returns400() {
        CreateExpenseRequest req = validRequest();
        req.setSharedWithIds(null);

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
        when(personDAO.findByEmail("owner@mail.com")).thenReturn(owner);

        assertEquals(403, api.createExpense(req).getStatus());
    }

    @Test
    void createExpense_roomieNotAcceptedYet_returns403() {
        CreateExpenseRequest req = validRequest();
        req.setRoomieEmail("pending@mail.com");
        when(personDAO.findByEmail("pending@mail.com")).thenReturn(pendingRoomie());

        assertEquals(403, api.createExpense(req).getStatus());
        verify(expenseDAO, never()).createAll(anyList());
    }

    @Test
    void createExpense_unexpectedError_returns500() {
        doThrow(new RuntimeException("database down")).when(expenseDAO).createAll(anyList());

        assertEquals(500, api.createExpense(validRequest()).getStatus());
    }
}