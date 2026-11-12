package be.cohabitapi.cohabitapi.Models;

import static be.cohabitapi.cohabitapi.Utils.VerificationOperation.normalizeStringAttribute;

import be.cohabitapi.cohabitapi.DAO.DAO;
import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.DAO.EventDAO;
import be.cohabitapi.cohabitapi.EXCEPTION.NotInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.NotMemberOfRoomShareException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import jakarta.persistence.*;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.*;

@Entity
@Table(name = "roomshare")
public class RoomShare {

    // ------------------------- Attributes and JPA Annotations --------------------------------------------------

    // @Id specifies a primary key
    @Id
    // @GeneratedValue specifies the auto-increment constraint
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // @Column specifies which column of the table is involved
    @Column(name = "roomshare_id")
    private Integer id;

    // "length = ..." to match with the db
    @Column(name = "roomshare_invitation_key", length = 50, nullable = false)
    private String invitationKey;

    @Column(name = "roomshare_name", length = 100, nullable = false)
    private String name;

    @Column(name = "roomshare_creation_date", nullable = false)
    private LocalDate creationDate;

    // Foreign key
    @ManyToOne // Specifies the relation like "Many RoomShares may belong to One Owner"
    @JoinColumn(name = "user_id", nullable = false)
    // Return the entire object, not only the id
    private Owner owner;

    @OneToMany(mappedBy = "roomshare")
    @OrderBy("idPerson") // Stable order: the split and the colors depend on it
    private List<Roomie> roomies = new ArrayList<>();

    // Tasks live and die with their roomshare
    // mappedBy is to say that the link is the roomshare attribute from Task
    // cascade all and orphanremoval it's used when a roomshare is deleted their tasks are too.
    @OneToMany(mappedBy = "roomshare", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();

    // Events live and die with their roomshare, its a composition
    @OneToMany(mappedBy = "roomshare", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Event> events = new ArrayList<>();

    // ------------------------- Getters and Setters --------------------------------------------------

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) { this.id = id; }

    public String getInvitationKey() {
        return invitationKey;
    }

    // Private visibility because we never have to set this attribute out of the model
    private void setInvitationKey(String invitationKey) {
        this.invitationKey = normalizeStringAttribute(invitationKey, "invitation key", 50);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = normalizeStringAttribute(name, "name", 100);
    }

    public LocalDate getCreationDate() { return creationDate; }

    // Private visibility because we never have to set this attribute out of the model
    private void setCreationDate(LocalDate creation_date) {
        if (creation_date == null)
            throw new IllegalArgumentException("The creation_date cannot be null");
        this.creationDate = creation_date;
    }

    public Owner getOwner() { return owner; }

    public void setOwner(Owner owner) {
        // Checks and throws an exception if the reference is null
        Objects.requireNonNull(owner);
        this.owner = owner;
    }

    public List<Roomie> getRoomies() {
        return roomies;
    }

    // unmodifiablelist return a read only list so we can't clear the list passing trough getTask
    public List<Task> getTasks() {return Collections.unmodifiableList(tasks); }

    // Read only list, same reason as getTasks
    public List<Event> getEvents() { return Collections.unmodifiableList(events); }


    // ------------------------- Constructors --------------------------------------------------

    // Only for Hibernate
    protected RoomShare() {
    }

    // Constructor used for the RoomShare creation form
    public RoomShare(String key, String name, Owner owner) {
        setInvitationKey(key);
        setName(name);
        // LocalDate is used to make the SQL DATE parse easy
        // It's almost like a DateTime
        setCreationDate(LocalDate.now());
        setOwner(owner);
    }


    // ------------------------- Utility methods --------------------------------------------------

    public static RoomShare findRoomShareByRoomie(PersonDAO personDAO, RoomShareDAO roomShareDAO, int userId)
    {
        Roomie roomie = Roomie.checkRightRoomie(personDAO, userId);
        if (roomie.getStatusMember() != MembershipStatus.ACCEPTED) throw new NotMemberOfRoomShareException();
        // We do not juste use roomie.getRoomShare(),
        // because we're not sure that
        // this is a whole object well-completed
        // So, we regenerate it with findById()
        return RoomShare.findById(roomShareDAO, roomie.getRoomshare().getId());
    }

    // We create our own method to generate de invitation key
    // We choose to have an invitation key of 6 characters
    public static String generateInvitationKey(RoomShareDAO dao) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rnd = new SecureRandom();
        String generatedKey;

        // With this loop, the unique constraint is protected from duplicates
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(characters.charAt(rnd.nextInt(characters.length())));
            }
            generatedKey = sb.toString();
        } while (dao.keyIsAlreadyUsed(generatedKey));

        return generatedKey;
    }

    // Always use this method to connect a roomie and a roomshare
    public void addRoomie(Roomie roomie) {
        if  (roomie == null)
            throw new IllegalArgumentException("The roomie cannot be null");
        if (roomies.contains(roomie))
            throw new IllegalArgumentException("The roomie already exists");
        if (roomie.getRoomshare() != null)
            throw new IllegalArgumentException("The roomie already belongs to a room share");
        roomies.add(roomie);
        roomie.setRoomshare(this);
    }

    // The pending roomies are in the list too, so we keep only the accepted ones
    public List<Roomie> getAcceptedRoomies() {
        List<Roomie> accepted = new ArrayList<>();

        for (Roomie roomie : roomies) {
            if (roomie.getStatusMember() == MembershipStatus.ACCEPTED)
                accepted.add(roomie);
        }

        return accepted;
    }

    // Looks for an accepted roomie of this roomshare with his id
    public Roomie findRoomie(Integer id) {
        if (id == null)
            throw new IllegalArgumentException("A roomie is required.");

        for (Roomie roomie : getAcceptedRoomies()) {
            if (id.equals(roomie.getIdPerson()))
                return roomie;
        }
        throw new IllegalArgumentException("A roomie of the list does not belong to your roomshare.");
    }


    // Same thing for several roomies: the ids sent by the front become objects
    // Each roomie is searched in the list of this roomshare, so they are all accepted roomies of it
    public List<Roomie> findRoomies(List<Integer> ids) {
        if (ids == null || ids.isEmpty())
            throw new IllegalArgumentException("Choose at least one roomie.");

        List<Roomie> found = new ArrayList<>();
        for (Integer id : ids) {
            // findRoomie refuses an empty id and an id that is not an accepted roomie of this roomshare
            found.add(findRoomie(id));
        }

        return found;
    }

    // ------------------------- DAO methods --------------------------------------------------

    public void create(DAO<RoomShare> dao) { dao.create(this); }

    // ------------------------- MEMBERS OF A ROOMSHARE --------------------------------------------------

    // The whole "see the members" use case. The API only calls this method
    // It returns the members of the roomshare of the user who asks: the owner first, then the accepted roomies
    // The owner and the roomies can both use it (the user who asks is in the list too)
    public static List<Person> findMembers(String email, PersonDAO personDAO) {
        // 1. Clean the email (IllegalArgumentException = 400 if it is empty) and find the user
        Person user = Person.findByEmail(Person.normalizeEmail(email), personDAO);

        // Nobody has this email (the API answers 401)
        if (user == null)
            throw new UserNotFoundException();

        // 2. Find the owner of the roomshare
        Owner owner;
        if (user instanceof Owner) {
            // The user is the owner himself
            owner = (Owner) user;
        } else {
            // The user is a roomie: he sees the members only if the owner accepted him (403)
            // A pending roomie is not a member yet, so he cannot see the others
            Roomie roomie = (Roomie) user;
            if (roomie.getStatusMember() != MembershipStatus.ACCEPTED)
                throw new NotInRoomshareException();

            // The roomshare and its owner are @ManyToOne, so they were loaded with the roomie (EAGER)
            owner = roomie.getRoomshare().getOwner();
        }

        // 3. Build the list: the owner first, then the accepted roomies given by the DAO
        List<Person> members = new ArrayList<>();
        members.add(owner);
        members.addAll(personDAO.findAcceptedRoomiesByOwner(owner.getIdPerson()));

        return members;
    }

    // ------------------------- EVENTS OF A ROOMSHARE --------------------------------------------------

    // ADD AN EVENT: the roomshare creates its event and adds it to its list, it<s a composition
    public void addEvent(Event event, EventDAO eventDAO) {

        // Nothing to add
        if (event == null)
            throw new IllegalArgumentException("The event cannot be null");

        // The event knows its roomshare (it needs it to be saved: roomshare_id cannot be null)
        event.setRoomshare(this);

        // The event saves itself and receives its id from the db
        event.create(eventDAO);

        // The roomshare knows its event
        // Last, so a save that fails does not leave an event in the list
        events.add(event);
    }

    // EVENTS LIST: the events of this roomshare from "from" to "to"
    // "from" is required and "to" is optional
    // The roomshare reads its own list "events", we do not ask the DAO because the objects already know the answer
    public List<Event> findEvents(String from, String to) {
        // Clean the dates: the text "2026-11-01" becomes a LocalDate
        LocalDate cleanFrom = Event.parseDate(from, "start date");

        // "to" is optional we only read it if the front sent something
        LocalDate cleanTo = null;
        if (to != null && !to.trim().isEmpty()) {
            cleanTo = Event.parseDate(to, "end date");

            // An end before the start can never match an event
            if (cleanTo.isBefore(cleanFrom))
                throw new IllegalArgumentException("The end date cannot be before the start date.");
        }

        // We go through the events of the roomshare and keep the ones between the two dates
        // isBefore and isAfter are strict, so we write "not before" and "not after" to include the two limits
        List<Event> found = new ArrayList<>();
        for (Event event : events) {
            // The event is on the start date or after it
            boolean afterStart = !event.getDate().isBefore(cleanFrom);
            // There is no end date, or the event is on the end date or before it
            boolean beforeEnd = cleanTo == null || !event.getDate().isAfter(cleanTo);

            if (afterStart && beforeEnd)
                found.add(event);
        }

        // Sorted by date, then by id for the events of the same day
        // An event that is not saved yet has no id, so we put it last instead of failing
        found.sort(Comparator.comparing(Event::getDate)
                .thenComparing(Event::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        return found;
    }

    public static RoomShare findById(RoomShareDAO dao, Integer id) { return dao.findById(id); }
}