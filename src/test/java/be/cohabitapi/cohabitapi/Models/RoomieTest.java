package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.EXCEPTION.AlreadyInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.ForbiddenRoleException;
import be.cohabitapi.cohabitapi.EXCEPTION.InvalidInvitationKeyException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import be.cohabitapi.cohabitapi.EXCEPTION.JoinRequestNotFoundException;
import be.cohabitapi.cohabitapi.EXCEPTION.NoPendingRequestException;
import be.cohabitapi.cohabitapi.EXCEPTION.ForbiddenRoleException;
import be.cohabitapi.cohabitapi.EXCEPTION.ForbiddenRoleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

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
    private static final String OWNER_EMAIL = "alice@test.be"; // the email of the owner built by owner()

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


    //==============================requestToJoin: MORE ERRORS==============================

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("requestToJoin: null, empty or blank email -> IllegalArgumentException, DAOs never called")
    void requestToJoin_blankEmail_throws(String email) {
        // Act
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Roomie.requestToJoin(email, KEY, personDAO, roomShareDAO));

        // Assert: the message comes from Person.normalizeEmail and nothing was searched
        assertEquals("Email is required", e.getMessage());
        verifyNoInteractions(personDAO, roomShareDAO);
    }

    @Test
    @DisplayName("requestToJoin: roomie already accepted in a roomshare -> AlreadyInRoomshareException, nothing saved")
    void requestToJoin_alreadyAccepted_throws() {
        // Arrange: this roomie is already a member of a roomshare
        Roomie roomie = new Roomie("Martin", "Bob", EMAIL, PASSWORD, roomshare());
        when(personDAO.findByEmail(EMAIL)).thenReturn(roomie);
        when(roomShareDAO.findByInvitationKey(KEY)).thenReturn(roomshare());

        // Act + Assert
        assertThrows(AlreadyInRoomshareException.class,
                () -> Roomie.requestToJoin(EMAIL, KEY, personDAO, roomShareDAO));
        verify(personDAO, never()).update(any());
    }

    // ACCEPT A ROOMIE

    // small methods for these tests
    // In the db every person has an id, and belongsToOwner compares the ids
    // So here we give an id to the owner (1) and to the roomie (2)
    private Owner ownerWithId() {
        Owner owner = owner();
        owner.setIdPerson(1);
        return owner;
    }

    // A roomie (id 2) who sent a join request to the roomshare of this owner: he is PENDING
    private Roomie pendingRoomie(Owner owner) {
        Roomie roomie = roomie();
        roomie.setIdPerson(2);
        roomie.sendJoinRequest(new RoomShare(KEY, "Belleville", owner));
        return roomie;
    }

    // Another owner (id 99), for the security tests
    private Owner otherOwner() {
        Owner other = new Owner("Smith", "Carl", "carl@test.be", PASSWORD);
        other.setIdPerson(99);
        return other;
    }

    // acceptRequest
    // These tests do not need any mock: the methods only change the roomie in memory

    @Test
    @DisplayName("acceptRequest: pending roomie -> ACCEPTED and stays in the roomshare")
    void acceptRequest_pending_becomesAccepted() {
        // Arrange: a roomie who is waiting
        Roomie roomie = pendingRoomie(ownerWithId());
        RoomShare roomshare = roomie.getRoomshare();

        // Act: the owner accepts
        roomie.acceptRequest();

        // Assert: the status is ACCEPTED and he is still in the same roomshare
        assertSame(MembershipStatus.ACCEPTED, roomie.getStatusMember());
        assertSame(roomshare, roomie.getRoomshare());
    }

    // refuseRequest
    @Test
    @DisplayName("refuseRequest: pending roomie -> NONE and no roomshare anymore")
    void refuseRequest_pending_goesBackToNone() {
        // Arrange
        Roomie roomie = pendingRoomie(ownerWithId());

        // Act: the owner refuses
        roomie.refuseRequest();

        // Assert: back to the start
        assertSame(MembershipStatus.NONE, roomie.getStatusMember());
        assertNull(roomie.getRoomshare());
    }

    @Test
    @DisplayName("refuseRequest then sendJoinRequest: a refused roomie can ask again")
    void refuseRequest_thenNewRequest_isAllowed() {
        // Arrange: the request is refused
        Roomie roomie = pendingRoomie(ownerWithId());
        roomie.refuseRequest();

        // Act: the roomie asks again
        roomie.sendJoinRequest(roomshare());

        // Assert: he is waiting again
        assertSame(MembershipStatus.PENDING, roomie.getStatusMember());
    }

    @Test
    @DisplayName("accept/refuse: already accepted -> NoPendingRequestException")
    void answer_alreadyAccepted_throws() {
        // Arrange: the roomie is already accepted
        Roomie roomie = pendingRoomie(ownerWithId());
        roomie.acceptRequest();

        // Act + Assert: a second answer is refused
        assertThrows(NoPendingRequestException.class, () -> roomie.acceptRequest());
        assertThrows(NoPendingRequestException.class, () -> roomie.refuseRequest());
        // Nothing changed
        assertSame(MembershipStatus.ACCEPTED, roomie.getStatusMember());
    }

    @Test
    @DisplayName("accept/refuse: roomie without request -> NoPendingRequestException")
    void answer_noRequest_throws() {
        // Arrange: a new roomie, status NONE and no roomshare
        Roomie roomie = roomie();

        // Act + Assert: there is nothing to answer
        // roomie::acceptRequest is a short way to write () -> roomie.acceptRequest()
        assertThrows(NoPendingRequestException.class, roomie::acceptRequest);
        assertThrows(NoPendingRequestException.class, roomie::refuseRequest);
    }

    // belongsToOwner

    @Test
    @DisplayName("belongsToOwner: same id but another object -> true")
    void belongsToOwner_sameId_true() {
        // Arrange: in the real app the owner is loaded by another EntityManager:
        // it is another object in memory, but with the same id
        Roomie roomie = pendingRoomie(ownerWithId());
        Owner sameOwner = ownerWithId();

        // Act + Assert
        assertTrue(roomie.belongsToOwner(sameOwner));
    }

    @Test
    @DisplayName("belongsToOwner: another owner, null or no roomshare -> false")
    void belongsToOwner_otherOwner_false() {
        Roomie roomie = pendingRoomie(ownerWithId());

        // Another owner
        assertFalse(roomie.belongsToOwner(otherOwner()));
        // No owner at all
        assertFalse(roomie.belongsToOwner(null));
        // A roomie without roomshare does not belong to anybody
        assertFalse(roomie().belongsToOwner(ownerWithId()));
    }

    // answerJoinRequest (the whole use case)
    // Here we need the fake DAO: we tell it what to answer with when(...).thenReturn(...)

    @Test
    @DisplayName("answerJoinRequest: accept -> ACCEPTED and saved")
    void answerJoinRequest_accept_savesAcceptedRoomie() {
        // Arrange: the fake DAO knows the owner and the roomie
        Owner owner = ownerWithId();
        Roomie roomie = pendingRoomie(owner);
        when(personDAO.findByEmail(OWNER_EMAIL)).thenReturn(owner);
        when(personDAO.findById(2)).thenReturn(roomie);

        // Act: email with spaces and uppercase, the model cleans it
        Roomie result = Roomie.answerJoinRequest("  ALICE@test.be ", 2, true, personDAO);

        // Assert: it is the same roomie, he is accepted and saved
        assertSame(roomie, result);
        assertSame(MembershipStatus.ACCEPTED, result.getStatusMember());
        verify(personDAO).update(roomie);
    }

    @Test
    @DisplayName("answerJoinRequest: refuse -> NONE, no roomshare and saved")
    void answerJoinRequest_refuse_savesRoomieWithoutRoomshare() {
        // Arrange
        Owner owner = ownerWithId();
        Roomie roomie = pendingRoomie(owner);
        when(personDAO.findByEmail(OWNER_EMAIL)).thenReturn(owner);
        when(personDAO.findById(2)).thenReturn(roomie);

        // Act
        Roomie result = Roomie.answerJoinRequest(OWNER_EMAIL, 2, false, personDAO);

        // Assert
        assertSame(MembershipStatus.NONE, result.getStatusMember());
        assertNull(result.getRoomshare());
        verify(personDAO).update(roomie);
    }

    @Test
    @DisplayName("answerJoinRequest: no answer (null) -> IllegalArgumentException, db never used")
    void answerJoinRequest_missingAnswer_throws() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> Roomie.answerJoinRequest(OWNER_EMAIL, 2, null, personDAO));

        // The DAO was never called: the inputs are checked first
        verifyNoInteractions(personDAO);
    }

    @Test
    @DisplayName("answerJoinRequest: no roomie id -> IllegalArgumentException, db never used")
    void answerJoinRequest_missingRoomieId_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> Roomie.answerJoinRequest(OWNER_EMAIL, null, true, personDAO));

        verifyNoInteractions(personDAO);
    }

    @Test
    @DisplayName("answerJoinRequest: unknown owner -> UserNotFoundException")
    void answerJoinRequest_unknownOwner_throws() {
        // Arrange: nobody has this email
        when(personDAO.findByEmail("nobody@test.be")).thenReturn(null);

        // Act + Assert: the API turns it into a 401
        assertThrows(UserNotFoundException.class,
                () -> Roomie.answerJoinRequest("nobody@test.be", 2, true, personDAO));

        verify(personDAO, never()).update(any());
    }

    @Test
    @DisplayName("answerJoinRequest: a roomie tries to answer -> ForbiddenRoleException")
    void answerJoinRequest_roomieInsteadOfOwner_throws() {
        // Arrange: the email is the one of a roomie
        Roomie roomie = pendingRoomie(ownerWithId());
        when(personDAO.findByEmail(EMAIL)).thenReturn(roomie);

        // Act + Assert: the API turns it into a 403
        assertThrows(ForbiddenRoleException.class,
                () -> Roomie.answerJoinRequest(EMAIL, 2, true, personDAO));

        // Nothing changed and nothing is saved
        assertSame(MembershipStatus.PENDING, roomie.getStatusMember());
        verify(personDAO, never()).update(any());
    }

    @Test
    @DisplayName("answerJoinRequest: unknown roomie id -> JoinRequestNotFoundException")
    void answerJoinRequest_unknownRoomie_throws() {
        // Arrange: nobody has the id 42
        when(personDAO.findByEmail(OWNER_EMAIL)).thenReturn(ownerWithId());
        when(personDAO.findById(42)).thenReturn(null);

        // Act + Assert: the API turns it into a 404
        assertThrows(JoinRequestNotFoundException.class,
                () -> Roomie.answerJoinRequest(OWNER_EMAIL, 42, true, personDAO));

        verify(personDAO, never()).update(any());
    }

    @Test
    @DisplayName("answerJoinRequest: the id is an owner -> JoinRequestNotFoundException")
    void answerJoinRequest_idOfAnOwner_throws() {
        // Arrange: the id 1 is the owner himself, not a roomie
        Owner owner = ownerWithId();
        when(personDAO.findByEmail(OWNER_EMAIL)).thenReturn(owner);
        when(personDAO.findById(1)).thenReturn(owner);

        // Act + Assert
        assertThrows(JoinRequestNotFoundException.class,
                () -> Roomie.answerJoinRequest(OWNER_EMAIL, 1, true, personDAO));
    }

    @Test
    @DisplayName("answerJoinRequest: roomshare of another owner -> JoinRequestNotFoundException")
    void answerJoinRequest_roomshareOfAnotherOwner_throws() {
        // Arrange: Carl tries to accept Bob, but Bob asked to join the roomshare of Alice
        Roomie roomie = pendingRoomie(ownerWithId());
        when(personDAO.findByEmail("carl@test.be")).thenReturn(otherOwner());
        when(personDAO.findById(2)).thenReturn(roomie);

        // Act + Assert: the security test, the API turns it into a 404
        assertThrows(JoinRequestNotFoundException.class,
                () -> Roomie.answerJoinRequest("carl@test.be", 2, true, personDAO));

        // The other owner changed nothing
        assertSame(MembershipStatus.PENDING, roomie.getStatusMember());
        verify(personDAO, never()).update(any());
    }

    @Test
    @DisplayName("answerJoinRequest: request already answered -> NoPendingRequestException")
    void answerJoinRequest_alreadyAnswered_throws() {
        // Arrange: Bob is already accepted
        Owner owner = ownerWithId();
        Roomie roomie = pendingRoomie(owner);
        roomie.acceptRequest();
        when(personDAO.findByEmail(OWNER_EMAIL)).thenReturn(owner);
        when(personDAO.findById(2)).thenReturn(roomie);

        // Act + Assert: the API turns it into a 409
        assertThrows(NoPendingRequestException.class,
                () -> Roomie.answerJoinRequest(OWNER_EMAIL, 2, false, personDAO));

        verify(personDAO, never()).update(any());
    }

    // findPendingRequests

    @Test
    @DisplayName("findPendingRequests: owner -> the list given by the DAO")
    void findPendingRequests_owner_returnsTheList() {
        // Arrange: the DAO finds Bob in the requests of Alice (id 1)
        Owner owner = ownerWithId();
        Roomie roomie = pendingRoomie(owner);
        when(personDAO.findByEmail(OWNER_EMAIL)).thenReturn(owner);
        when(personDAO.findPendingRoomiesByOwner(1)).thenReturn(List.of(roomie));

        // Act
        List<Roomie> result = Roomie.findPendingRequests(OWNER_EMAIL, personDAO);

        // Assert: one request, and it is Bob
        assertEquals(1, result.size());
        assertSame(roomie, result.get(0));
    }

    @Test
    @DisplayName("findPendingRequests: no request -> empty list")
    void findPendingRequests_noRequest_returnsEmptyList() {
        // Arrange: the DAO finds nothing
        when(personDAO.findByEmail(OWNER_EMAIL)).thenReturn(ownerWithId());
        when(personDAO.findPendingRoomiesByOwner(1)).thenReturn(Collections.emptyList());

        // Act + Assert: an empty list, not an error
        assertTrue(Roomie.findPendingRequests(OWNER_EMAIL, personDAO).isEmpty());
    }

    @Test
    @DisplayName("findPendingRequests: a roomie -> ForbiddenRoleException")
    void findPendingRequests_roomie_throws() {
        // Arrange: the email is the one of a roomie
        when(personDAO.findByEmail(EMAIL)).thenReturn(roomie());

        // Act + Assert
        assertThrows(ForbiddenRoleException.class,
                () -> Roomie.findPendingRequests(EMAIL, personDAO));

        // The list was never asked to the db
        verify(personDAO, never()).findPendingRoomiesByOwner(any());
    }
}