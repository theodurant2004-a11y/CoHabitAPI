package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.ExpenseDAO;
import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.DTO.CreateExpenseRequest;
import be.cohabitapi.cohabitapi.DTO.ExpensesResponse;
import be.cohabitapi.cohabitapi.EXCEPTION.ForbiddenRoleException;
import be.cohabitapi.cohabitapi.EXCEPTION.NotInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import be.cohabitapi.cohabitapi.Models.Expense;
import be.cohabitapi.cohabitapi.Models.ExpenseCategory;
import be.cohabitapi.cohabitapi.Models.Person;
import be.cohabitapi.cohabitapi.Models.RoomShare;
import be.cohabitapi.cohabitapi.Models.Roomie;
import static be.cohabitapi.cohabitapi.API.APIResponse.error;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

// The url is /api/expenses
@Path("/expenses")
public class ExpenseAPI {

    @Inject
    private PersonDAO personDAO;
    @Inject
    private ExpenseDAO expenseDAO;
    @Inject
    private RoomShareDAO roomShareDAO;


    // To see my expenses
    // TODO : remove the email when the session system is efficient (the API will know who is calling)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getExpenses(@QueryParam("email") String email) {
        try {
            Roomie me = Roomie.findAcceptedMember(email, personDAO, roomShareDAO);

            // We answer with dto and not the model because of the password hash
            return Response.ok(toResponse(me, me.findMyExpenses(), me.suggestRefunds())).build();

        } catch (IllegalArgumentException e) {
            // error 400
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (UserNotFoundException e) {
            // error 401
            return error(Response.Status.UNAUTHORIZED, e.getMessage());

        } catch (ForbiddenRoleException e) {
            // error 403
            return error(Response.Status.FORBIDDEN,e.getMessage());

        } catch (NotInRoomshareException e) {
            // error 403
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (RuntimeException e) {
            // error 500
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    // To add an expense one debt is created for each roomie
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createExpense(CreateExpenseRequest req) {

        // if no body at all 400 error
        if (req == null) return error(Response.Status.BAD_REQUEST, "JSON invalid.");

        try {
            Roomie me = Roomie.findAcceptedMember(req.getRoomieEmail(), personDAO, roomShareDAO);
            RoomShare roomshare = me.getRoomshare();

            // The JSON only has ids: we turn them into roomies of the roomshare
            Roomie payer = roomshare.findRoomie(req.getPaidById());

            List<Roomie> sharers = new ArrayList<>();
            if (req.getSharedWithIds() != null) {
                for (Integer id : req.getSharedWithIds()) {
                    sharers.add(roomshare.findRoomie(id));
                }
            }

            // The roomie who paid adds the expense
            payer.addExpense(
                    req.getDescription(),
                    req.getAmount(),
                    req.getDate(),
                    req.getCategory(),
                    sharers,
                    expenseDAO);

            // 201 Created: new rows exist now. The front asks the list again
            return Response.status(Response.Status.CREATED).build();

        } catch (IllegalArgumentException e) {
            // error 400
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (UserNotFoundException e) {
            // error 401
            return error(Response.Status.UNAUTHORIZED, e.getMessage());

        } catch (ForbiddenRoleException e) {
            // error 403: an owner has no expenses
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (NotInRoomshareException e) {
            // error 403: no roomshare yet or request  pending
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (RuntimeException e) {
            // error 500
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }


    // Model => DTO
    // This method only copies what the model calculated into the DTOs
    private ExpensesResponse toResponse(Roomie me, List<Expense> mine, List<Expense> refunds) {
        List<ExpensesResponse.MemberResponse> members = new ArrayList<>();
        List<ExpensesResponse.BalanceResponse> balanceList = new ArrayList<>();

        for (Roomie roomie : me.getRoomshare().getAcceptedRoomies()) {
            String color = roomie.chooseColor();

            members.add(new ExpensesResponse.MemberResponse(roomie.getIdPerson(), fullName(roomie), color));
            balanceList.add(new ExpensesResponse.BalanceResponse(
                    fullName(roomie), color, roomie.calculateBalance(),
                    roomie.calculatePercent(), roomie.getBalanceSign()));
        }

        List<ExpensesResponse.CategoryResponse> categories = new ArrayList<>();
        for (ExpenseCategory category : ExpenseCategory.values()) {
            categories.add(new ExpensesResponse.CategoryResponse(
                    category.getCode(), category.getLabel(), category.getIcon()));
        }

        List<ExpensesResponse.ExpenseResponse> expenses = new ArrayList<>();
        for (Expense expense : mine) {
            expenses.add(new ExpensesResponse.ExpenseResponse(
                    expense.getId(),
                    expense.getName(),
                    expense.getSum(),
                    expense.getCreationDate().toString(),    // "2026-10-20"
                    fullName(expense.getDebtor()),
                    expense.getCreditor() == null ? "" : fullName(expense.getCreditor()),
                    expense.getCategory().getIcon(),
                    expense.isSettled() ? "settled" : "pending"));
        }

        List<ExpensesResponse.RefundResponse> refundList = new ArrayList<>();
        for (Expense refund : refunds) {
            refundList.add(new ExpensesResponse.RefundResponse(
                    fullName(refund.getDebtor()), fullName(refund.getCreditor()), refund.getSum()));
        }

        ExpensesResponse.SummaryResponse summary = new ExpensesResponse.SummaryResponse(
                me.calculateToPay(), me.calculateToReceive(),
                me.hasToPay(), me.hasToReceive(),
                me.calculateBalance(), me.getBalanceStatus());

        return new ExpensesResponse(me.getIdPerson(), members, categories, summary,
                expenses, balanceList, refundList);
    }

    // First name  last name of a person
    private String fullName(Person person) {
        return person.getFirstName() + " " + person.getLastName();
    }
}