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
}