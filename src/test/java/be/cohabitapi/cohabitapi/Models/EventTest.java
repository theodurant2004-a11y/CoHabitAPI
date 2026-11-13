package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.EventDAO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// These tests check the Event object alone: the constructor, the setters and their checks
// What happens between an event and its roomshare (the list, the search) is tested in RoomShareEventTest
@ExtendWith(MockitoExtension.class)
class EventTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 5);

    @Mock
    private EventDAO eventDAO;

    // A valid event, with no roomshare yet
    private Event event() {
        return new Event("Dinner", "Everybody cooks", "2026-11-05", "18:00", "21:00", "social");
    }


    // ------------------------- The constructor --------------------------------------------------

    @Test
    void constructor_theTextsOfTheFront_becomeObjects() {
        Event event = new Event("  Dinner ", "  Everybody cooks ", " 2026-11-05 ", " 18:00 ", "21:00", " SOCIAL ");

        assertEquals("Dinner", event.getTitle());               // the spaces are removed
        assertEquals("Everybody cooks", event.getDescription());
        assertEquals(DATE, event.getDate());                    // "2026-11-05" became a LocalDate
        assertEquals(LocalTime.of(18, 0), event.getStartTime()); // "18:00" became a LocalTime
        assertEquals(LocalTime.of(21, 0), event.getEndTime());
        assertEquals(EventType.SOCIAL, event.getType());        // "SOCIAL" became the value of the enum
        // No roomshare has added it to its list yet
        assertNull(event.getRoomshare());
    }

    @Test
    void constructor_noTimeAndNoDescription_isAccepted() {
        // The times and the description are optional
        Event event = new Event("Dinner", null, "2026-11-05", null, null, "social");

        assertNull(event.getDescription());
        assertNull(event.getStartTime());
        assertNull(event.getEndTime());
    }

    @Test
    void constructor_emptyTextsForTheTimes_areTheSameAsNoTime() {
        // What the front sends when the user chose no time
        Event event = new Event("Dinner", "", "2026-11-05", "", "", "social");

        assertNull(event.getDescription());
        assertNull(event.getStartTime());
        assertNull(event.getEndTime());
    }

    @Test
    void constructor_onlyAStartTime_isAccepted() {
        Event event = new Event("Dinner", null, "2026-11-05", "18:00", null, "social");

        assertEquals(LocalTime.of(18, 0), event.getStartTime());
        assertNull(event.getEndTime());
    }


    // ------------------------- The title --------------------------------------------------

    @Test
    void setTitle_blank_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Event("   ", null, "2026-11-05", null, null, "social"));
        assertThrows(IllegalArgumentException.class,
                () -> new Event(null, null, "2026-11-05", null, null, "social"));
    }

    @Test
    void setTitle_of101Characters_throwsException() {
        String tooLong = "a".repeat(101);

        assertThrows(IllegalArgumentException.class,
                () -> new Event(tooLong, null, "2026-11-05", null, null, "social"));
    }

    @Test
    void setTitle_of100Characters_isAccepted() {
        String title = "a".repeat(100);

        assertEquals(title, new Event(title, null, "2026-11-05", null, null, "social").getTitle());
    }


    // ------------------------- The description --------------------------------------------------

    @Test
    void setDescription_blank_becomesNull() {
        Event event = event();

        event.setDescription("   ");

        assertNull(event.getDescription());
    }

    @Test
    void setDescription_of256Characters_throwsException() {
        Event event = event();

        assertThrows(IllegalArgumentException.class, () -> event.setDescription("a".repeat(256)));
    }


    // ------------------------- The date --------------------------------------------------

    @Test
    void setDate_null_throwsException() {
        Event event = event();

        assertThrows(IllegalArgumentException.class, () -> event.setDate(null));
        // The old date did not change
        assertEquals(DATE, event.getDate());
    }

    @Test
    void constructor_missingDate_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Event("Dinner", null, null, null, null, "social"));
        assertThrows(IllegalArgumentException.class,
                () -> new Event("Dinner", null, "   ", null, null, "social"));
    }

    @Test
    void constructor_textThatIsNotADate_throwsException() {
        // The format of the front is always YYYY-MM-DD
        assertThrows(IllegalArgumentException.class,
                () -> new Event("Dinner", null, "05/11/2026", null, null, "social"));
        // A day that does not exist
        assertThrows(IllegalArgumentException.class,
                () -> new Event("Dinner", null, "2026-02-30", null, null, "social"));
    }


    // ------------------------- The times --------------------------------------------------

    @Test
    void setTimes_endBeforeOrEqualToTheStart_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Event("Dinner", null, "2026-11-05", "21:00", "18:00", "social"));
        assertThrows(IllegalArgumentException.class,
                () -> new Event("Dinner", null, "2026-11-05", "18:00", "18:00", "social"));
    }

    @Test
    void setTimes_textThatIsNotATime_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Event("Dinner", null, "2026-11-05", "6pm", null, "social"));
    }

    @Test
    void setTimes_secondsAreCut_becauseTheDbKeepsHoursAndMinutes() {
        Event event = new Event("Dinner", null, "2026-11-05", "18:00:45", null, "social");

        assertEquals(LocalTime.of(18, 0), event.getStartTime());
    }

    @Test
    void setTimes_theOldTimesAreReplaced() {
        Event event = event();

        event.setTimes(LocalTime.of(9, 0), LocalTime.of(10, 30));

        assertEquals(LocalTime.of(9, 0), event.getStartTime());
        assertEquals(LocalTime.of(10, 30), event.getEndTime());
    }


    // ------------------------- The type --------------------------------------------------

    @Test
    void setType_null_throwsException() {
        Event event = event();

        assertThrows(IllegalArgumentException.class, () -> event.setType(null));
    }

    @Test
    void constructor_missingOrUnknownType_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Event("Dinner", null, "2026-11-05", null, null, "unknown"));
        assertThrows(IllegalArgumentException.class,
                () -> new Event("Dinner", null, "2026-11-05", null, null, null));
    }

    @Test
    void setType_changesTheType() {
        Event event = event();

        event.setType(EventType.MEETING);

        assertEquals(EventType.MEETING, event.getType());
    }


    // ------------------------- The roomshare --------------------------------------------------

    @Test
    void setRoomshare_null_throwsException() {
        Event event = event();

        assertThrows(IllegalArgumentException.class, () -> event.setRoomshare(null));
    }


    // ------------------------- Save --------------------------------------------------

    @Test
    void create_eventOfARoomshare_asksTheDaoToSaveIt() {
        Owner owner = new Owner("Doe", "Alice", "owner@mail.com", "Test1234!");
        RoomShare roomshare = new RoomShare("ABC123", "Belleville", owner);
        Event event = event();
        // What RoomShare.addEvent does before saving: the event knows its roomshare
        event.setRoomshare(roomshare);

        event.create(eventDAO);

        verify(eventDAO).create(event);
    }

    @Test
    void create_eventWithoutRoomshare_throwsExceptionAndSavesNothing() {
        // The column roomshare_id cannot be null, so the insert would fail
        assertThrows(IllegalArgumentException.class, () -> event().create(eventDAO));

        verify(eventDAO, never()).create(any());
    }
}