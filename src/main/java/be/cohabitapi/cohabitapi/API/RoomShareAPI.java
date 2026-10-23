package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.DTO.CreateRoomShareRequest;
import be.cohabitapi.cohabitapi.DTO.CreateRoomShareResponse;
import be.cohabitapi.cohabitapi.EXCEPTION.UnauthorizedUserException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import be.cohabitapi.cohabitapi.Models.Owner;
import be.cohabitapi.cohabitapi.Models.RoomShare;
import static be.cohabitapi.cohabitapi.API.APIResponse.error;
import static be.cohabitapi.cohabitapi.Models.RoomShare.generateInvitationKey;
import static be.cohabitapi.cohabitapi.Models.Owner.checkRightOwner;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Collections;

import be.cohabitapi.cohabitapi.DTO.JoinRoomShareRequest;
import be.cohabitapi.cohabitapi.DTO.JoinRoomShareResponse;
import be.cohabitapi.cohabitapi.EXCEPTION.AlreadyInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.ForbiddenRoleException;
import be.cohabitapi.cohabitapi.EXCEPTION.InvalidInvitationKeyException;
import be.cohabitapi.cohabitapi.Models.Roomie;
import java.util.Locale;

@Path("/roomshares")
public class RoomShareAPI {

    @Inject
    private RoomShareDAO roomShareDAO;
    @Inject private PersonDAO personDAO;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createRoomShare(CreateRoomShareRequest req){

        if (req == null) return error(Response.Status.BAD_REQUEST, "JSON invalid.");

        try {
            // Returns the corresponding user and verifies that it is an owner
            Owner owner = checkRightOwner(req.getOwnerEmail(), personDAO);

            // Generates the invitation key of the room share
            String key = generateInvitationKey(roomShareDAO);

            // Builds a RoomShare object to insert its attributes in db
            RoomShare rs = new RoomShare(key, req.getName(), owner);
            rs.create(roomShareDAO);

            // Build of the response using a DTO and not the real model
            CreateRoomShareResponse resp = new CreateRoomShareResponse(rs.getId(), rs.getInvitationKey(), rs.getName(), rs.getCreationDate(), rs.getOwner().getIdPerson());

            return Response.status(Response.Status.CREATED)
                    .entity(Collections.singletonMap("roomshare", resp))
                    .build();
        }
        catch (IllegalArgumentException e) {
            return error(Response.Status.BAD_REQUEST, e.getMessage());
        }
        catch (UserNotFoundException e) {
            return error(Response.Status.UNAUTHORIZED, e.getMessage());
        }
        catch (UnauthorizedUserException e) {
            return error(Response.Status.FORBIDDEN, e.getMessage());
        }
        catch(RuntimeException e){
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    // JOIN A ROOMSHARE : a roomie asks to join a roomshare with its invitation key.
    // Same class as the creation because it is an action on a roomshare (there is no "join request" class in the class diagram).
    // The full URL is /api/roomshares/join (the class path + this path).
    @POST
    @Path("/join")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response joinRoomShare(JoinRoomShareRequest req) {

        // No body at all: 400
        if (req == null) return error(Response.Status.BAD_REQUEST, "JSON invalid.");

        try {
            // The model does all the work (checks, search, save)
            Roomie roomie = Roomie.requestToJoin(
                    req.getRoomieEmail(),
                    req.getInvitationKey(),
                    personDAO,
                    roomShareDAO);

            // We answer with a small DTO, never with the real model (it contains the password hash)
            JoinRoomShareResponse resp = new JoinRoomShareResponse(
                    roomie.getStatusMember().name().toLowerCase(Locale.ROOT), // PENDING becomes "pending"
                    roomie.getRoomshare().getName());

            // 200 OK: nothing is created.the state of an existing roomie changes
            return Response.ok(Collections.singletonMap("joinRequest", resp)).build();

        } catch (IllegalArgumentException e) {
            // 400: the key or the email is not valid
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (UserNotFoundException e) {
            // 401: neutral message, so nobody can find out which emails have an account
            return error(Response.Status.UNAUTHORIZED, "Authentication required.");

        } catch (ForbiddenRoleException e) {
            // 403: an owner cannot join a roomshare
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (InvalidInvitationKeyException e) {
            // 404: no roomshare uses this key
            return error(Response.Status.NOT_FOUND, "Invalid invitation key.");

        } catch (AlreadyInRoomshareException e) {
            // 409: the roomie already has a roomshare or a pending request
            return error(Response.Status.CONFLICT, e.getMessage());

        } catch (RuntimeException e) {
            // 500: anything unexpected but  the client  gets a generic message
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }
}