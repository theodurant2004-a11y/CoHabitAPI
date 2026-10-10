package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.DTO.CreateRoomShareRequest;
import be.cohabitapi.cohabitapi.DTO.CreateRoomShareResponse;
import be.cohabitapi.cohabitapi.DTO.UserResponse;
import be.cohabitapi.cohabitapi.Models.Owner;
import be.cohabitapi.cohabitapi.Models.Person;
import be.cohabitapi.cohabitapi.Models.RoomShare;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Collections;

@Path("/roomshares")
public class RoomShareAPI {
    private final RoomShareDAO dao;

    public RoomShareAPI() { this(new RoomShareDAO());}

    public RoomShareAPI(RoomShareDAO dao){
        this.dao = dao;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createRoomShare(CreateRoomShareRequest req){

        // I use this DTO too but for the request
        // So, I think we could rename this DTO
        // as User or UserDTO
        UserResponse ownerDTO = req.getOwner();


        try {
            // Returns the Owner object if the email matches
            // Or null if not
            Owner owner = (Owner) Person.findByEmail(ownerDTO.getEmail(), new PersonDAO());
            if(owner == null)
            {
                // I know that this case will probably never appear
                // Because I test the owner who is already logged in
                // But if an injection through the backend
                // Without passing by the JS is possible
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Collections.singletonMap("message", "Inexistent owner specified."))
                        .build();
            }

            // Build a RoomShare object to insert its attributes in db
            RoomShare rs = new RoomShare(req.getName().trim(), owner);
            rs.create(dao);

            // Build of the response using DTO and not the real model
            CreateRoomShareResponse resp = new CreateRoomShareResponse(rs);

            return Response.status(Response.Status.CREATED)
                    .entity(Collections.singletonMap("roomshare", resp))
                    .build();
        }
        // I must talk to Théo about the exception manage,
        // because he created a specific class to do that
        catch(Exception e){
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("message", e.getMessage()))
                    .build();
        }
    }
}
