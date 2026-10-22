package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.EXCEPTION.AlreadyInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.ForbiddenRoleException;
import be.cohabitapi.cohabitapi.EXCEPTION.InvalidInvitationKeyException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// assertEquals, assertSame, assertNull, assertThrows : the "check" methods of JUnit
import static org.junit.jupiter.api.Assertions.*;

// any(): "any value", used when we do not care about the exact argument
import static org.mockito.ArgumentMatchers.any;
// when, verify, times, never, verifyNoInteractions...: the tools of Mockito
import static org.mockito.Mockito.*;

// A mock is a fake object, the real PersonDAO and RoomShareDAO talk to the db
// So we use fake DAOs. They do nothing by default, and we tell them what to answer
// This way we test ONLY the the business logic and not the db

// @ExtendWith(MockitoExtension.class): JUnit extension
// Before each test it creates the fields marked with @Mock, and after each test it cleans them
// It also warns us if a "when(...)" was written but never used
@ExtendWith(MockitoExtension.class)
class RoomieTest {

    // values reused in many tests
    private static final String EMAIL = "bob@test.be";
    private static final String PASSWORD = "Test1234!"; // Strong enough for the Person rules
    private static final String KEY = "ABC123";          // A valid invitation key (6 letters or digits)

    // @Mock: Mockito creates a fake PersonDAO and nothing is saved
    @Mock
    private PersonDAO personDAO;

    // @Mock: fake RoomShareDAO too
    @Mock
    private RoomShareDAO roomShareDAO;

    // small methods that build the objects we need in the tests, to not write "new Roomie(...)" line in every test
    // The objects are REAL (not mocks): we want to test the real logic of Roomie
    // The constructors validate the data and hash the password, like in the application
    private Roomie roomie() {
        return new Roomie("Martin", "Bob", EMAIL, PASSWORD);
    }

    private Owner owner() {
        return new Owner("Doe", "Alice", "alice@test.be", PASSWORD);
    }

    // A roomshare owned by the owner above, with the valid key
    private RoomShare roomshare() {
        return new RoomShare(KEY, "Belleville", owner());
    }


    // state of roomie

    @Test
    @DisplayName("new roomie: no roomshare and status NONE")
    void newRoomie_startsWithNone() {
        // create a roomie without any roomshare
        Roomie roomie = roomie();

        // Assert: he has no roomshare yet and his status is NONE
        // assertNull: the value must be null
        assertNull(roomie.getRoomshare());
        // assertSame: exactly the same object (perfect to compare enum values). Ckeck if both are  none
        assertSame(MembershipStatus.NONE, roomie.getStatusMember());
    }

    @Test
    @DisplayName("new roomie created directly in a roomshare -> status ACCEPTED")
    void newRoomieInRoomshare_isAccepted() {
        // Arrange: prepare a roomshare
        RoomShare roomshare = roomshare();

        // this constructor makes the roomie join directly
        Roomie roomie = new Roomie("Martin", "Bob", EMAIL, PASSWORD, roomshare);

        // Assert: he is directly an accepted member
        assertSame(MembershipStatus.ACCEPTED, roomie.getStatusMember());
    }

    //sendJoinRequest
    // These tests do not need any mock: sendJoinRequest only changes the roomie in memory

    @Test
    @DisplayName("sendJoinRequest: free roomie -> roomshare set and status PENDING")
    void sendJoinRequest_freeRoomie_becomesPending() {

        Roomie roomie = roomie();
        RoomShare roomshare = roomshare();

        // Act: the roomie asks to join
        roomie.sendJoinRequest(roomshare);

        // Assert: the roomshare is saved in the roomie and the status is PENDING
        assertSame(roomshare, roomie.getRoomshare());
        assertSame(MembershipStatus.PENDING, roomie.getStatusMember());
    }

    @Test
    @DisplayName("sendJoinRequest: null roomshare -> IllegalArgumentException")
    void sendJoinRequest_null_throws() {

        Roomie roomie = roomie();

        // Act + Assert
        // assertThrows(Exception.class, () -> code): the code inside "() -> ..." must throw this exception
        // If no exception is thrown or another one is detected  the test fails
        assertThrows(IllegalArgumentException.class, () -> roomie.sendJoinRequest(null));
        // The failed request must not have changed anything
        assertSame(MembershipStatus.NONE, roomie.getStatusMember());
    }

    @Test
    @DisplayName("sendJoinRequest: pending request already -> AlreadyInRoomshareException")
    void sendJoinRequest_alreadyPending_throws() {
        // the roomie already has a pending request
        Roomie roomie = roomie();
        roomie.sendJoinRequest(roomshare());

        // Act + Assert: a second request is refused and thé API turns this exception into a 409
        assertThrows(AlreadyInRoomshareException.class, () -> roomie.sendJoinRequest(roomshare()));
    }

    @Test
    @DisplayName("sendJoinRequest: already accepted in a roomshare -> AlreadyInRoomshareException")
    void sendJoinRequest_alreadyAccepted_throws() {
        // this roomie already belongs to a roomshare so the status is accepted
        Roomie roomie = new Roomie("Martin", "Bob", EMAIL, PASSWORD, roomshare());

        assertThrows(AlreadyInRoomshareException.class, () -> roomie.sendJoinRequest(roomshare()));
    }

    //requestToJoin: SUCCESS
    // requestToJoin is the whole use case. It uses the DAOs so here we use the mocks

    @Test
    @DisplayName("requestToJoin: valid email and key -> roomie PENDING and saved once")
    void requestToJoin_valid_savesRoomie() {
        // Arrange
        Roomie roomie = roomie();
        RoomShare roomshare = roomshare();

        // when(...).thenReturn(...): now we teach the fake DAO what to answer
        // "When someone asks personDAO to find EMAIL, answer with this roomie"
        // Without this line the fake DAO would answer null because it knows nobody
        when(personDAO.findByEmail(EMAIL)).thenReturn(roomie);
        // Same for the roomshare: we told him ; "when someone searches KEY, answer with this roomshare"
        when(roomShareDAO.findByInvitationKey(KEY)).thenReturn(roomshare);

        // we call the real method of Roomie with the fake DAOs
        Roomie result = Roomie.requestToJoin(EMAIL, KEY, personDAO, roomShareDAO);

        // Assert on the result
        assertSame(roomie, result);
        assertSame(MembershipStatus.PENDING, result.getStatusMember());
        assertSame(roomshare, result.getRoomshare());

        // verify(mock).method(argument): checks that the fake dao was calles like this
        // times(1) = exactly one time. So update(roomie) must have been called once (the save)
        verify(personDAO, times(1)).update(roomie);
    }

    @Test
    @DisplayName("requestToJoin: key in lowercase with spaces and email in uppercase -> both normalized")
    void requestToJoin_normalizesInputs() {
        // the DAOs only know the CLEAN values (lowercase email, uppercase key without spaces)
        when(personDAO.findByEmail(EMAIL)).thenReturn(roomie());
        when(roomShareDAO.findByInvitationKey(KEY)).thenReturn(roomshare());

        // we give messy values on purpose
        Roomie.requestToJoin("  BOB@Test.BE ", "  abc123 ", personDAO, roomShareDAO);

        // the DAOs received the clean values, so the normalization works
        // if it did not, the "when" above would not match and the method would throw an exception
        verify(personDAO).findByEmail(EMAIL);              // verify without times() = exactly once
        verify(roomShareDAO).findByInvitationKey(KEY);
    }

    //requestToJoin: ERRORS

    // @ParameterizedTest: the test runs once for each value below
    // @NullAndEmptySource: runs once with null and once with ""
    // @ValueSource: the other values: spaces only, too short, too long, bad character, accents
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "ABC12", "ABC1234", "ABC-12", "ÉÉÉÉÉÉ"})
    @DisplayName("requestToJoin: invalid key -> IllegalArgumentException, DAOs never called")
    void requestToJoin_invalidKey_throws(String key) {
        // the key is checked first, so an exception is thrown
        assertThrows(IllegalArgumentException.class,
                () -> Roomie.requestToJoin(EMAIL, key, personDAO, roomShareDAO));

        // verifyNoInteractions(mocks): checks that NOTHING was called on these fake DAOs
        // So with a bad key the database is never asked anything
        verifyNoInteractions(personDAO, roomShareDAO);
    }

    @Test
    @DisplayName("requestToJoin: unknown email -> UserNotFoundException, nothing saved")
    void requestToJoin_unknownEmail_throws() {
        // the DAO answers null = nobody has this email
        when(personDAO.findByEmail(EMAIL)).thenReturn(null);

        // Act + Assert ;the API turns this exception into a 401
        assertThrows(UserNotFoundException.class,
                () -> Roomie.requestToJoin(EMAIL, KEY, personDAO, roomShareDAO));

        // verify(mock, never()): this method must NEVER have been called
        // any(): "whatever the argument is"
        verify(personDAO, never()).update(any());
        // The key is not searched when the user does not exist
        verifyNoInteractions(roomShareDAO);
    }

    @Test
    @DisplayName("requestToJoin: user is an owner -> ForbiddenRoleException, nothing saved")
    void requestToJoin_owner_throws() {
        // Arrange: the email belongs to an Owner, and only roomies can join a roomshare
        when(personDAO.findByEmail("alice@test.be")).thenReturn(owner());

        // Act + Assert (the API turns this exception into a 403)
        assertThrows(ForbiddenRoleException.class,
                () -> Roomie.requestToJoin("alice@test.be", KEY, personDAO, roomShareDAO));
        verify(personDAO, never()).update(any());
    }

    @Test
    @DisplayName("requestToJoin: unknown key -> InvalidInvitationKeyException, nothing saved")
    void requestToJoin_unknownKey_throws() {
        // the roomie exists but no roomshare uses this key
        when(personDAO.findByEmail(EMAIL)).thenReturn(roomie());
        when(roomShareDAO.findByInvitationKey(KEY)).thenReturn(null);

        // Act + Assert (the API turns this exception into a 404)
        assertThrows(InvalidInvitationKeyException.class,
                () -> Roomie.requestToJoin(EMAIL, KEY, personDAO, roomShareDAO));
        verify(personDAO, never()).update(any());
    }

    @Test
    @DisplayName("requestToJoin: roomie already pending -> AlreadyInRoomshareException, nothing saved")
    void requestToJoin_alreadyPending_throws() {
        // this roomie already sent a request before
        Roomie roomie = roomie();
        roomie.sendJoinRequest(roomshare());
        when(personDAO.findByEmail(EMAIL)).thenReturn(roomie);
        when(roomShareDAO.findByInvitationKey(KEY)).thenReturn(roomshare());

        // Act + Assert (the API turns this exception into a 409)
        assertThrows(AlreadyInRoomshareException.class,
                () -> Roomie.requestToJoin(EMAIL, KEY, personDAO, roomShareDAO));
        // The second request must not be saved
        verify(personDAO, never()).update(any());
    }
}