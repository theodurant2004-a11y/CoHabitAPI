package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.EXCEPTION.ForbiddenRoleException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import be.cohabitapi.cohabitapi.EXCEPTION.NotInRoomshareException;
import jakarta.persistence.*;

@Entity
@Table(name = "Owner")
@PrimaryKeyJoinColumn(name = "user_id", referencedColumnName = "user_id")
public class Owner extends Person{

    public Owner() {
    }

    public Owner(String lastName, String firstName, String email, String password) {
        super(lastName, firstName, email, password);
    }

    public static Owner checkRightOwner(String ownerEmail, PersonDAO dao) {
        String cleanEmail;

        // Using the Mother method to be sure that the email is in lower case and not empty
        cleanEmail = normalizeEmail(ownerEmail);

        // Returns a Person object if the email corresponds to someone
        Person owner = Person.findByEmail(cleanEmail, dao);

        // Throw a specific exception if the user sent is not found by the method above
        if(owner == null)
            throw new UserNotFoundException();

        // Throw a specific exception if the user role is not "owner"
        if(!(owner instanceof Owner))
            throw new ForbiddenRoleException();

        return (Owner)owner;
    }

    // to log in : the roomshare this owner already created or null if he has not created it yet
    // The relation owner -> roomshare is only stored on the roomshare side, so we ask the DAO
    public RoomShare findRoomshare(RoomShareDAO dao) {
        return dao.findByOwner(this);
    }

    // CALENDAR : the roomshare this owner created loaded with its events
    // The owner has no link to his roomshare (it is only stored on the roomshare side) so we need to ask the DAO
    @Override
    public RoomShare findRoomshareWithEvents(RoomShareDAO dao) {
        RoomShare roomshare = dao.findByOwner(this);

        // if the owner has not created his roomshare yet he has no calendar
        if (roomshare == null)
            throw new NotInRoomshareException();

        return roomshare;
    }

    // An owner never owes money, so his balance is only what he has to receive
    @Override
    public double calculateBalance() {
        return calculateToReceive();
    }
}