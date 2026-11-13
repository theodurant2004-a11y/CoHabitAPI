package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.TaskDAO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Unit tests of the Task model (no database needed)
// create and update use a fake TaskDAO (mock): we only check that they call it, or not
class TaskTest {

    // Objects shared by all the tests
    // Created once (@BeforeAll) because the password hash (BCrypt) is slow
    private static Owner owner;
    private static RoomShare roomshare;
    private static RoomShare otherRoomshare;
    private static Roomie accepted1;
    private static Roomie accepted2;
    private static Roomie acceptedOther;

    @BeforeAll
    static void setUp() {
        // The roomshare of the tests
        // No database here, so we give the ids ourselves (belongsTo and findRoomie compare the ids)
        owner = new Owner("Owner", "Unit", "owner.unit@test.be", "Test1234!");
        roomshare = new RoomShare("UNIT01", "unit roomshare", owner);
        roomshare.setId(1);

        // Another roomshare
        Owner otherOwner = new Owner("Other", "Unit", "owner.other.unit@test.be", "Test1234!");
        otherRoomshare = new RoomShare("UNIT02", "other unit roomshare", otherOwner);
        otherRoomshare.setId(2);

        // Accepted roomies (this constructor joins the roomshare directly as ACCEPTED)
        accepted1 = new Roomie("Accepted", "One", "accepted1.unit@test.be", "Test1234!", roomshare);
        accepted1.setIdPerson(10);
        accepted2 = new Roomie("Accepted", "Two", "accepted2.unit@test.be", "Test1234!", roomshare);
        accepted2.setIdPerson(20);

        // An accepted roomie of the other roomshare
        acceptedOther = new Roomie("Accepted", "Other", "accepted.other.unit@test.be", "Test1234!", otherRoomshare);
        acceptedOther.setIdPerson(40);
    }

    // A valid task, used by many tests
    private Task newTask() {
        return new Task("Clean the kitchen", "With soap", LocalDate.of(2026, 12, 15), TaskStatus.PENDING, roomshare);
    }

    // Dates relative to today, so the tests do not fail one day
    private static String tomorrow() {
        return LocalDate.now().plusDays(1).toString();
    }

    private static String yesterday() {
        return LocalDate.now().minusDays(1).toString();
    }


    // ------------------------- Constructor -------------------------

    @Test
    void constructor_validValues_setsEverything() {
        Task task = newTask();

        assertNull(task.getId());   // no id before the database gives one
        assertEquals("Clean the kitchen", task.getName());
        assertEquals("With soap", task.getDescription());
        assertEquals(LocalDate.of(2026, 12, 15), task.getDueDate());
        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertSame(roomshare, task.getRoomshare());
        assertTrue(task.getRoomies().isEmpty());
    }

    @Test
    void constructor_withoutRoomshare_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new Task("Clean", null, null, TaskStatus.PENDING, null));
    }

    @Test
    void constructor_withoutDueDateAndDescription_isAllowed() {
        Task task = new Task("Clean", null, null, TaskStatus.PENDING, roomshare);

        assertNull(task.getDescription());
        assertNull(task.getDueDate());
    }


    // ------------------------- Name -------------------------

    @Test
    void setName_isTrimmed() {
        Task task = newTask();
        task.setName("   Do the dishes   ");
        assertEquals("Do the dishes", task.getName());
    }

    @Test
    void setName_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> newTask().setName(null));
    }

    @Test
    void setName_onlySpaces_throws() {
        assertThrows(IllegalArgumentException.class, () -> newTask().setName("    "));
    }

    @Test
    void setName_100Characters_isAllowed() {
        Task task = newTask();
        task.setName("a".repeat(100));
        assertEquals(100, task.getName().length());
    }

    @Test
    void setName_101Characters_throws() {
        assertThrows(IllegalArgumentException.class, () -> newTask().setName("a".repeat(101)));
    }


    // ------------------------- Description -------------------------

    @Test
    void setDescription_isTrimmed() {
        Task task = newTask();
        task.setDescription("   Kitchen and hall   ");
        assertEquals("Kitchen and hall", task.getDescription());
    }

    @Test
    void setDescription_nullOrSpaces_meansNoDescription() {
        Task task = newTask();

        task.setDescription(null);
        assertNull(task.getDescription());

        task.setDescription("   ");
        assertNull(task.getDescription());
    }

    @Test
    void setDescription_255Characters_isAllowed() {
        Task task = newTask();
        task.setDescription("a".repeat(255));
        assertEquals(255, task.getDescription().length());
    }

    @Test
    void setDescription_256Characters_throws() {
        assertThrows(IllegalArgumentException.class, () -> newTask().setDescription("a".repeat(256)));
    }


    // ------------------------- Status and due date -------------------------

    @Test
    void setStatus_changesTheStatus() {
        Task task = newTask();
        task.setStatus(TaskStatus.COMPLETED);
        assertEquals(TaskStatus.COMPLETED, task.getStatus());
    }

    @Test
    void setStatus_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> newTask().setStatus(null));
    }

    @Test
    void setDueDate_null_removesTheDueDate() {
        Task task = newTask();
        task.setDueDate(null);
        assertNull(task.getDueDate());
    }


    // ------------------------- Roomies -------------------------

    @Test
    void getRoomies_isReadOnly() {
        Task task = newTask();
        // The roomies are only assigned when the task is created or updated
        assertThrows(UnsupportedOperationException.class, () -> task.getRoomies().add(accepted1));
    }


    // ------------------------- belongsTo -------------------------

    @Test
    void belongsTo_itsRoomshare_isTrue() {
        assertTrue(newTask().belongsTo(roomshare));
    }

    @Test
    void belongsTo_otherRoomshare_isFalse() {
        assertFalse(newTask().belongsTo(otherRoomshare));
    }

    @Test
    void belongsTo_null_isFalse() {
        assertFalse(newTask().belongsTo(null));
    }

    @Test
    void belongsTo_sameRoomshareButOtherInstance_isTrue() {
        // Two different objects for the same roomshare (same id), like when they come from two EntityManagers
        RoomShare sameRoomshare = new RoomShare("UNIT01", "unit roomshare", owner);
        sameRoomshare.setId(1);

        // The ids are compared, not the objects
        assertTrue(newTask().belongsTo(sameRoomshare));
    }


    // ------------------------- Constructor with the values of the front (add / update) -------------------------

    @Test
    void newTask_validValues_setsEverything() {
        Task task = new Task("Clean", "Kitchen", tomorrow(), null, List.of(10, 20), roomshare);

        assertEquals("Clean", task.getName());
        assertEquals("Kitchen", task.getDescription());
        assertEquals(LocalDate.now().plusDays(1), task.getDueDate());
        assertEquals(TaskStatus.PENDING, task.getStatus());   // pending by default
        assertEquals(2, task.getRoomies().size());
        assertTrue(task.getRoomies().contains(accepted1));
        assertTrue(task.getRoomies().contains(accepted2));
    }

    @Test
    void newTask_statusCompleted_isKept() {
        Task task = new Task("Clean", null, null, "completed", null, roomshare);
        assertEquals(TaskStatus.COMPLETED, task.getStatus());
    }

    @Test
    void newTask_unknownStatus_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new Task("Clean", null, null, "done", null, roomshare));
    }

    @Test
    void newTask_noDueDate_isAllowed() {
        Task task = new Task("Clean", null, "", null, null, roomshare);
        assertNull(task.getDueDate());
    }

    @Test
    void newTask_dueDateInThePast_isAllowedByTheConstructor() {
        // The constructor only checks the values: the date rule is in create and update
        Task task = new Task("Clean", null, yesterday(), null, null, roomshare);
        assertEquals(LocalDate.now().minusDays(1), task.getDueDate());
    }

    @Test
    void newTask_dueDateWrongFormat_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new Task("Clean", null, "15/12/2026", null, null, roomshare));
    }

    @Test
    void newTask_noRoomie_isAllowed() {
        Task task = new Task("Clean", null, null, null, List.of(), roomshare);
        assertTrue(task.getRoomies().isEmpty());
    }

    @Test
    void newTask_sameRoomieTwice_isAddedOnlyOnce() {
        Task task = new Task("Clean", null, null, null, List.of(10, 10), roomshare);
        // A Set: no duplicate (like the primary key of Task_Roomie)
        assertEquals(1, task.getRoomies().size());
    }

    @Test
    void newTask_roomieOfAnotherRoomshare_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new Task("Clean", null, null, null, List.of(40), roomshare));
    }

    @Test
    void newTask_unknownRoomieId_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new Task("Clean", null, null, null, List.of(999), roomshare));
    }

    @Test
    void newTask_nullRoomieId_throws() {
        // List.of refuses null, so we use Arrays.asList
        assertThrows(IllegalArgumentException.class,
                () -> new Task("Clean", null, null, null, Arrays.asList(10, null), roomshare));
    }

    @Test
    void newTask_withoutRoomshare_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new Task("Clean", null, null, null, null, null));
    }


    // ------------------------- create -------------------------

    @Test
    void create_validTask_isSaved() {
        TaskDAO taskDAO = mock(TaskDAO.class);
        Task task = new Task("Clean", null, tomorrow(), null, null, roomshare);

        task.create(taskDAO);

        verify(taskDAO).create(task);
    }

    @Test
    void create_withoutDueDate_isSaved() {
        TaskDAO taskDAO = mock(TaskDAO.class);
        Task task = new Task("Clean", null, null, null, null, roomshare);

        task.create(taskDAO);

        verify(taskDAO).create(task);
    }

    @Test
    void create_dueDateInThePast_throwsAndSavesNothing() {
        TaskDAO taskDAO = mock(TaskDAO.class);
        Task task = new Task("Clean", null, yesterday(), null, null, roomshare);

        assertThrows(IllegalArgumentException.class, () -> task.create(taskDAO));
        verify(taskDAO, never()).create(any());
    }


    // ------------------------- update -------------------------

    @Test
    void update_copiesEveryValueAndSaves() {
        TaskDAO taskDAO = mock(TaskDAO.class);
        Task task = new Task("Old name", "Old", tomorrow(), null, List.of(10), roomshare);
        String inTwoDays = LocalDate.now().plusDays(2).toString();
        Task changes = new Task("New name", "New", inTwoDays, "completed", List.of(20), roomshare);

        task.update(changes, taskDAO);

        assertEquals("New name", task.getName());
        assertEquals("New", task.getDescription());
        assertEquals(LocalDate.now().plusDays(2), task.getDueDate());
        assertEquals(TaskStatus.COMPLETED, task.getStatus());
        // The old roomie is replaced by the new one
        assertEquals(1, task.getRoomies().size());
        assertTrue(task.getRoomies().contains(accepted2));
        verify(taskDAO).update(task);
    }

    @Test
    void update_withoutRoomies_removesThemAll() {
        TaskDAO taskDAO = mock(TaskDAO.class);
        Task task = new Task("Clean", null, null, null, List.of(10, 20), roomshare);
        Task changes = new Task("Clean", null, null, null, null, roomshare);

        task.update(changes, taskDAO);

        assertTrue(task.getRoomies().isEmpty());
    }

    @Test
    void update_withoutStatus_meansPending() {
        TaskDAO taskDAO = mock(TaskDAO.class);
        Task task = new Task("Clean", null, null, "completed", null, roomshare);
        Task changes = new Task("Clean", null, null, null, null, roomshare);

        task.update(changes, taskDAO);

        // PUT replaces everything: no status sent = the default value
        assertEquals(TaskStatus.PENDING, task.getStatus());
    }

    @Test
    void update_keepTheSameLateDate_isAllowed() {
        TaskDAO taskDAO = mock(TaskDAO.class);
        // A task that is already late (like a task loaded from the db)
        Task task = new Task("Clean", null, yesterday(), null, null, roomshare);
        Task changes = new Task("New name", null, yesterday(), null, null, roomshare);

        task.update(changes, taskDAO);

        assertEquals("New name", task.getName());
        verify(taskDAO).update(task);
    }

    @Test
    void update_newDateInThePast_throwsAndChangesNothing() {
        TaskDAO taskDAO = mock(TaskDAO.class);
        Task task = new Task("Old name", null, tomorrow(), null, null, roomshare);
        Task changes = new Task("New name", null, yesterday(), null, null, roomshare);

        assertThrows(IllegalArgumentException.class, () -> task.update(changes, taskDAO));

        // The exception is thrown before the setters: the task did not change and nothing is saved
        assertEquals("Old name", task.getName());
        verify(taskDAO, never()).update(any());
    }
}