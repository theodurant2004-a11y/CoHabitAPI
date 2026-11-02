package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.ExpenseDAO;
import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DTO.CreateExpenseRequest;
import be.cohabitapi.cohabitapi.DTO.ExpensesResponse;
import be.cohabitapi.cohabitapi.EXCEPTION.ForbiddenRoleException;
import be.cohabitapi.cohabitapi.EXCEPTION.NotInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import be.cohabitapi.cohabitapi.Models.Expense;
import be.cohabitapi.cohabitapi.Models.ExpenseCategory;
import be.cohabitapi.cohabitapi.Models.Person;
import be.cohabitapi.cohabitapi.Models.Roomie;
import static be.cohabitapi.cohabitapi.API.APIResponse.error;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// The url is /api/expenses
@Path("/expenses")
public class ExpenseAPI {

    @Inject
    private PersonDAO personDAO;
    @Inject
    private ExpenseDAO expenseDAO;


    // to see my expenses, gives everything the page "Expenses" of a roomie needs
    // TODO : remove the email when the session system is efficient (the API will know who is calling)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getExpenses(@QueryParam("email") String email) {
        try {
            // the model does all the work, it checks, search and calculations
            Roomie me = Roomie.findAcceptedMember(email, personDAO);
            List<Roomie> roomies = me.findRoomies(personDAO);
            List<Expense> all = Expense.findAllOfRoomshare(me, expenseDAO);

            List<Expense> mine = Expense.keepMine(me, all);
            double toPay = Expense.calculateToPay(me, all);
            double toReceive = Expense.calculateToReceive(me, all);
            Map<Integer, Long> balances = Expense.calculateBalances(roomies, all);
            List<Expense> refunds = Expense.suggestRefunds(roomies, balances);

            // we answer with DTOs and not the model because of the passworldhash
            return Response.ok(toResponse(me, roomies, mine, toPay, toReceive, balances, refunds)).build();

        } catch (IllegalArgumentException e) {
            // error 400 : for ex the email is empty
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (UserNotFoundException e) {
            // error 401: neutral message so nobody can find out which emails have an account
            return error(Response.Status.UNAUTHORIZED, "Authentication required.");

        } catch (ForbiddenRoleException e) {
            // error 403: an owner has no expenses
            return error(Response.Status.FORBIDDEN, "Only a roomie can use the expenses.");

        } catch (NotInRoomshareException e) {
            // error 403: no roomshare yet or the request is still pending
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (RuntimeException e) {
            // error 500: anything unexpected, with a generic message
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    //to add an expence; one debt is created for each roomie of the split
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createExpense(CreateExpenseRequest req) {

        // if no body at all ; 400 error
        if (req == null) return error(Response.Status.BAD_REQUEST, "JSON invalid.");

        try {
            Expense.addExpense(
                    req.getRoomieEmail(),
                    req.getDescription(),
                    req.getAmount(),
                    req.getDate(),
                    req.getPaidById(),
                    req.getCategory(),
                    req.getSharedWithIds(),
                    personDAO,
                    expenseDAO);

            // 201 Created: new rows exist now. The front asks the list again, so there is no body
            return Response.status(Response.Status.CREATED).build();

        } catch (IllegalArgumentException e) {
            // eror 400 : something sent by the user is not valid
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (UserNotFoundException e) {
            // error 401: neutral message so nobody can find out which emails have an account
            return error(Response.Status.UNAUTHORIZED, "Authentication required.");

        } catch (ForbiddenRoleException e) {
            // error 403: an owner has no expenses
            return error(Response.Status.FORBIDDEN, "Only a roomie can use the expenses.");

        } catch (NotInRoomshareException e) {
            // error 403: no roomshare yet, or the request is still pending
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (RuntimeException e) {
            // error 500: anything unexpected, but the client only gets a generic message
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }


    //Model => DTO

    // this method only copies what the model calculated into the DTOs
    // the color and the width of the bar are only for the display, so we choose  here
    private static ExpensesResponse toResponse(Roomie me, List<Roomie> roomies, List<Expense> mine,
                                               double toPay, double toReceive,
                                               Map<Integer, Long> balances, List<Expense> refunds) {
        List<ExpensesResponse.MemberResponse> members = new ArrayList<>();
        List<ExpensesResponse.BalanceResponse> balanceList = new ArrayList<>();

        for (int i = 0; i < roomies.size(); i++) {
            Roomie roomie = roomies.get(i);
            String color = Expense.colorOf(i);
            long cents = balances.get(roomie.getIdPerson());
            int percent = Expense.calculatePercent(cents, balances);
            String sign = Expense.signOf(cents);

            members.add(new ExpensesResponse.MemberResponse(roomie.getIdPerson(), fullName(roomie), color));
            balanceList.add(new ExpensesResponse.BalanceResponse(
                    fullName(roomie), color, Expense.centsToEuros(cents), percent, sign));
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

        return new ExpensesResponse(me.getIdPerson(), members, categories,
                new ExpensesResponse.SummaryResponse(toPay, toReceive,
                        Expense.hasAmount(toPay), Expense.hasAmount(toReceive),
                        Expense.calculateMyBalance(me, balances), Expense.describeBalance(me, balances)),
                expenses, balanceList, refundList);
    }

    // first name + last name of a person
    private static String fullName(Person person) {
        return person.getFirstName() + " " + person.getLastName();
    }
}