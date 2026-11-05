package be.cohabitapi.cohabitapi.Models;


import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.EXCEPTION.AlreadyInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.ForbiddenRoleException;
import be.cohabitapi.cohabitapi.EXCEPTION.InvalidInvitationKeyException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import jakarta.persistence.*;

import java.util.Objects;

import java.util.Locale;

@Entity
@Table(name = "Roomie")
@PrimaryKeyJoinColumn(name = "user_id", referencedColumnName = "user_id")
public class Roomie extends Person{

    // ------------------------- Attributes and JPA Annotations --------------------------------------------------

    @ManyToOne
    @JoinColumn(name = "roomshare_id")
    private RoomShare roomShare;

    // The membership status
    // @Convert: JPA uses the converter inside the enum to store it as a number
    // A new roomie has no roomshare, so he starts with none
    // nullable = false because the roomie_status column is NOT NULL in the db
    @Convert(converter = MembershipStatus.DbConverter.class)
    @Column(name = "roomie_status", nullable = false)
    private MembershipStatus statusMember = MembershipStatus.NONE;


    // ------------------------- Getter and Setter --------------------------------------------------

    public RoomShare getRoomShare() {
        return roomShare;
    }

    // Private visibility because we don't want that someone can access this setter from the outside
    void setRoomShare(RoomShare roomshare) {
        Objects.requireNonNull(roomshare);
        this.roomShare = roomshare;
    }


    // ------------------------- Constructors --------------------------------------------------

    // No setter for the status: they only change through the methods
    public MembershipStatus getStatusMember() {
        return statusMember;
    }

    protected Roomie() {
    }

    public Roomie(String lastName, String firstName, String email, String password) {
        super(lastName, firstName, email, password);
    }

    // Maybe useless, we will see in future
    // Same as the constructor above but with the possibility to join directly a room share
    public Roomie(String lastName, String firstName, String email, String password, RoomShare roomShare) {
        super(lastName, firstName, email, password);
        roomShare.addRoomie(this); // Add the roomie to the room share list but add the roomshare to the roomie too
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
        if (this.roomShare != null || this.statusMember != MembershipStatus.NONE)
            throw new AlreadyInRoomshareException();

        this.roomShare = roomshare;
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


    // ------------------------- Utility methods --------------------------------------------------

    public static Roomie checkRightRoomie(PersonDAO dao, String roomieEmail) {
        String cleanEmail;

        // Using the Mother method to be sure that the email is in lower case and not empty
        cleanEmail = normalizeEmail(roomieEmail);

        // Returns a Person object if the email corresponds to someone
        Person roomie = Person.findByEmail(cleanEmail, dao);

        // Throw a specific exception if the user sent is not found by the method above
        if(roomie == null)
            throw new UserNotFoundException();

        // Throw a specific exception if the user role is not "roomie"
        if(!(roomie instanceof Roomie))
            throw new ForbiddenRoleException();

        return (Roomie)roomie;
    }

    public static Roomie checkRightRoomie(PersonDAO dao, int userId) {
        // Returns a Person object if the email corresponds to someone
        Person roomie = Person.findById(dao, userId);

        // Throw a specific exception if the user role is not "roomie"
        if(!(roomie instanceof Roomie))
            throw new ForbiddenRoleException();

        return (Roomie)roomie;
    }
}
