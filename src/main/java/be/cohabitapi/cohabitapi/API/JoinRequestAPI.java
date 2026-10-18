package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.DTO.JoinRoomShareRequest;
import be.cohabitapi.cohabitapi.DTO.JoinRoomShareResponse;
import be.cohabitapi.cohabitapi.EXCEPTION.AlreadyInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.ForbiddenRoleException;
import be.cohabitapi.cohabitapi.EXCEPTION.InvalidInvitationKeyException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import be.cohabitapi.cohabitapi.Models.Roomie;
import static be.cohabitapi.cohabitapi.API.APIResponse.error;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Collections;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

// The URL
@Path("/join-requests")
public class JoinRequestAPI {

    // Used to write the technical error in the Tomcat console
    private static final Logger LOGGER = Logger.getLogger(JoinRequestAPI.class.getName());

    // @Inject: the server gives us the DAOs
    @Inject
    private PersonDAO personDAO;

    @Inject
    private RoomShareDAO roomShareDAO;

    // POST: the front sends data. It consumes JSON (the DTO) and produces JSON (the response)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendJoinRequest(JoinRoomShareRequest req) {

        // Nobody at all: 400. The API reads, asks the model, translates the result
        if (req == null) return error(Response.Status.BAD_REQUEST, "JSON invalid.");

        try {
            // The model
            Roomie roomie = Roomie.requestToJoin(
                    req.getRoomieEmail(),
                    req.getInvitationKey(),
                    personDAO,
                    roomShareDAO);

            // We answer with a small DTO never with the real model
            JoinRoomShareResponse resp = new JoinRoomShareResponse(
                    roomie.getStatusMember().name().toLowerCase(Locale.ROOT), // PENDING becomes "pending"
                    roomie.getRoomshare().getName());

            // 201 CREATED: a request was created
            return Response.status(Response.Status.CREATED)
                    .entity(Collections.singletonMap("joinRequest", resp))
                    .build();

            // Each exception of the model becomes an HTTP code.
            // The order matters: IllegalArgumentException comes first, RuntimeException (the generic one) last.
        } catch (IllegalArgumentException e) {
            // 400: the key or the email is not valid (the message comes from the model)
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (UserNotFoundException e) {
            // 401: neutral message, so nobody can find out which emails have an account
            return error(Response.Status.UNAUTHORIZED, "Authentication required.");

        } catch (ForbiddenRoleException e) {
            // 403: the user is known but his role is not allowed (an owner cannot join)
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (InvalidInvitationKeyException e) {
            // 404: no roomshare uses this key
            return error(Response.Status.NOT_FOUND, "Invalid invitation key.");

        } catch (AlreadyInRoomshareException e) {
            // 409 : the roomie already has a roomshare or a pending request
            return error(Response.Status.CONFLICT, e.getMessage());

        } catch (RuntimeException e) {
            // 500: anything unexpected (for example the database). We log the cause for us
            LOGGER.log(Level.SEVERE, "Join request failed", e);
            // but the client only gets a generic message, never a technical detail
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }
}