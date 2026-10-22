package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.DTO.JoinRoomShareRequest;
import be.cohabitapi.cohabitapi.Models.MembershipStatus;
import be.cohabitapi.cohabitapi.Models.Owner;
import be.cohabitapi.cohabitapi.Models.RoomShare;
import be.cohabitapi.cohabitapi.Models.Roomie;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// The API doesn't contain business logic. It :
    //   1. read the request, 2. call the model, 3. turn the result or the exception into an HTTP code
// So these tests check that every situation gives the right HTTP code and the right message:
    // 200 OK, 400 bad request, 401 not authenticated, 403 forbidden, 404 not found, 409 conflict, 500 server error

// We call the method joinRoomShare() directly, like a normal Java method
// No Tomcat, no HTTP request, no db and the DAOs are fakes (mocks)

// @ExtendWith(MockitoExtension.class): creates the @Mock fields before each test and cleans them after
@ExtendWith(MockitoExtension.class)
class RoomShareAPITest {

    // Constants reused in the tests
    private static final String EMAIL = "bob@test.be";
    private static final String KEY = "ABC123"; // A valid invitation key

    // Fake DAOs: no db. We tell them what to answer with when(...).thenReturn(...)
    @Mock
    private PersonDAO personDAO;

    @Mock
    private RoomShareDAO roomShareDAO;

    // @InjectMocks: Mockito creates a REAL RoomShareAPI and puts the fake DAOs inside it
    // It finds the right fields by their type (PersonDAO, RoomShareDAO)
    // So the API uses our fake DAOs instead of the real ones
    @InjectMocks
    private RoomShareAPI api;


    // small methods that avoid repeating the same lines in every test

    // Builds the request object. In the real app Jackson builds it from the JSON sent by the browser
    private JoinRoomShareRequest request(String key, String email) {
        JoinRoomShareRequest req = new JoinRoomShareRequest();
        req.setInvitationKey(key);
        req.setRoomieEmail(email);
        return req;
    }

    // Reads the "message" field of an error response
    // The API puts a Map like {"message": "..."} in the response, so we read the "message" key of that map
    private String messageOf(Response response) {
        return (String) ((Map<?, ?>) response.getEntity()).get("message");
    }

    //SUCCESS

    @Test
    @DisplayName("join: valid request -> 200 and roomie saved as PENDING")
    void join_valid_returns200() {
        // a real roomie and a real roomshare, and the fake DAOs know them
        Roomie roomie = new Roomie("Martin", "Bob", EMAIL, "Test1234!");
        RoomShare roomshare = new RoomShare(KEY, "Belleville", new Owner("Doe", "Alice", "a@test.be", "Test1234!"));
        when(personDAO.findByEmail(EMAIL)).thenReturn(roomie);
        when(roomShareDAO.findByInvitationKey(KEY)).thenReturn(roomshare);

        // call the API like the browser would do
        // try (...) { }: "try-with-resources". The Response is closed automatically at the end of the block
        try (Response response = api.joinRoomShare(request(KEY, EMAIL))) {
            // Assert: HTTP code 200
            assertEquals(200, response.getStatus());
            // The body contains a "joinRequest" key (the data sent back to the front)
            assertTrue(((Map<?, ?>) response.getEntity()).containsKey("joinRequest"));
        }

        // the roomie is waiting for the owner's answer
        assertSame(MembershipStatus.PENDING, roomie.getStatusMember());
        // And the change was saved once through the DAO
        verify(personDAO).update(roomie);
    }

    //ERRORS

    @Test
    @DisplayName("join: invalid key -> 400, nothing saved")
    void join_invalidKey_returns400() {
        // The key "abc" is too short, so the model throws IllegalArgumentException and the API answers 400
        try (Response response = api.joinRoomShare(request("abc", EMAIL))) {
            assertEquals(400, response.getStatus());
        }
        // verify(mock, never()).method(any()): this method was never called, whatever the argument
        verify(personDAO, never()).update(any());
    }

    @Test
    @DisplayName("join: unknown user -> 401 with a neutral message")
    void join_unknownUser_returns401() {
        // Arrange: the fake DAO answers null = nobody has this email
        when(personDAO.findByEmail(EMAIL)).thenReturn(null);

        try (Response response = api.joinRoomShare(request(KEY, EMAIL))) {
            assertEquals(401, response.getStatus());
            // The message is neutral on purpose: it does not tell if the email exists or not
            assertEquals("Authentication required.", messageOf(response));
        }
    }

    @Test
    @DisplayName("join: owner -> 403")
    void join_owner_returns403() {
        // Arrange: this email belongs to an Owner, and only roomies can join a roomshare
        when(personDAO.findByEmail("a@test.be"))
                .thenReturn(new Owner("Doe", "Alice", "a@test.be", "Test1234!"));

        try (Response response = api.joinRoomShare(request(KEY, "a@test.be"))) {
            assertEquals(403, response.getStatus()); // 403 = forbidden
        }
    }

    @Test
    @DisplayName("join: unknown key -> 404 Invalid invitation key.")
    void join_unknownKey_returns404() {
        // Arrange: the roomie exists, but no roomshare has this key so the DAO answers null
        when(personDAO.findByEmail(EMAIL)).thenReturn(new Roomie("Martin", "Bob", EMAIL, "Test1234!"));
        when(roomShareDAO.findByInvitationKey(KEY)).thenReturn(null);

        try (Response response = api.joinRoomShare(request(KEY, EMAIL))) {
            assertEquals(404, response.getStatus()); // 404 = not found
            assertEquals("Invalid invitation key.", messageOf(response));
        }
    }

    @Test
    @DisplayName("join: roomie already pending -> 409")
    void join_alreadyPending_returns409() {
        // Arrange: this roomie already sent a request to this roomshare
        Roomie roomie = new Roomie("Martin", "Bob", EMAIL, "Test1234!");
        RoomShare roomshare = new RoomShare(KEY, "Belleville", new Owner("Doe", "Alice", "a@test.be", "Test1234!"));
        roomie.sendJoinRequest(roomshare);
        when(personDAO.findByEmail(EMAIL)).thenReturn(roomie);
        when(roomShareDAO.findByInvitationKey(KEY)).thenReturn(roomshare);

        try (Response response = api.joinRoomShare(request(KEY, EMAIL))) {
            assertEquals(409, response.getStatus()); // 409 = conflict
        }
        // The second request must not be saved
        verify(personDAO, never()).update(any());
    }

    @Test
    @DisplayName("join: database failure -> 500 without technical details")
    void join_databaseFails_returns500() {
        // Arrange: thenThrow makes the fake DAO crash, like a lost Oracle connection
        when(personDAO.findByEmail(EMAIL)).thenThrow(new RuntimeException("ORA-17002 secret"));

        try (Response response = api.joinRoomShare(request(KEY, EMAIL))) {
            assertEquals(500, response.getStatus()); // 500 = server error
            // The message is generic: the technical detailmust never reach the client
            assertEquals("Server error", messageOf(response));
        }
    }
}