package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.EventDAO;
import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.DTO.CreateEventRequest;
import be.cohabitapi.cohabitapi.DTO.EventResponse;
import be.cohabitapi.cohabitapi.DTO.EventTypeResponse;
import be.cohabitapi.cohabitapi.EXCEPTION.NotInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import be.cohabitapi.cohabitapi.Models.Event;
import be.cohabitapi.cohabitapi.Models.EventType;
import be.cohabitapi.cohabitapi.Models.Person;
import be.cohabitapi.cohabitapi.Models.RoomShare;
import static be.cohabitapi.cohabitapi.API.APIResponse.error;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//  full URL /api/events
// The owner and the roomies of the roomshare can see and add events
// Each route finds the caller with his email, then the model gives the roomshare of this member
@Path("/events")
public class EventAPI {

    // The DAOs are injected by the server
    @Inject
    private PersonDAO personDAO;
    @Inject
    private RoomShareDAO roomShareDAO;
    @Inject
    private EventDAO eventDAO;

    // A GET has no body, so the email comes in the URL
    // TODO : remove email when the session system is efficient
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEvents(@QueryParam("email") String email,
                              @QueryParam("from") String from,
                              @QueryParam("to") String to) {
        try {
            // The user found with his email can be an owner or roomie
            Person member = Person.checkRightPerson(email, personDAO);

            // His roomshare with its events: each kind of user knows how to find it
            RoomShare roomshare = member.findRoomshareWithEvents(roomShareDAO);

            // The roomshare reads its own list of events
            List<Event> events = roomshare.findEvents(from, to);

            // We answer with DTOs
            List<EventResponse> response = new ArrayList<>();
            for (Event event : events) {
                response.add(toEventResponse(event));
            }
            return Response.ok(response).build();

        } catch (UserNotFoundException e) {
            // 401 unknown email
            return error(Response.Status.UNAUTHORIZED, e.getMessage());

        } catch (NotInRoomshareException e) {
            // 403 the user has no roomshare or his request is still pending
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (IllegalArgumentException e) {
            // 400 the email or a date is missing or has a wrong format
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (RuntimeException e) {
            // 500 anything unexpected
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    // TYPES : GET /api/events/types
    // The types for the buttons of the pop up so the code = value to send back and label = text to show
    @GET
    @Path("/types")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTypes() {
        List<EventTypeResponse> types = new ArrayList<>();
        // values() gives all the values of the enum in the order written in EventType
        for (EventType type : EventType.values()) {
            types.add(new EventTypeResponse(type.getCode(), type.getLabel()));
        }
        return Response.ok(types).build();
    }

    // ADD AN EVENT : POST /api/events
    // The event is added in the roomshare of the caller, the front never sends a roomshare id
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createEvent(CreateEventRequest req) {

        // No body at all => 400
        if (req == null) return error(Response.Status.BAD_REQUEST, "JSON invalid.");

        try {
            // The user found with his email (owner or roomie)
            Person member = Person.checkRightPerson(req.getRoomieEmail(), personDAO);

            //  His roomshare with its events
            RoomShare roomshare = member.findRoomshareWithEvents(roomShareDAO);

            // The model checks the values and saves
            Event event = roomshare.addEvent(
                    req.getTitle(),
                    req.getDescription(),
                    req.getDate(),
                    req.getStartTime(),
                    req.getEndTime(),
                    req.getType(),
                    eventDAO);

            // 201 CREATED a new event exists in the db
            return Response.status(Response.Status.CREATED)
                    .entity(Collections.singletonMap("event", toEventResponse(event)))
                    .build();

        } catch (UserNotFoundException e) {
            // 401 unknown email
            return error(Response.Status.UNAUTHORIZED, e.getMessage());

        } catch (NotInRoomshareException e) {
            // 403 the user has no roomshare or request still pending
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (IllegalArgumentException e) {
            // 400 a business rule is broken in the model (title, date, times, type...)
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (RuntimeException e) {
            // 500 anything unexpected, the client only gets a generic message
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    // Model => DTO
    // This method only copies the model into the DTO
    private EventResponse toEventResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                // LocalDate => "2026-11-05"
                event.getDate().toString(),
                // LocalTime => "18:00" or null if there is no time
                event.getStartTime() == null ? null : event.getStartTime().toString(),
                event.getEndTime() == null ? null : event.getEndTime().toString(),
                // SOCIAL => "social"
                event.getType().getCode());
    }
}