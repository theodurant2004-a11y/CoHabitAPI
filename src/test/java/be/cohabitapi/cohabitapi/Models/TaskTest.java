package be.cohabitapi.cohabitapi.Models;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests of the Task model (no database needed)
// The use cases that need the DAOs (create, update, delete...) are tested with the .http files
class TaskTest {

    // Objects shared by all the tests
    // Created once (@BeforeAll) because the password hash (BCrypt) is slow
    private static Owner owner;
    private static RoomShare roomshare;
    private static RoomShare otherRoomshare;
    private static Roomie accepted1;
    private static Roomie accepted2;
    private static Roomie pending;
    private static Roomie withoutRoomshare;
    private static Roomie acceptedOther;

    @BeforeAll
    static void setUp() {
        // The roomshare of the tests
        // No database here, so we give the ids ourselves (isAcceptedMemberOf compares the ids)
        owner = new Owner("Owner", "Unit", "owner.unit@test.be", "Test1234!");
        roomshare = new RoomShare("UNIT01", "unit roomshare", owner);
        roomshare.setId(1);

        // Another roomshare
        Owner otherOwner = new Owner("Other", "Unit", "owner.other.unit@test.be", "Test1234!");
        otherRoomshare = new RoomShare("UNIT02", "other unit roomshare", otherOwner);
        otherRoomshare.setId(2);

        // Accepted roomies (this constructor joins the roomshare directly as ACCEPTED)
        accepted1 = new Roomie("Accepted", "One", "accepted1.unit@test.be", "Test1234!", roomshare);
        accepted2 = new Roomie("Accepted", "Two", "accepted2.unit@test.be", "Test1234!", roomshare);
        acceptedOther = new Roomie("Accepted", "Other", "accepted.other.unit@test.be", "Test1234!", otherRoomshare);

        // A roomie without roomshare (NONE)
        withoutRoomshare = new Roomie("Without", "Roomshare", "none.unit@test.be", "Test1234!");

        // A pending roomie (sendJoinRequest puts him in PENDING)
        pending = new Roomie("Pending", "Roomie", "pending.unit@test.be", "Test1234!");
        pending.sendJoinRequest(roomshare);
    }

    // A valid task, used by many tests
    private Task newTask() {
        return new Task("Clean the kitchen", "With soap", LocalDate.of(2026, 12, 15), TaskStatus.PENDING, roomshare);
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
        // The only way to add a roomie is assignRoomie
        assertThrows(UnsupportedOperationException.class, () -> task.getRoomies().add(accepted1));
    }

    @Test
    void assignRoomie_acceptedRoomieOfTheRoomshare_isAdded() {
        Task task = newTask();

        task.assignRoomie(accepted1);
        task.assignRoomie(accepted2);

        assertEquals(2, task.getRoomies().size());
        assertTrue(task.getRoomies().contains(accepted1));
        assertTrue(task.getRoomies().contains(accepted2));
    }

    @Test
    void assignRoomie_sameRoomieTwice_isAddedOnlyOnce() {
        Task task = newTask();

        task.assignRoomie(accepted1);
        task.assignRoomie(accepted1);

        // A Set: no duplicate (like the primary key of Task_Roomie)
        assertEquals(1, task.getRoomies().size());
    }

    @Test
    void assignRoomie_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> newTask().assignRoomie(null));
    }

    @Test
    void assignRoomie_pendingRoomie_throws() {
        assertThrows(IllegalArgumentException.class, () -> newTask().assignRoomie(pending));
    }

    @Test
    void assignRoomie_roomieWithoutRoomshare_throws() {
        assertThrows(IllegalArgumentException.class, () -> newTask().assignRoomie(withoutRoomshare));
    }

    @Test
    void assignRoomie_roomieOfAnotherRoomshare_throws() {
        assertThrows(IllegalArgumentException.class, () -> newTask().assignRoomie(acceptedOther));
    }

    @Test
    void assignRoomie_sameRoomshareButOtherInstance_isAdded() {
        // Two different objects for the same roomshare (same id), like when they come from two EntityManagers
        RoomShare sameRoomshare = new RoomShare("UNIT01", "unit roomshare", owner);
        sameRoomshare.setId(1);

        Task task = new Task("Clean", null, null, TaskStatus.PENDING, sameRoomshare);
        task.assignRoomie(accepted1);   // accepted1 belongs to the other instance

        // The ids are compared, not the objects
        assertTrue(task.getRoomies().contains(accepted1));
    }
}