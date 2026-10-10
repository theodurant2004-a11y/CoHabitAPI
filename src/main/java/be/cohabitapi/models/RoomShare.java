package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.DAO;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Random;

@Entity
@Table(name = "roomshare")
public class RoomShare {
    // @Id specifies a primary key
    @Id
    // @GeneratedValue specifies the auto-increment constraint
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // @Column specifies which column of the table is involved
    @Column(name = "roomshare_id")
    private int id;

    // "length = ..." to match with the db
    @Column(name = "roomshare_invitation_key", length = 50, nullable = false)
    private String invitationKey;

    @Column(name = "roomshare_name", length = 100, nullable = false)
    private String name;

    @Column(name = "roomshare_image")
    private String image;

    @Column(name = "roomshare_creation_date", length = 255, nullable = false)
    private LocalDate creationDate;

    // Foreign key
    @ManyToOne // Specifies the relation like "Many RoomShares may belong to One Owner"
    @JoinColumn(name = "user_id", nullable = false)
    // Return the entire object, not only the id
    private Owner owner;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getInvitationKey() {
        return invitationKey;
    }

    // I ask in myself if it worth something to let this setter
    // Because, in my mind, the invitation key is never set out of this class
    public void setInvitationKey(String invitationKey) {
        this.invitationKey = invitationKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("The name cannot be null or empty");
        }
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creation_date) {
        this.creationDate = creation_date;
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        // Checks and throws an exception if the reference is null
        Objects.requireNonNull(owner);
        this.owner = owner;
    }

    public RoomShare() {
    }

    // Constructor used for the RoomShare creation form
    public RoomShare(String name, Owner owner) {
        setName(name);
        // LocalDate is used to make the SQL DATE parse easy
        // It's almost like a DateTime
        this.creationDate = LocalDate.now();
        this.invitationKey = generateInvitationKey();
        // The owner set is the user in the current session
        setOwner(owner);
    }

    // We create our own method to generate de invitation key
    // We choose to have an invitation key of 6 characters
    private String generateInvitationKey() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(characters.charAt(rnd.nextInt(characters.length())));
        }
        return sb.toString();
    }

    public void create(DAO<RoomShare> dao) { dao.create(this); }
}
