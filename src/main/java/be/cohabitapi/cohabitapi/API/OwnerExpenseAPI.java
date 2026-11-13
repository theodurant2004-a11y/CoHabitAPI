package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.ExpenseDAO;
import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.DTO.ChargeResponse;
import be.cohabitapi.cohabitapi.DTO.CreateChargeRequest;
import be.cohabitapi.cohabitapi.DTO.ExpenseLineResponse;
import be.cohabitapi.cohabitapi.DTO.MemberResponse;
import be.cohabitapi.cohabitapi.DTO.OwnerExpensesResponse;
import be.cohabitapi.cohabitapi.EXCEPTION.ChargeAlreadyExistsException;
import be.cohabitapi.cohabitapi.EXCEPTION.ExpenseNotFoundException;
import be.cohabitapi.cohabitapi.EXCEPTION.ForbiddenRoleException;
import be.cohabitapi.cohabitapi.EXCEPTION.NotCreditorException;
import be.cohabitapi.cohabitapi.EXCEPTION.NotInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import be.cohabitapi.cohabitapi.Models.Charge;
import be.cohabitapi.cohabitapi.Models.Expense;
import be.cohabitapi.cohabitapi.Models.Owner;
import be.cohabitapi.cohabitapi.Models.RoomShare;
import be.cohabitapi.cohabitapi.Models.Roomie;
import static be.cohabitapi.cohabitapi.API.APIResponse.error;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

// Every route about the charges of the owner: the rent and the bills he publishes for his roomies
// The full URL is /api/expenses/owner (the routes of the roomies are in ExpenseAPI)
// Each route starts with Owner.checkRightOwner: it checks who is calling (401 / 403)
@Path("/expenses/owner")
public class OwnerExpenseAPI {

    @Inject
    private PersonDAO personDAO;
    @Inject
    private ExpenseDAO expenseDAO;
    @Inject
    private RoomShareDAO roomShareDAO;


    // MY CHARGES : GET /api/expenses/owner?email=...
    // Everything the page shows: the roomies of the forms, and the rents and the bills with the part of each roomie
    // A GET has no body, so the email comes in the URL
    // TODO : remove the email when the session system is efficient (the API will know who is calling)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCharges(@QueryParam("email") String email) {
        try {
            // 1. Who is calling? He must be an owner
            Owner owner = Owner.checkRightOwner(email, personDAO);

            // 2. His roomshare, loaded with its roomies and the charges
            RoomShare roomshare = owner.findRoomshareWithCharges(roomShareDAO);

            // 3. The roomshare groups the parts in charges. We answer with DTOs and never with the model itself
            return Response.ok(toResponse(roomshare, roomshare.findCharges())).build();

        } catch (IllegalArgumentException e) {
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (UserNotFoundException e) {
            return error(Response.Status.UNAUTHORIZED, e.getMessage());

        } catch (ForbiddenRoleException | NotInRoomshareException e) {
            // The user exists but is not an owner, or has no roomshare yet
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (RuntimeException e) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    // PUBLISH A CHARGE : POST /api/expenses/owner
    // The owner types one rent or one bill, and the API saves one expense for each roomie who has to pay
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCharge(CreateChargeRequest req) {

        if (req == null) return error(Response.Status.BAD_REQUEST, "JSON invalid.");

        try {
            Owner owner = Owner.checkRightOwner(req.getOwnerEmail(), personDAO);
            RoomShare roomshare = owner.findRoomshareWithCharges(roomShareDAO);

            // JSON => objects of the model, the API only translates
            // The ids of the JSON become roomies: the roomshare looks for them in its own list (400)
            List<Roomie> sharers = roomshare.findRoomies(req.getRoomieIds());

            // The charge as it was typed in the form: its own setters check every value (400)
            // and it is split between the roomies
            Charge charge = new Charge(req.getName(), req.getAmount(), req.getDueDate(),
                    req.getCategory(), req.getRecurrence(), sharers);

            // The roomshare publishes it: its owner is owed every part (409 if it already exists)
            roomshare.addCharge(charge, expenseDAO);

            // 201 Created: new rows exist now. The front asks the list again
            return Response.status(Response.Status.CREATED).build();

        } catch (IllegalArgumentException e) {
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (UserNotFoundException e) {
            return error(Response.Status.UNAUTHORIZED, e.getMessage());

        } catch (ForbiddenRoleException | NotInRoomshareException e) {
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (ChargeAlreadyExistsException e) {
            return error(Response.Status.CONFLICT, e.getMessage());

        } catch (RuntimeException e) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    // CONFIRM A PAYMENT : PUT /api/expenses/owner/{id}/settle?email=...
    // The owner says that a roomie paid his part. {id} is the id of the part (one expense), not of the whole charge
    @PUT
    @Path("/{id}/settle")
    @Produces(MediaType.APPLICATION_JSON)
    public Response settlePart(@PathParam("id") int id, @QueryParam("email") String email) {
        try {
            Owner owner = Owner.checkRightOwner(email, personDAO);

            // Only the person who is owed can settle it (403), only once (400), and it must exist (404)
            owner.settleExpense(id, expenseDAO);

            // The front asks the list again to see the new status
            return Response.noContent().build();

        } catch (IllegalArgumentException e) {
            // For ex : the part is already settled
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (UserNotFoundException e) {
            return error(Response.Status.UNAUTHORIZED, e.getMessage());

        } catch (ExpenseNotFoundException e) {
            return error(Response.Status.NOT_FOUND, e.getMessage());

        } catch (NotCreditorException | ForbiddenRoleException e) {
            // The user exists but is not allowed to do this action
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (RuntimeException e) {
            // We do not send the real message : it could show details of the server
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }


    // This method only copies what the model calculated into the DTOs, it never calculates anything
    private OwnerExpensesResponse toResponse(RoomShare roomshare, List<Charge> charges) {
        List<MemberResponse> members = new ArrayList<>();
        for (Roomie roomie : roomshare.getAcceptedRoomies()) {
            members.add(new MemberResponse(roomie.getIdPerson(), roomie.getFullName()));
        }

        List<ChargeResponse> chargeList = new ArrayList<>();
        for (Charge charge : charges) {

            // One line for each roomie who has to pay his part
            List<ExpenseLineResponse> parts = new ArrayList<>();
            for (Expense part : charge.getParts()) {
                parts.add(new ExpenseLineResponse(
                        part.getId(),
                        part.getName(),
                        part.getSum(),
                        part.getCreationDate().toString(),
                        part.getDebtor().getIdPerson(),
                        part.getDebtor().getFullName(),
                        part.getCreditor().getIdPerson(),
                        part.getCreditor().getFullName(),
                        part.getType().getCode(),
                        part.isSettled()));
            }

            // The due date is always filled for a charge, but an old row of the db could have none
            chargeList.add(new ChargeResponse(
                    charge.getName(),
                    charge.getCategory().getCode(),
                    charge.getTotal(),
                    charge.getDueDate() == null ? null : charge.getDueDate().toString(),
                    parts));
        }

        return new OwnerExpensesResponse(members, chargeList);
    }
}
