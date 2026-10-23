package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.EXCEPTION.UnauthorizedUserException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "Roomie")
@PrimaryKeyJoinColumn(name = "user_id", referencedColumnName = "user_id")
public class Roomie extends Person{

    // ------------------------- Attributes and JPA Annotations --------------------------------------------------

    @ManyToOne
    @JoinColumn(name = "roomshare_id")
    private RoomShare roomshare;


    // ------------------------- Getter and Setter --------------------------------------------------

    public RoomShare getRoomshare() {
        return roomshare;
    }

    // Private visibility because we don't want that someone can access this setter from the outside
    void setRoomshare(RoomShare roomshare) {
        Objects.requireNonNull(roomshare);
        this.roomshare = roomshare;
    }


    // ------------------------- Constructors --------------------------------------------------

    protected Roomie() {
    }

    public Roomie(String lastName, String firstName, String email, String password) {
        super(lastName, firstName, email, password);
    }

    // Maybe useless, we will see in future
    // Same as the constructor above but with the possibility to join directly a room share
    public Roomie(String lastName, String firstName, String email, String passwordHash, RoomShare roomshare) {
        super(lastName, firstName, email, passwordHash);
        roomshare.addRoomie(this); // Add the roomie to the room share list but add the roomshare to the roomie too
    }


    // ------------------------- Utility methods --------------------------------------------------

    public static Roomie checkRightRoomie(String roomieEmail, PersonDAO dao) {
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
            throw new UnauthorizedUserException();

        return (Roomie)roomie;
    }
}
