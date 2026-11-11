package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.EventDAO;
import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.DTO.CreateEventRequest;
import be.cohabitapi.cohabitapi.DTO.EventResponse;
import be.cohabitapi.cohabitapi.DTO.EventTypeResponse;
import be.cohabitapi.cohabitapi.Models.Event;
import be.cohabitapi.cohabitapi.Models.EventType;
import be.cohabitapi.cohabitapi.Models.Owner;
import be.cohabitapi.cohabitapi.Models.RoomShare;
import be.cohabitapi.cohabitapi.Models.Roomie;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// These tests check that EventAPI answers with the right HTTP code
// The business logic is tested in EventTest and RoomShareEventTest: here we only check
// the translation JSON => model => HTTP
// The DAOs are mocks (no database), but the roomshare, the members and the events are real objects
@ExtendWith(MockitoExtension.class)
class EventAPITest {

    private static final String ROOMIE_EMAIL = "me@mail.com";
    private static final String OWNER_EMAIL = "owner@mail.com";

    @Mock
    private PersonDAO personDAO;
    @Mock
    private RoomShareDAO roomShareDAO;
    @Mock
    private EventDAO eventDAO;
    @InjectMocks
    private EventAPI api;

    private Owner owner;
    private RoomShare roomshare;
    private Roomie me;

    // A roomshare with its owner and one accepted roomie
    @BeforeEach
    void setUp() {
        owner = new Owner("Doe", "Alice", OWNER_EMAIL, "Test1234!");
        roomshare = new RoomShare("ABC123", "Belleville", owner);

        // This constructor adds the roomie to the roomshare and makes him ACCEPTED
        me = new Roomie("Roomie", "Test", ROOMIE_EMAIL, "Test1234!", roomshare);

        // lenient() because some tests do not use this answer
        lenient().when(personDAO.findByEmail(ROOMIE_EMAIL)).thenReturn(me);
    }


    // ------------------------- Helpers --------------------------------------------------

    // An event of the given day, already in the roomshare
    private Event eventOn(String date) {
        Event event = new Event("Dinner", "Everybody cooks", date, "18:00", "21:00", "social");
        roomshare.addEvent(event, eventDAO);
        return event;
    }

    // A valid request: a dinner on the 5th of November
    private CreateEventRequest validRequest() {
        CreateEventRequest req = new CreateEventRequest();
        req.setRoomieEmail(ROOMIE_EMAIL);
        req.setTitle("Dinner");
        req.setDescription("Everybody cooks");
        req.setDate("2026-11-05");
        req.setStartTime("18:00");
        req.setEndTime("21:00");
        req.setType("social");
        return req;
    }

    // A roomie who sent a join request but is not accepted yet
    private Roomie pendingRoomie() {
        Roomie pending = new Roomie("Ding", "Pen", "pending@mail.com", "Test1234!");
        pending.sendJoinRequest(roomshare);
        return pending;
    }


    // ------------------------- GET : the events of a period --------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getEvents_roomieOfTheRoomshare_returns200WithTheEvents() {
        eventOn("2026-11-05");
        eventOn("2026-12-01");   // outside the period

        Response response = api.getEvents(ROOMIE_EMAIL, "2026-11-01", "2026-11-30");

        assertEquals(200, response.getStatus());
        List<EventResponse> body = (List<EventResponse>) response.getEntity();
        assertEquals(1, body.size());
        assertEquals("Dinner", body.get(0).getTitle());
        assertEquals("Everybody cooks", body.get(0).getDescription());
        assertEquals("2026-11-05", body.get(0).getDate());
        assertEquals("18:00", body.get(0).getStartTime());
        assertEquals("21:00", body.get(0).getEndTime());
        // The front receives the code of the type and chooses the icon itself
        assertEquals("social", body.get(0).getType());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getEvents_theOwner_seesTheSameEvents() {
        // The owner has no link to his roomshare, so the API asks the DAO for it
        eventOn("2026-11-05");
        when(personDAO.findByEmail(OWNER_EMAIL)).thenReturn(owner);
        when(roomShareDAO.findByOwner(owner)).thenReturn(roomshare);

        Response response = api.getEvents(OWNER_EMAIL, "2026-11-01", "2026-11-30");

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<EventResponse>) response.getEntity()).size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getEvents_noEventInThePeriod_returns200WithAnEmptyList() {
        Response response = api.getEvents(ROOMIE_EMAIL, "2026-11-01", "2026-11-30");

        assertEquals(200, response.getStatus());
        assertTrue(((List<EventResponse>) response.getEntity()).isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getEvents_withoutAnEndDate_returns200() {
        eventOn("2026-12-25");

        // The front does not always send "to"
        Response response = api.getEvents(ROOMIE_EMAIL, "2026-11-01", null);

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<EventResponse>) response.getEntity()).size());
    }

    @Test
    void getEvents_missingOrInvalidDates_returns400() {
        assertEquals(400, api.getEvents(ROOMIE_EMAIL, null, null).getStatus());
        assertEquals(400, api.getEvents(ROOMIE_EMAIL, "01/11/2026", null).getStatus());
        assertEquals(400, api.getEvents(ROOMIE_EMAIL, "2026-11-30", "2026-11-01").getStatus());
    }

    @Test
    void getEvents_emptyEmail_returns400() {
        // normalizeEmail refuses a null or blank email
        assertEquals(400, api.getEvents(null, "2026-11-01", null).getStatus());
        assertEquals(400, api.getEvents("   ", "2026-11-01", null).getStatus());
    }

    @Test
    void getEvents_unknownEmail_returns401() {
        when(personDAO.findByEmail("nobody@mail.com")).thenReturn(null);

        assertEquals(401, api.getEvents("nobody@mail.com", "2026-11-01", null).getStatus());
    }

    @Test
    void getEvents_roomieNotAcceptedYet_returns403() {
        when(personDAO.findByEmail("pending@mail.com")).thenReturn(pendingRoomie());

        assertEquals(403, api.getEvents("pending@mail.com", "2026-11-01", null).getStatus());
    }

    @Test
    void getEvents_ownerWithoutRoomshare_returns403() {
        // The owner has not created his roomshare yet, so he has no calendar
        when(personDAO.findByEmail(OWNER_EMAIL)).thenReturn(owner);
        when(roomShareDAO.findByOwner(owner)).thenReturn(null);

        assertEquals(403, api.getEvents(OWNER_EMAIL, "2026-11-01", null).getStatus());
    }

    @Test
    void getEvents_unexpectedError_returns500() {
        when(personDAO.findByEmail(ROOMIE_EMAIL)).thenThrow(new RuntimeException("database down"));

        assertEquals(500, api.getEvents(ROOMIE_EMAIL, "2026-11-01", null).getStatus());
    }


    // ------------------------- GET : the types --------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getTypes_returns200WithAllTheTypes() {
        Response response = api.getTypes();

        assertEquals(200, response.getStatus());
        List<EventTypeResponse> body = (List<EventTypeResponse>) response.getEntity();
        assertEquals(EventType.values().length, body.size());
        assertEquals(EventType.SOCIAL.getCode(), body.get(0).getCode());
        assertEquals(EventType.SOCIAL.getLabel(), body.get(0).getLabel());
    }


    // ------------------------- POST : add an event --------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void createEvent_validRequest_returns201AndSavesTheEvent() {
        Response response = api.createEvent(validRequest());

        assertEquals(201, response.getStatus());
        verify(eventDAO, times(1)).create(any(Event.class));

        // The event is in the list of the roomshare
        assertEquals(1, roomshare.getEvents().size());
        assertSame(roomshare, roomshare.getEvents().get(0).getRoomshare());

        // The answer carries the event that was created
        Map<String, EventResponse> body = (Map<String, EventResponse>) response.getEntity();
        assertEquals("Dinner", body.get("event").getTitle());
        assertEquals("2026-11-05", body.get("event").getDate());
    }

    @Test
    void createEvent_theOwnerCanAddAnEventToo() {
        when(personDAO.findByEmail(OWNER_EMAIL)).thenReturn(owner);
        when(roomShareDAO.findByOwner(owner)).thenReturn(roomshare);

        CreateEventRequest req = validRequest();
        req.setRoomieEmail(OWNER_EMAIL);

        assertEquals(201, api.createEvent(req).getStatus());
        assertEquals(1, roomshare.getEvents().size());
    }

    @Test
    void createEvent_noBody_returns400() {
        assertEquals(400, api.createEvent(null).getStatus());

        verify(eventDAO, never()).create(any());
    }

    @Test
    void createEvent_invalidValues_returns400AndSavesNothing() {
        // The event is built by its constructor: its setters refuse a wrong title, date, time or type
        CreateEventRequest blankTitle = validRequest();
        blankTitle.setTitle("   ");
        assertEquals(400, api.createEvent(blankTitle).getStatus());

        CreateEventRequest badDate = validRequest();
        badDate.setDate("05/11/2026");
        assertEquals(400, api.createEvent(badDate).getStatus());

        CreateEventRequest badTime = validRequest();
        badTime.setStartTime("6pm");
        assertEquals(400, api.createEvent(badTime).getStatus());

        CreateEventRequest endBeforeStart = validRequest();
        endBeforeStart.setEndTime("17:00");
        assertEquals(400, api.createEvent(endBeforeStart).getStatus());

        CreateEventRequest badType = validRequest();
        badType.setType("unknown");
        assertEquals(400, api.createEvent(badType).getStatus());

        verify(eventDAO, never()).create(any());
        assertTrue(roomshare.getEvents().isEmpty());
    }

    @Test
    void createEvent_unknownEmail_returns401() {
        CreateEventRequest req = validRequest();
        req.setRoomieEmail("nobody@mail.com");
        when(personDAO.findByEmail("nobody@mail.com")).thenReturn(null);

        assertEquals(401, api.createEvent(req).getStatus());
        verify(eventDAO, never()).create(any());
    }

    @Test
    void createEvent_roomieNotAcceptedYet_returns403() {
        CreateEventRequest req = validRequest();
        req.setRoomieEmail("pending@mail.com");
        when(personDAO.findByEmail("pending@mail.com")).thenReturn(pendingRoomie());

        assertEquals(403, api.createEvent(req).getStatus());
        verify(eventDAO, never()).create(any());
    }

    @Test
    void createEvent_unexpectedError_returns500() {
        doThrow(new RuntimeException("database down")).when(eventDAO).create(any());

        assertEquals(500, api.createEvent(validRequest()).getStatus());
    }
}