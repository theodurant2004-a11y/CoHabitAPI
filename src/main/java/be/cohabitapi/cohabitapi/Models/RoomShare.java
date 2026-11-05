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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roomshare_id")
    private Integer id;

    @Column(name = "roomshare_invitation_key", length = 50, nullable = false)
    private String invitationKey;

    @Column(name = "roomshare_name", length = 100, nullable = false)
    private String name;

    @Column(name = "roomshare_creation_date", nullable = false)
    private LocalDate creationDate;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Owner owner;

    @OneToMany(mappedBy = "roomshare")
    @OrderBy("idPerson") // Stable order: the split and the colors depend on it
    private List<Roomie> roomies = new ArrayList<>();


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
        Objects.requireNonNull(owner);
        this.owner = owner;
    }

    public List<Roomie> getRoomies() {
        return roomies;
    }


    // ------------------------- Constructors --------------------------------------------------

    // Only for Hibernate
    protected RoomShare() {
    }

    // Constructor used for the RoomShare creation form
    public RoomShare(String key, String name, Owner owner) {
        setInvitationKey(key);
        setName(name);
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

    // Generates an invitation key of 6 characters
    public static String generateInvitationKey(RoomShareDAO dao) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rnd = new SecureRandom();
        String generatedKey;

        // The key must be unique in the db
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


    // ------------------------- DAO methods --------------------------------------------------

    public void create(DAO<RoomShare> dao) { dao.create(this); }
}
