package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.DTO.SigninRequest;
import be.cohabitapi.cohabitapi.DTO.UserResponse;
import be.cohabitapi.cohabitapi.Models.Owner;
import be.cohabitapi.cohabitapi.Models.RoomShare;
import be.cohabitapi.cohabitapi.Models.Roomie;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OwnerLoginAPITest {

    private static final String EMAIL = "alice@test.be";
    private static final String PASSWORD = "Test1234!";

    @Mock
    private PersonDAO dao;              // Fake DAOs: no database operations are performed.

    @Mock
    private RoomShareDAO roomShareDAO;

    @InjectMocks
    private PersonAPI api;              // Real API; Mockito puts the fake DAOs into its @Inject fields.

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // The body of the response is {"user": UserResponse}
    private UserResponse userOf(Response response) {
        Map<?, ?> body = (Map<?, ?>) response.getEntity();
        return (UserResponse) body.get("user");
    }

    @Test
    @DisplayName("login: owner with a roomshare -> the response contains its name and its key")
    void login_ownerWithRoomshare_returnsNameAndKey() {
        // Arrange
        Owner alice = new Owner("Doe", "Alice", EMAIL, PASSWORD);
        RoomShare roomshare = new RoomShare("ABC123", "Belleville", alice);
        when(dao.findByEmail(EMAIL)).thenReturn(alice);
        when(roomShareDAO.findByOwner(alice)).thenReturn(roomshare);

        // Act
        Response response = api.login(new SigninRequest(EMAIL, PASSWORD));

        // Assert
        assertEquals(200, response.getStatus());
        UserResponse user = userOf(response);
        assertEquals("owner", user.getRole());
        assertEquals("Belleville", user.getRoomshareName());
        assertEquals("ABC123", user.getInvitationKey());
    }

    @Test
    @DisplayName("login: owner without roomshare -> name and key stay null")
    void login_ownerWithoutRoomshare_returnsNoRoomshare() {
        // Arrange
        Owner alice = new Owner("Doe", "Alice", EMAIL, PASSWORD);
        when(dao.findByEmail(EMAIL)).thenReturn(alice);
        when(roomShareDAO.findByOwner(alice)).thenReturn(null);

        // Act
        Response response = api.login(new SigninRequest(EMAIL, PASSWORD));

        // Assert
        assertEquals(200, response.getStatus());
        UserResponse user = userOf(response);
        assertNull(user.getRoomshareName());
        assertNull(user.getInvitationKey());
    }

    @Test
    @DisplayName("login: a roomie never asks the roomshare DAO and gets no key")
    void login_roomie_doesNotUseOwnerLogic() {
        // Arrange
        Roomie bob = new Roomie("Doe", "Bob", "bob@test.be", PASSWORD);
        when(dao.findByEmail("bob@test.be")).thenReturn(bob);

        // Act
        Response response = api.login(new SigninRequest("bob@test.be", PASSWORD));

        // Assert
        assertEquals(200, response.getStatus());
        assertNull(userOf(response).getInvitationKey());
        verifyNoInteractions(roomShareDAO);
    }
}