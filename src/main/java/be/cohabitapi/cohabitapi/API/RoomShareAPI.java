package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.DTO.CreateRoomShareRequest;
import be.cohabitapi.cohabitapi.DTO.CreateRoomShareResponse;
import be.cohabitapi.cohabitapi.EXCEPTION.UnauthorizedUserException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import be.cohabitapi.cohabitapi.Models.Owner;
import be.cohabitapi.cohabitapi.Models.Person;
import be.cohabitapi.cohabitapi.Models.RoomShare;
import static be.cohabitapi.cohabitapi.API.APIResponse.error;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.Random;

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
            // Returns a Person object if the email corresponds to someone
            Person user = Person.findByEmail(req.getOwnerEmail(), personDAO);

            // Throw a specific exception if the user sent is not found by the method above
            if(user == null)
                throw new UserNotFoundException();

            // Throw a specific exception if the user role is not "owner"
            if(!(user instanceof Owner))
                throw new UnauthorizedUserException();

            // Generate the invitation key of the room share
            String key = generateInvitationKey(roomShareDAO);

            // Build a RoomShare object to insert its attributes in db
            RoomShare rs = new RoomShare(key, req.getName(), (Owner) user);
            rs.create(roomShareDAO);

            // Build of the response using a DTO and not the real model
            CreateRoomShareResponse resp = new CreateRoomShareResponse(rs.getId(), rs.getInvitationKey(), rs.getName(), rs.getImage(), rs.getCreationDate(), rs.getOwner().getIdPerson());

            return Response.status(Response.Status.CREATED)
                    .entity(Collections.singletonMap("roomshare", resp))
                    .build();
        }
        catch (IllegalArgumentException e) {
            return error(Response.Status.BAD_REQUEST, e.getMessage());
        }
        catch (UserNotFoundException | UnauthorizedUserException e) {
            return error(Response.Status.UNAUTHORIZED, e.getMessage());
        }
        catch(RuntimeException e){
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }


    // We create our own method to generate de invitation key
    // We choose to have an invitation key of 6 characters
    // We put it in the API and not in the RoomShare model to respect the Inject of the RoomShareDAO
    private String generateInvitationKey(RoomShareDAO dao) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rnd = new SecureRandom();
        String generatedKey;

        // With this loop, the unique constraint is protected from duplicates
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(characters.charAt(rnd.nextInt(characters.length())));
            }
            generatedKey = sb.toString();
        } while (dao.keyIsAlreadyUsed(generatedKey));

        return generatedKey;
    }
}
