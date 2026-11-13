package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.DTO.*;
import be.cohabitapi.cohabitapi.EXCEPTION.NotInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import be.cohabitapi.cohabitapi.Models.Owner;
import be.cohabitapi.cohabitapi.Models.Person;
import be.cohabitapi.cohabitapi.Models.RoomShare;
import static be.cohabitapi.cohabitapi.API.APIResponse.error;
import static be.cohabitapi.cohabitapi.Models.RoomShare.generateInvitationKey;
import static be.cohabitapi.cohabitapi.Models.Owner.checkRightOwner;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Collections;

import be.cohabitapi.cohabitapi.EXCEPTION.AlreadyInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.ForbiddenRoleException;
import be.cohabitapi.cohabitapi.EXCEPTION.InvalidInvitationKeyException;
import be.cohabitapi.cohabitapi.Models.Roomie;
import java.util.Locale;

import be.cohabitapi.cohabitapi.EXCEPTION.JoinRequestNotFoundException;
import be.cohabitapi.cohabitapi.EXCEPTION.NoPendingRequestException;
import java.util.ArrayList;
import java.util.List;
import be.cohabitapi.cohabitapi.EXCEPTION.MemberNotFoundException;

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
        catch (ForbiddenRoleException e) {
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

    // ACCEPT A ROOMIE : the owner sees the requests waiting for his answer (Members page)
    // GET because we only read data, nothing changes in the db
    // The full URL is /api/roomshares/join-requests?ownerEmail=... (the class path + this path)
    // TO DO : remove the ownerEmail parameter when the session system is efficient
    @GET
    @Path("/join-requests")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPendingRequests(@QueryParam("ownerEmail") String ownerEmail) {
        // A GET has no body, so the email comes from the URL with @QueryParam

        try {
            // The model does the work (checks the owner, asks the DAO)
            List<Roomie> roomies = Roomie.findPendingRequests(ownerEmail, personDAO);

            // We answer with a list of small DTOs, never with the real models (they contain the password hash)
            List<PendingJoinResponse> resp = new ArrayList<>();
            for (Roomie roomie : roomies) {
                // One DTO for each roomie = one line of the list in the front
                resp.add(new PendingJoinResponse(
                        roomie.getIdPerson(),
                        roomie.getFirstName(),
                        roomie.getLastName(),
                        roomie.getEmail()));
            }

            // 200 OK, the JSON looks like {"joinRequests": [ ... ]}
            // An empty list is not an error: there is just no request for now
            return Response.ok(Collections.singletonMap("joinRequests", resp)).build();

        } catch (IllegalArgumentException e) {
            // 400: the email is missing or empty
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (UserNotFoundException e) {
            // 401: neutral message, so nobody can find out which emails have an account
            return error(Response.Status.UNAUTHORIZED, "Authentication required.");

        } catch (ForbiddenRoleException e) {
            // 403: a roomie cannot see the requests, only an owner
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (RuntimeException e) {
            // 500: anything unexpected (for example the db) but the client only gets a generic message
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    // ACCEPT A ROOMIE : the owner accepts or refuses the request of one roomie
    // PUT and not POST: we change something that already exists (the request), we don't create anything
    // The full URL is /api/roomshares/join-requests/{roomieId}, for example /api/roomshares/join-requests/2
    @PUT
    @Path("/join-requests/{roomieId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response answerJoinRequest(@PathParam("roomieId") Integer roomieId, AnswerJoinRequest req) {
        // @PathParam takes the {roomieId} part of the URL
        // req is the JSON body, Jackson turns it into the DTO

        // No body at all: 400
        if (req == null) return error(Response.Status.BAD_REQUEST, "JSON invalid.");

        try {
            // The model does all the work (checks, search, change of status, save)
            Roomie roomie = Roomie.answerJoinRequest(
                    req.getOwnerEmail(),
                    roomieId,
                    req.getAccept(),
                    personDAO);

            // We answer with a small DTO, never with the real model
            AnswerJoinResponse resp = new AnswerJoinResponse(
                    roomie.getIdPerson(),
                    roomie.getFirstName(),
                    roomie.getLastName(),
                    roomie.getStatusMember().name().toLowerCase(Locale.ROOT)); // ACCEPTED becomes "accepted"

            // 200 OK: the state of an existing roomie changed
            return Response.ok(Collections.singletonMap("joinRequest", resp)).build();

            // Each exception of the model becomes an HTTP code
            // The order matters: RuntimeException (the generic one) must be the last,
            // because all our exceptions extend it and it would catch everything
        } catch (IllegalArgumentException e) {
            // 400: the email, the id or the answer is missing (the message comes from the model)
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (UserNotFoundException e) {
            // 401: neutral message
            return error(Response.Status.UNAUTHORIZED, "Authentication required.");

        } catch (ForbiddenRoleException e) {
            // 403: only an owner can answer a request
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (JoinRequestNotFoundException e) {
            // 404: no request of this roomie in the roomshare of this owner
            return error(Response.Status.NOT_FOUND, e.getMessage());

        } catch (NoPendingRequestException e) {
            // 409: the request was already answered
            return error(Response.Status.CONFLICT, e.getMessage());

        } catch (RuntimeException e) {
            // 500: anything unexpected but the client only gets a generic message
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }
    // MEMBERS : the owner and the accepted roomies of the roomshare (Members page)
    // The owner AND the roomies can use it: everybody sees who lives in the roomshare
    // The full URL is /api/roomshares/members?email=... (the class path + this path)
    // TO DO : remove the email parameter when the session system is efficient
    @GET
    @Path("/members")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMembers(@QueryParam("email") String email) {
        // A GET has no body, so the email comes from the URL with @QueryParam

        try {
            // The model does the work (finds the user, his roomshare and the members)
            List<Person> members = RoomShare.findMembers(email, personDAO);

            // We answer with a list of small DTOs, never with the real models (they contain the password hash)
            List<MembersResponse> resp = new ArrayList<>();
            for (Person member : members) {
                // The role is "owner" or "roomie", like in the login answer
                String role = member instanceof Owner ? "owner" : "roomie";
                resp.add(new MembersResponse(
                        member.getIdPerson(),
                        member.getFirstName(),
                        member.getLastName(),
                        member.getEmail(),
                        role));
            }

            // 200 OK, the JSON looks like {"members": [ ... ]}, the owner is always the first one
            return Response.ok(Collections.singletonMap("members", resp)).build();

        } catch (IllegalArgumentException e) {
            // 400: the email is missing or empty
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (UserNotFoundException e) {
            // 401: neutral message, so nobody can find out which emails have an account
            return error(Response.Status.UNAUTHORIZED, "Authentication required.");

        } catch (NotInRoomshareException e) {
            // 403: a roomie who is not accepted yet cannot see the members
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (RuntimeException e) {
            // 500: anything unexpected but the client only gets a generic message
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    // REMOVE A ROOMIE : the owner removes an accepted roomie from his roomshare (button "Remove" of the Members page)
    // DELETE because the roomie is taken out of the roomshare (his account is NOT deleted, only his membership)
    // The full URL is /api/roomshares/members/{roomieId}?ownerEmail=..., for example /api/roomshares/members/2
    // TO DO : remove the ownerEmail parameter when the session system is efficient
    @DELETE
    @Path("/members/{roomieId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeMember(@PathParam("roomieId") Integer roomieId, @QueryParam("ownerEmail") String ownerEmail) {
        // @PathParam takes the {roomieId} part of the URL
        // A DELETE usually has no body, so the email comes from the URL with @QueryParam (like for the GET)

        try {
            // The model does all the work (checks, search, change of status, save)
            Roomie.removeMember(ownerEmail, roomieId, personDAO);

            // 204 NO CONTENT: it worked and there is nothing to send back
            // (the front loads the list of members again to show the new state)
            return Response.noContent().build();

            // Each exception of the model becomes an HTTP code
            // The order matters: RuntimeException (the generic one) must be the last
        } catch (IllegalArgumentException e) {
            // 400: the email or the id is missing (the message comes from the model)
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (UserNotFoundException e) {
            // 401: neutral message
            return error(Response.Status.UNAUTHORIZED, "Authentication required.");

        } catch (ForbiddenRoleException e) {
            // 403: only an owner can remove a roomie
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (MemberNotFoundException e) {
            // 404: this roomie is not a member of the roomshare of this owner
            return error(Response.Status.NOT_FOUND, e.getMessage());

        } catch (RuntimeException e) {
            // 500: anything unexpected but the client only gets a generic message
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }

}