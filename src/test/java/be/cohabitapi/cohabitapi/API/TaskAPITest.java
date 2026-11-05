package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.TaskDAO;
import be.cohabitapi.cohabitapi.DTO.TaskCreateRequest;
import be.cohabitapi.cohabitapi.DTO.TaskListResponse;
import be.cohabitapi.cohabitapi.DTO.TaskResponse;
import be.cohabitapi.cohabitapi.DTO.TaskStatusRequest;
import be.cohabitapi.cohabitapi.DTO.TaskUpdateRequest;
import be.cohabitapi.cohabitapi.Models.Owner;
import be.cohabitapi.cohabitapi.Models.RoomShare;
import be.cohabitapi.cohabitapi.Models.Roomie;
import be.cohabitapi.cohabitapi.Models.Task;
import be.cohabitapi.cohabitapi.Models.TaskStatus;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
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

// The API doesn't contain business logic. It :
    //   1. reads the request, 2. calls the model, 3. turns the result or the exception into an HTTP code
// So these tests check that every situation gives the right HTTP code and the right message:
    // 200 OK, 201 created, 204 no content, 400 bad request, 401 not authenticated, 403 forbidden, 404 not found, 500 server error

// We call the methods of TaskAPI directly, like normal Java methods
// No Tomcat, no HTTP request, no db and the DAOs are fakes (mocks)

// @ExtendWith(MockitoExtension.class): creates the @Mock fields before each test and cleans them after
@ExtendWith(MockitoExtension.class)
class TaskAPITest {

    // Constants reused in the tests
    private static final String EMAIL = "bob@test.be";          // an accepted roomie
    private static final String OWNER_EMAIL = "alice@test.be";  // an owner
    private static final String PENDING_EMAIL = "tom@test.be";  // a pending roomie
    private static final Integer TASK_ID = 10;

    // Fake DAOs: no db. We tell them what to answer with when(...).thenReturn(...)
    @Mock
    private PersonDAO personDAO;

    @Mock
    private TaskDAO taskDAO;

    // @InjectMocks: Mockito creates a REAL TaskAPI and puts the fake DAOs inside it
    @InjectMocks
    private TaskAPI api;

    // Real model objects, created once because the password hash (BCrypt) is slow
    private static Owner owner;
    private static RoomShare roomshare;
    private static RoomShare otherRoomshare;
    private static Roomie roomie;         // accepted in roomshare
    private static Roomie pendingRoomie;  // waiting for the owner's answer

    @BeforeAll
    static void createObjects() {
        owner = new Owner("Doe", "Alice", OWNER_EMAIL, "Test1234!");

        // No db, so we give the ids ourselves (the model compares the ids of the roomshares)
        roomshare = new RoomShare("ABC123", "Belleville", owner);
        roomshare.setId(1);
        otherRoomshare = new RoomShare("XYZ789", "Other", owner);
        otherRoomshare.setId(2);

        // This constructor joins the roomshare directly as ACCEPTED
        roomie = new Roomie("Martin", "Bob", EMAIL, "Test1234!", roomshare);
        roomie.setIdPerson(350);

        // sendJoinRequest puts the roomie in PENDING
        pendingRoomie = new Roomie("Leroy", "Tom", PENDING_EMAIL, "Test1234!");
        pendingRoomie.sendJoinRequest(roomshare);
    }


    // small methods that avoid repeating the same lines in every test

    // A valid add request. In the real app Jackson builds it from the JSON
    private TaskCreateRequest createRequest(String email) {
        TaskCreateRequest req = new TaskCreateRequest();
        req.setName("Take out the trash");
        req.setRoomieEmail(email);
        return req;
    }

    // A task of the roomshare (as if it was loaded from the db)
    private Task taskOf(RoomShare rs) {
        return new Task("Clean the kitchen", null, null, TaskStatus.PENDING, rs);
    }

    // Reads the "message" field of an error response ({"message": "..."})
    private String messageOf(Response response) {
        return (String) ((Map<?, ?>) response.getEntity()).get("message");
    }

    // Reads the "task" field of a success response ({"task": {...}})
    private TaskResponse taskOf(Response response) {
        return (TaskResponse) ((Map<?, ?>) response.getEntity()).get("task");
    }


    //==============================ADD A TASK (POST)==============================

    @Test
    @DisplayName("add: valid request -> 201 and the task is saved")
    void create_valid_returns201() {
        when(personDAO.findByEmail(EMAIL)).thenReturn(roomie);

        try (Response response = api.createTask(createRequest(EMAIL))) {
            assertEquals(201, response.getStatus());
            assertEquals("Take out the trash", taskOf(response).getName());
            assertEquals("pending", taskOf(response).getStatus()); // pending by default
        }

        verify(taskDAO).create(any(Task.class));
    }

    @Test
    @DisplayName("add: null request -> 400, DAOs never called")
    void create_nullRequest_returns400() {
        try (Response response = api.createTask(null)) {
            assertEquals(400, response.getStatus());
        }
        verifyNoInteractions(personDAO, taskDAO);
    }

    @Test
    @DisplayName("add: missing name -> 400, nothing saved")
    void create_missingName_returns400() {
        when(personDAO.findByEmail(EMAIL)).thenReturn(roomie);
        TaskCreateRequest req = createRequest(EMAIL);
        req.setName(null);

        try (Response response = api.createTask(req)) {
            assertEquals(400, response.getStatus());
            assertEquals("The task name is required.", messageOf(response));
        }
        verify(taskDAO, never()).create(any());
    }

    @Test
    @DisplayName("add: unknown email -> 401 with a neutral message")
    void create_unknownUser_returns401() {
        when(personDAO.findByEmail(EMAIL)).thenReturn(null);

        try (Response response = api.createTask(createRequest(EMAIL))) {
            assertEquals(401, response.getStatus());
            assertEquals("Authentication required.", messageOf(response));
        }
        verify(taskDAO, never()).create(any());
    }

    @Test
    @DisplayName("add: owner -> 403")
    void create_owner_returns403() {
        when(personDAO.findByEmail(OWNER_EMAIL)).thenReturn(owner);

        try (Response response = api.createTask(createRequest(OWNER_EMAIL))) {
            assertEquals(403, response.getStatus());
        }
        verify(taskDAO, never()).create(any());
    }

    @Test
    @DisplayName("add: pending roomie -> 403")
    void create_pendingRoomie_returns403() {
        when(personDAO.findByEmail(PENDING_EMAIL)).thenReturn(pendingRoomie);

        try (Response response = api.createTask(createRequest(PENDING_EMAIL))) {
            assertEquals(403, response.getStatus());
            assertEquals("You must be an accepted member of a roomshare.", messageOf(response));
        }
        verify(taskDAO, never()).create(any());
    }

    @Test
    @DisplayName("add: database failure -> 500 without technical details")
    void create_databaseFails_returns500() {
        // thenThrow makes the fake DAO crash, like a lost Oracle connection
        when(personDAO.findByEmail(EMAIL)).thenThrow(new RuntimeException("ORA-17002 secret"));

        try (Response response = api.createTask(createRequest(EMAIL))) {
            assertEquals(500, response.getStatus());
            assertEquals("Server error", messageOf(response));
        }
    }


    //==============================LIST THE TASKS (GET)==============================

    @Test
    @DisplayName("list: accepted roomie -> 200 with the tasks and the roomies")
    void getTasks_valid_returns200() {
        when(personDAO.findByEmail(EMAIL)).thenReturn(roomie);
        when(taskDAO.findByRoomshare(roomshare)).thenReturn(List.of(taskOf(roomshare)));
        when(personDAO.findAcceptedRoomies(roomshare)).thenReturn(List.of(roomie));

        try (Response response = api.getTasks(EMAIL)) {
            assertEquals(200, response.getStatus());

            // No "task" wrapper here: the body is directly the TaskListResponse
            TaskListResponse body = (TaskListResponse) response.getEntity();
            assertEquals(1, body.getTasks().size());
            assertEquals(1, body.getRoomies().size());
        }
    }

    @Test
    @DisplayName("list: missing email -> 400, DAOs never called")
    void getTasks_missingEmail_returns400() {
        try (Response response = api.getTasks(null)) {
            assertEquals(400, response.getStatus());
        }
        verifyNoInteractions(personDAO, taskDAO);
    }


    //==============================UPDATE A TASK (PUT)==============================

    @Test
    @DisplayName("update: valid request -> 200 and the task is saved with the new values")
    void update_valid_returns200() {
        Task task = taskOf(roomshare);
        when(personDAO.findByEmail(EMAIL)).thenReturn(roomie);
        when(taskDAO.findById(TASK_ID)).thenReturn(task);

        TaskUpdateRequest req = new TaskUpdateRequest();
        req.setName("New name");
        req.setStatus("completed");
        req.setRoomieEmail(EMAIL);

        try (Response response = api.updateTask(TASK_ID, req)) {
            assertEquals(200, response.getStatus());
            assertEquals("New name", taskOf(response).getName());
            assertEquals("completed", taskOf(response).getStatus());
        }

        verify(taskDAO).update(task);
    }

    @Test
    @DisplayName("update: task not found -> 404, nothing saved")
    void update_taskNotFound_returns404() {
        when(personDAO.findByEmail(EMAIL)).thenReturn(roomie);
        when(taskDAO.findById(TASK_ID)).thenReturn(null);

        TaskUpdateRequest req = new TaskUpdateRequest();
        req.setName("New name");
        req.setStatus("pending");
        req.setRoomieEmail(EMAIL);

        try (Response response = api.updateTask(TASK_ID, req)) {
            assertEquals(404, response.getStatus());
        }
        verify(taskDAO, never()).update(any());
    }

    @Test
    @DisplayName("update: task of another roomshare -> 404 (not 403)")
    void update_taskOfOtherRoomshare_returns404() {
        when(personDAO.findByEmail(EMAIL)).thenReturn(roomie);
        when(taskDAO.findById(TASK_ID)).thenReturn(taskOf(otherRoomshare));

        TaskUpdateRequest req = new TaskUpdateRequest();
        req.setName("New name");
        req.setStatus("pending");
        req.setRoomieEmail(EMAIL);

        try (Response response = api.updateTask(TASK_ID, req)) {
            assertEquals(404, response.getStatus());
        }
        verify(taskDAO, never()).update(any());
    }


    //==============================CHECK / UNCHECK (PUT /status)==============================

    @Test
    @DisplayName("status: completed -> 200 and the task is checked")
    void changeStatus_completed_returns200() {
        Task task = taskOf(roomshare);
        when(personDAO.findByEmail(EMAIL)).thenReturn(roomie);
        when(taskDAO.findById(TASK_ID)).thenReturn(task);

        TaskStatusRequest req = new TaskStatusRequest();
        req.setStatus("completed");
        req.setRoomieEmail(EMAIL);

        try (Response response = api.changeStatus(TASK_ID, req)) {
            assertEquals(200, response.getStatus());
            assertEquals("completed", taskOf(response).getStatus());
        }

        verify(taskDAO).update(task);
    }

    @Test
    @DisplayName("status: unknown status -> 400, nothing saved")
    void changeStatus_unknownStatus_returns400() {
        when(personDAO.findByEmail(EMAIL)).thenReturn(roomie);
        when(taskDAO.findById(TASK_ID)).thenReturn(taskOf(roomshare));

        TaskStatusRequest req = new TaskStatusRequest();
        req.setStatus("done");
        req.setRoomieEmail(EMAIL);

        try (Response response = api.changeStatus(TASK_ID, req)) {
            assertEquals(400, response.getStatus());
            assertEquals("Invalid status.", messageOf(response));
        }
        verify(taskDAO, never()).update(any());
    }


    //==============================DELETE A TASK (DELETE)==============================

    @Test
    @DisplayName("delete: valid request -> 204 and the task is deleted")
    void delete_valid_returns204() {
        Task task = taskOf(roomshare);
        when(personDAO.findByEmail(EMAIL)).thenReturn(roomie);
        when(taskDAO.findById(TASK_ID)).thenReturn(task);

        try (Response response = api.deleteTask(TASK_ID, EMAIL)) {
            assertEquals(204, response.getStatus());
            assertNull(response.getEntity()); // 204: no body
        }

        verify(taskDAO).delete(task);
    }

    @Test
    @DisplayName("delete: task not found -> 404, nothing deleted")
    void delete_taskNotFound_returns404() {
        when(personDAO.findByEmail(EMAIL)).thenReturn(roomie);
        when(taskDAO.findById(TASK_ID)).thenReturn(null);

        try (Response response = api.deleteTask(TASK_ID, EMAIL)) {
            assertEquals(404, response.getStatus());
            assertEquals("The received task does not exist.", messageOf(response));
        }
        verify(taskDAO, never()).delete(any());
    }
}