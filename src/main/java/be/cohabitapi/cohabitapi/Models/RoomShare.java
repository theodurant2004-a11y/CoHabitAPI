package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.DAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
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

    @OneToMany(mappedBy = "roomshare") // Make the bi-directionnal link with the roomie
    private final List<Roomie> roomies = new ArrayList<>();


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


    // ------------------------- Constructors --------------------------------------------------

    // Not in public so as not to allow outsiders to use this empty constructor
    // Not in private because Hibernate needs to use this empty constructor
    public RoomShare() {
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

    // Trim any string attribute and checks if they are null, empty or upper than the max length
    private static String normalizeStringAttribute(String attribute, String attributeName, int maxLength){
        if (attribute == null)
            throw new IllegalArgumentException("The " + attributeName + " cannot be null");

        String trimmed = attribute.trim();

        if (trimmed.isEmpty())
            throw new IllegalArgumentException("The " + attributeName + " cannot be empty");
        if (trimmed.length() > maxLength)
            throw new IllegalArgumentException("The " + attributeName + " cannot be longer than " + maxLength + " characters");

        return trimmed;
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


    // ------------------------- DAO methods --------------------------------------------------

    public void create(DAO<RoomShare> dao) { dao.create(this); }
}
