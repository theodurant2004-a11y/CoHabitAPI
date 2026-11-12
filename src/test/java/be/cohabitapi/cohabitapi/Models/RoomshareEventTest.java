package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.EventDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// These tests check what happens between a roomshare and its events:
// adding an event to the list, and reading the list to find the events of a period
// The DAO is a mock: these tests check the model, not the database
@ExtendWith(MockitoExtension.class)
class RoomShareEventTest {

    private RoomShare roomshare;

    @Mock
    private EventDAO eventDAO;

    @BeforeEach
    void createRoomshare() {
        Owner owner = new Owner("Doe", "Alice", "owner@mail.com", "Test1234!");
        roomshare = new RoomShare("ABC123", "Belleville", owner);
    }

    // An event of the given day, already added to the roomshare
    private Event eventOn(String date) {
        Event event = new Event("Dinner", null, date, null, null, "social");
        roomshare.addEvent(event, eventDAO);
        return event;
    }


    // ------------------------- addEvent --------------------------------------------------

    @Test
    void addEvent_putsTheEventInTheListAndSetsTheRoomshare() {
        Event event = new Event("Dinner", null, "2026-11-05", null, null, "social");

        roomshare.addEvent(event, eventDAO);

        assertTrue(roomshare.getEvents().contains(event));
        assertSame(roomshare, event.getRoomshare());
    }

    @Test
    void addEvent_savesTheEventWithItsRoomshare() {
        Event event = new Event("Dinner", null, "2026-11-05", null, null, "social");

        roomshare.addEvent(event, eventDAO);

        // The roomshare was set before the save, so the column roomshare_id is never null
        verify(eventDAO).create(event);
        assertSame(roomshare, event.getRoomshare());
    }

    @Test
    void addEvent_theSaveFails_theEventDoesNotStayInTheList() {
        Event event = new Event("Dinner", null, "2026-11-05", null, null, "social");
        doThrow(new RuntimeException("database down")).when(eventDAO).create(any());

        assertThrows(RuntimeException.class, () -> roomshare.addEvent(event, eventDAO));

        // The event is added to the list only once it is really in the db
        assertTrue(roomshare.getEvents().isEmpty());
    }

    @Test
    void addEvent_null_throwsExceptionAndSavesNothing() {
        assertThrows(IllegalArgumentException.class, () -> roomshare.addEvent(null, eventDAO));

        verify(eventDAO, never()).create(any());
    }

    @Test
    void getEvents_isReadOnly() {
        eventOn("2026-11-05");

        // The only way to add an event is addEvent, which also sets the roomshare of the event
        assertThrows(UnsupportedOperationException.class, () -> roomshare.getEvents().clear());
    }


    // ------------------------- findEvents --------------------------------------------------

    @Test
    void findEvents_keepsOnlyTheEventsOfThePeriod() {
        Event october = eventOn("2026-10-31");
        Event first = eventOn("2026-11-01");
        Event middle = eventOn("2026-11-15");
        Event last = eventOn("2026-11-30");
        Event december = eventOn("2026-12-01");

        List<Event> found = roomshare.findEvents("2026-11-01", "2026-11-30");

        // The two limits are included
        assertEquals(3, found.size());
        assertTrue(found.contains(first));
        assertTrue(found.contains(middle));
        assertTrue(found.contains(last));
        assertFalse(found.contains(october));
        assertFalse(found.contains(december));
    }

    @Test
    void findEvents_withoutAnEndDate_keepsEverythingFromTheStart() {
        eventOn("2026-10-31");
        eventOn("2026-11-15");
        eventOn("2027-05-01");

        // The front does not always send "to"
        assertEquals(2, roomshare.findEvents("2026-11-01", null).size());
        assertEquals(2, roomshare.findEvents("2026-11-01", "   ").size());
    }

    @Test
    void findEvents_theOldestIsFirst() {
        Event last = eventOn("2026-11-30");
        Event first = eventOn("2026-11-01");

        List<Event> found = roomshare.findEvents("2026-11-01", "2026-11-30");

        assertSame(first, found.get(0));
        assertSame(last, found.get(1));
    }

    @Test
    void findEvents_noEventInThePeriod_givesAnEmptyList() {
        eventOn("2026-11-15");

        assertTrue(roomshare.findEvents("2026-12-01", "2026-12-31").isEmpty());
    }

    @Test
    void findEvents_missingOrInvalidStartDate_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> roomshare.findEvents(null, "2026-11-30"));
        assertThrows(IllegalArgumentException.class, () -> roomshare.findEvents("   ", "2026-11-30"));
        assertThrows(IllegalArgumentException.class, () -> roomshare.findEvents("01/11/2026", "2026-11-30"));
    }

    @Test
    void findEvents_invalidEndDate_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> roomshare.findEvents("2026-11-01", "30/11/2026"));
    }

    @Test
    void findEvents_endBeforeTheStart_throwsException() {
        // This period can never match an event, so we refuse it instead of answering an empty list
        assertThrows(IllegalArgumentException.class, () -> roomshare.findEvents("2026-11-30", "2026-11-01"));
    }

    @Test
    void findEvents_readsTheListAndNotTheDao() {
        // The events are already loaded with the roomshare, so findEvents needs no DAO at all
        Event event = eventOn("2026-11-05");

        assertEquals(List.of(event), roomshare.findEvents("2026-11-01", "2026-11-30"));
    }
}