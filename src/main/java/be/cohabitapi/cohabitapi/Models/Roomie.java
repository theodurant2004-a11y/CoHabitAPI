package be.cohabitapi.cohabitapi.Models;


import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.EXCEPTION.AlreadyInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.ForbiddenRoleException;
import be.cohabitapi.cohabitapi.EXCEPTION.InvalidInvitationKeyException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import be.cohabitapi.cohabitapi.EXCEPTION.NotInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.NoPendingRequestException;
import be.cohabitapi.cohabitapi.EXCEPTION.JoinRequestNotFoundException;
import jakarta.persistence.*;

import java.util.Locale;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "Roomie")
@PrimaryKeyJoinColumn(name = "user_id", referencedColumnName = "user_id")
public class Roomie extends Person{

    @ManyToOne
    @JoinColumn(name = "roomshare_id")
    private RoomShare roomshare;

    // The membership status
    // @Convert: JPA uses the converter inside the enum to store it as a number
    // A new roomie has no roomshare, so he starts with none
    // nullable = false because the roomie_status column is NOT NULL in the db
    @Convert(converter = MembershipStatus.DbConverter.class)
    @Column(name = "roomie_status", nullable = false)
    private MembershipStatus statusMember = MembershipStatus.NONE;

    public RoomShare getRoomshare() {
        return roomshare;
    }

    // Private visibility because we don't want that someone can access this setter from the outside
    void setRoomshare(RoomShare roomshare) {
        this.roomshare = roomshare;
    }

    // No setter for the status: they only change through the methods
    public MembershipStatus getStatusMember() {
        return statusMember;
    }

    protected Roomie() {
    }

    public Roomie(String lastName, String firstName, String email, String password) {
        super(lastName, firstName, email, password);
    }

    // Same as the constructor above but with the possibility to join directly a room share
    public Roomie(String lastName, String firstName, String email, String password, RoomShare roomshare) {
        super(lastName, firstName, email, password);
        roomshare.addRoomie(this); // Add the roomie to the room share list but add the roomshare to the roomie too
        // He joins directly, so he is an accepted member
        this.statusMember = MembershipStatus.ACCEPTED;
    }

    //  JOIN A ROOMSHARE

    // The roomie asks to join a roomshare and waits for the owner's answer but a roomie can have only one roomshare
    // The roomies list of the roomshare is not used on purpose: it is lazy and the roomshare
    // was loaded by another EntityManager, so touching the list would throw an Exception
    public void sendJoinRequest(RoomShare roomshare) {
        if (roomshare == null)
            throw new IllegalArgumentException("The roomshare cannot be null");

        // Already in a roomshare, or a request is already waiting: refuse and the API turns it into a 409
        if (this.roomshare != null || this.statusMember != MembershipStatus.NONE)
            throw new AlreadyInRoomshareException();

        this.roomshare = roomshare;
        this.statusMember = MembershipStatus.PENDING;
    }

    // The whole "join a roomshare" use case. The API only calls this method
    // It receives the DAOs as parameters
    public static Roomie requestToJoin(String email, String invitationKey,
                                       PersonDAO personDAO, RoomShareDAO roomShareDAO) {
        // 1. Clean the inputs
        // Both methods throw an IllegalArgumentException (400) if the value is not valid
        String cleanKey = normalizeInvitationKey(invitationKey);
        String cleanEmail = Person.normalizeEmail(email);

        // 2. Find the user by his email
        Person person = Person.findByEmail(cleanEmail, personDAO);

        // Nobody has this email (the API answers 401)
        if (person == null)
            throw new UserNotFoundException();

        // An owner is a Person but not a Roomie: only roomies can join a roomshare (403)
        if (!(person instanceof Roomie))
            throw new ForbiddenRoleException();

        // We know it is a Roomie, so we can use it as a Roomie
        Roomie roomie = (Roomie) person;

        // 3. Find the roomshare that uses this invitation key
        RoomShare found = roomShareDAO.findByInvitationKey(cleanKey);

        // No roomshare for this key (404)
        if (found == null)
            throw new InvalidInvitationKeyException();

        // 4. Change the state of the roomie in memory
        roomie.sendJoinRequest(found);

        //  save it in the db
        personDAO.update(roomie);

        return roomie;
    }

    // This method is used to find the roomie who is signed in and to check that he can use the expenses
    // We do this because an owner or an unknown email or a roomie not accepted yet must be refused
    public static Roomie findAcceptedMember(String email, PersonDAO personDAO) {
        String cleanEmail = normalizeEmail(email);
        Person person = Person.findByEmail(cleanEmail, personDAO);

        // Nobody has this email (the API answers 401)
        if (person == null)
            throw new UserNotFoundException();

        // An owner is not a roomie (403)
        if (!(person instanceof Roomie))
            throw new ForbiddenRoleException();

        Roomie roomie = (Roomie) person;

        // No roomshare, or the owner did not accept him yet (403)
        if (roomie.getRoomshare() == null || roomie.getStatusMember() != MembershipStatus.ACCEPTED)
            throw new NotInRoomshareException();

        return roomie;
    }

    // This method is used to get all the accepted roomies of my roomshare (me included)
    public List<Roomie> findRoomies(PersonDAO personDAO) {
        return personDAO.findAcceptedRoomies(roomshare);
    }

    // Cleans the invitation key and checks that it is valid
    private static String normalizeInvitationKey(String key) {
        if (key == null)
            throw new IllegalArgumentException("The invitation key is required.");

        // trim removes the spaces around
        // Locale.ROOT makes the result the same on every computer
        String clean = key.trim().toUpperCase(Locale.ROOT);

        // 6 characters, only letters A-Z and digits 0-9
        if (!clean.matches("[A-Z0-9]{6}"))
            throw new IllegalArgumentException("The invitation key must contain exactly 6 letters or digits.");

        return clean;
    }

    //  ACCEPT A ROOMIE

    // The owner accepts the request: the roomie becomes a real member of the roomshare
    public void acceptRequest() {
        // Only a request that is waiting can be accepted (the API turns it into a 409)
        if (this.statusMember != MembershipStatus.PENDING)
            throw new NoPendingRequestException();

        // The roomshare does not change: it was already set by sendJoinRequest
        this.statusMember = MembershipStatus.ACCEPTED;
    }

    // The owner refuses the request: the roomie goes back to the start, without roomshare
    public void refuseRequest() {
        // Only a request that is waiting can be refused (the API turns it into a 409)
        if (this.statusMember != MembershipStatus.PENDING)
            throw new NoPendingRequestException();

        // No roomshare anymore: sendJoinRequest refuses a new request if the roomshare is not null,
        // so without this line a refused roomie could never ask again
        this.roomshare = null;
        this.statusMember = MembershipStatus.NONE;
    }

    // Returns true if this roomie asked to join a roomshare of this owner
    // It is the security rule: an owner cannot answer the requests of another owner
    public boolean belongsToOwner(Owner owner) {
        // Each check protects the next one from a NullPointerException (&& stops at the first false)
        return owner != null
                && owner.getIdPerson() != null
                && this.roomshare != null
                && this.roomshare.getOwner() != null
                // We compare the ids and not the objects: the owner and the roomie were loaded
                // by different EntityManagers, so they are different objects in memory
                // Objects.equals because the ids are Integer (== between two Integer compares the objects)
                && Objects.equals(this.roomshare.getOwner().getIdPerson(), owner.getIdPerson());
    }

    // The whole "accept a roomie" use case. The API only calls this method
    // accept = true: the roomie is accepted, accept = false: the request is refused
    // It receives the DAO as a parameter, like requestToJoin
    public static Roomie answerJoinRequest(String ownerEmail, Integer roomieId, Boolean accept, PersonDAO personDAO) {
        // 1. Check the inputs before using the db (the API answers 400)
        if (roomieId == null)
            throw new IllegalArgumentException("The roomie id is required.");
        if (accept == null)
            throw new IllegalArgumentException("The answer (accept) is required.");
        // 2. Find the user who answers and check that he is an owner
        // checkRightOwner throws UserNotFoundException (401) or UnauthorizedUserException (403)
        Owner owner = Owner.checkRightOwner(ownerEmail, personDAO);
        // 3. Find the roomie with his id
        Person person = personDAO.findById(roomieId);

        // Nobody with this id, or it is not a roomie, or he asked to join the roomshare of another owner (404)
        // instanceof is false when person is null, so one condition covers the first two cases
        // With ||, belongsToOwner is only called when the person is a Roomie
        if (!(person instanceof Roomie) || !((Roomie) person).belongsToOwner(owner))
            throw new JoinRequestNotFoundException();

        // We know it is a Roomie, so we can use it as a Roomie
        Roomie roomie = (Roomie) person;

        // 4. Change the state of the roomie in memory (409 if the request is not pending anymore)
        if (accept) {
            roomie.acceptRequest();
        } else {
            roomie.refuseRequest();
        }

        // 5. Save it in the db, at the end so nothing is saved if a step failed
        personDAO.update(roomie);

        return roomie;
    }

    // The list of the requests waiting for an answer in the roomshare of this owner
    // The API only calls this method
    public static List<Roomie> findPendingRequests(String ownerEmail, PersonDAO personDAO) {
        // Only an owner can see the requests (401 / 403 like above)
        Owner owner = Owner.checkRightOwner(ownerEmail, personDAO);

        // The DAO does the search in the db
        return personDAO.findPendingRoomiesByOwner(owner.getIdPerson());
    }


}
