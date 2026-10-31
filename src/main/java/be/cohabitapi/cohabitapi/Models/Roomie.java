package be.cohabitapi.cohabitapi.Models;


import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.EXCEPTION.*;
import jakarta.persistence.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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

    // The tasks of this roomie (association with Task in the class diagram)
    // mappedBy: the Task_Roomie table is managed by the "roomies" attribute of Task, this side only reads it
    // No cascade: it is an association, not a composition (a task does not live inside a roomie)
    @ManyToMany(mappedBy = "roomies")
    private Set<Task> tasks = new HashSet<>();


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

    // Read only: a task is assigned with Task.assignRoomie (which checks the rules), never from here
    public Set<Task> getTasks(){
        return Collections.unmodifiableSet(tasks);
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

    // TASKS

    // True if this roomie is an accepted member of this roomshare
    // The roomie knows his status and his roomshare, so he answers himself (Task does not look inside him)
    // We compare the ids: the two roomshares can be two different instances of the same roomshare
    public boolean isAcceptedMemberOf(RoomShare roomshare) {
        return roomshare != null
                && this.statusMember == MembershipStatus.ACCEPTED
                && this.roomshare != null
                && this.roomshare.getId().equals(roomshare.getId());
    }

    // Finds a roomie by his email and checks that he is an accepted member of a roomshare
    // Same idea as Owner.checkRightOwner, used by every task use case (add, list, update, delete)
    public static Roomie checkAcceptedRoomie(String email, PersonDAO personDAO) {
        // Same cleaning as the login (400 if empty)
        String cleanEmail = Person.normalizeEmail(email);
        Person person = Person.findByEmail(cleanEmail, personDAO);

        // Nobody has this email (the API answers 401)
        if (person == null)
            throw new UserNotFoundException();

        // An owner cannot manage tasks (403)
        if (!(person instanceof Roomie))
            throw new ForbiddenRoleException();

        Roomie roomie = (Roomie) person;

        // No roomshare yet, or the join request is still pending (403)
        if (!roomie.isAcceptedMemberOf(roomie.getRoomshare()))
            throw new NotInRoomshareException();

        return roomie;
    }
}
