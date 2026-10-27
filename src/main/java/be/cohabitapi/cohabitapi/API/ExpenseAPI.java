package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.ExpenseDAO;
import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DTO.CreateExpenseRequest;
import be.cohabitapi.cohabitapi.EXCEPTION.ForbiddenRoleException;
import be.cohabitapi.cohabitapi.EXCEPTION.NotInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import be.cohabitapi.cohabitapi.Models.Expense;
import static be.cohabitapi.cohabitapi.API.APIResponse.error;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

// The url is /api/expenses
@Path("/expenses")
public class ExpenseAPI {

    @Inject
    private PersonDAO personDAO;
    @Inject
    private ExpenseDAO expenseDAO;

    //to add an expence; one debt is created for each roomie of the split
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
}