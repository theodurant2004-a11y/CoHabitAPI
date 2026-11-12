package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.ExpenseDAO;
import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.DTO.BalanceResponse;
import be.cohabitapi.cohabitapi.DTO.CategoryResponse;
import be.cohabitapi.cohabitapi.DTO.CreateExpenseRequest;
import be.cohabitapi.cohabitapi.DTO.ExpenseLineResponse;
import be.cohabitapi.cohabitapi.DTO.ExpensesResponse;
import be.cohabitapi.cohabitapi.DTO.MemberResponse;
import be.cohabitapi.cohabitapi.DTO.RefundResponse;
import be.cohabitapi.cohabitapi.DTO.SummaryResponse;
import be.cohabitapi.cohabitapi.EXCEPTION.ForbiddenRoleException;
import be.cohabitapi.cohabitapi.EXCEPTION.NotInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import be.cohabitapi.cohabitapi.EXCEPTION.ExpenseNotFoundException;
import be.cohabitapi.cohabitapi.EXCEPTION.NotCreditorException;
import be.cohabitapi.cohabitapi.Models.Expense;
import be.cohabitapi.cohabitapi.Models.ExpenseCategory;
import be.cohabitapi.cohabitapi.Models.Person;
import be.cohabitapi.cohabitapi.Models.Refund;
import be.cohabitapi.cohabitapi.Models.RoomShare;
import be.cohabitapi.cohabitapi.Models.Roomie;
import static be.cohabitapi.cohabitapi.API.APIResponse.error;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

// Every route about the expenses of a roomshare
// The full URL is /api/expenses
// Each route starts with Roomie.findAcceptedMember: it checks who is calling (401 / 403)
// and gives back the roomie with his roomshare, its roomies and all their expenses loaded
@Path("/expenses")
public class ExpenseAPI {

    @Inject
    private PersonDAO personDAO;
    @Inject
    private ExpenseDAO expenseDAO;
    @Inject
    private RoomShareDAO roomShareDAO;


    // MY EXPENSES : GET /api/expenses?email=...
    // Everything the page shows: the summary, my expenses, the balances of everybody and the refunds
    // A GET has no body, so the email comes in the URL
    // TODO : remove the email when the session system is efficient (the API will know who is calling)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getExpenses(@QueryParam("email") String email) {
        try {
            // 1. Who is calling? He must be an accepted roomie
            Roomie me = Roomie.findAcceptedMember(email, personDAO, roomShareDAO);

            // 2. The model reads its own lists and calculates everything
            // We answer with DTOs and never with the model itself, which carries the password hash
            return Response.ok(toResponse(me, me.findMyExpenses(), me.suggestRefunds())).build();

        } catch (IllegalArgumentException e) {
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (UserNotFoundException e) {
            return error(Response.Status.UNAUTHORIZED, e.getMessage());

        } catch (ForbiddenRoleException e) {
            return error(Response.Status.FORBIDDEN,e.getMessage());

        } catch (NotInRoomshareException e) {
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (RuntimeException e) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    // THE CATEGORIES : GET /api/expenses/categories
    // Used to fill the list of the form. The front adds the icon of each category itself
    @GET
    @Path("/categories")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCategories() {
        try {
            List<CategoryResponse> categories = new ArrayList<>();
            for (ExpenseCategory category : ExpenseCategory.values()) {
                categories.add(new CategoryResponse(category.getCode(), category.getLabel()));
            }

            return Response.ok(categories).build();

        } catch (RuntimeException e) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    // ADD AN EXPENSE : POST /api/expenses
    // The roomie types one expense, and the API saves one expense for each roomie who owes his part
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createExpense(CreateExpenseRequest req) {

        if (req == null) return error(Response.Status.BAD_REQUEST, "JSON invalid.");

        try {
            Roomie me = Roomie.findAcceptedMember(req.getRoomieEmail(), personDAO, roomShareDAO);

            // JSON => objects of the model, the API only translates
            // The expense as it was typed in the form: its own setters check every value (400)
            Expense expense = new Expense(req.getDescription(), req.getAmount(), req.getDate(), req.getCategory());

            // The ids of the JSON become roomies: the roomshare looks for them in its own list (400)
            RoomShare roomshare = me.getRoomshare();
            Roomie payer = roomshare.findRoomie(req.getPaidById());
            List<Roomie> sharers = roomshare.findRoomies(req.getSharedWithIds());

            // The roomie who paid shares the expense with the roomies of the split
            payer.addExpense(expense, sharers, expenseDAO);

            // 201 Created: new rows exist now. The front asks the list again
            return Response.status(Response.Status.CREATED).build();

        } catch (IllegalArgumentException e) {
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (UserNotFoundException e) {
            return error(Response.Status.UNAUTHORIZED, e.getMessage());

        } catch (ForbiddenRoleException e) {
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (NotInRoomshareException e) {
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (RuntimeException e) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    // SETTLE AN EXPENSE : PUT /api/expenses/{id}/settle?email=...
    // Only the roomie who is owed the money can say that he was paid back
    // {id} is read from the URL by @PathParam (/api/expenses/abc gives a 404 automatically)
    // TODO : remove the email when the session system is efficient (the API will know who is calling)
    @PUT
    @Path("/{id}/settle")
    @Produces(MediaType.APPLICATION_JSON)
    public Response settleExpense(@PathParam("id") int id, @QueryParam("email") String email) {
        try {
            // must be an accepted roomie
            Roomie me = Roomie.findAcceptedMember(email, personDAO, roomShareDAO);

            // Only the roomie who is owed can settle it (403), only once (400), and it must exist (404)
            me.settleExpense(id, expenseDAO);

            // The front asks the list again to see the new totals
            return Response.noContent().build();

        } catch (IllegalArgumentException e) {
            // For ex : the expense is already settled
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (UserNotFoundException e) {
            return error(Response.Status.UNAUTHORIZED, e.getMessage());

        } catch (ExpenseNotFoundException e) {
            return error(Response.Status.NOT_FOUND, e.getMessage());

        } catch (NotCreditorException | ForbiddenRoleException | NotInRoomshareException e) {
            // The user exists but is not allowed to do this action
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (RuntimeException e) {
            // We do not send the real message : it could show details of the server
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }


    // Model => DTO
    // This method only copies what the model calculated into the DTOs, it never calculates anything
    private ExpensesResponse toResponse(Roomie me, List<Expense> mine, List<Refund> refunds) {
        List<MemberResponse> members = new ArrayList<>();
        List<BalanceResponse> balanceList = new ArrayList<>();

        for (Roomie roomie : me.getRoomshare().getAcceptedRoomies()) {

            members.add(new MemberResponse(roomie.getIdPerson(), roomie.getFullName()));
            balanceList.add(new BalanceResponse(
                    roomie.getFullName(), roomie.calculateBalance(), roomie.calculatePercent()));
        }

        List<ExpenseLineResponse> expenses = new ArrayList<>();
        for (Expense expense : mine) {
            Person creditor = expense.getCreditor();

            expenses.add(new ExpenseLineResponse(
                    expense.getId(),
                    expense.getName(),
                    expense.getSum(),
                    expense.getCreationDate().toString(),
                    expense.getDebtor().getIdPerson(),
                    expense.getDebtor().getFullName(),
                    creditor == null ? null : creditor.getIdPerson(),
                    creditor == null ? "" : creditor.getFullName(),
                    expense.getType().getCode(),
                    expense.isSettled()));
        }

        List<RefundResponse> refundList = new ArrayList<>();
        for (Refund refund : refunds) {
            refundList.add(new RefundResponse(
                    refund.getFrom().getFullName(), refund.getTo().getFullName(), refund.getAmount()));
        }

        SummaryResponse summary = new SummaryResponse(
                me.calculateToPay(), me.calculateToReceive(), me.calculateBalance());

        return new ExpensesResponse(me.getIdPerson(), members, summary, expenses, balanceList, refundList);
    }
}