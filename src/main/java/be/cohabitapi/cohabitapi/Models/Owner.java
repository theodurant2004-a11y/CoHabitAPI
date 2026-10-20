package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.EXCEPTION.UnauthorizedUserException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
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
        // Returns a Person object if the email corresponds to someone
        Person owner = Person.findByEmail(ownerEmail, dao);

        // Throw a specific exception if the user sent is not found by the method above
        if(owner == null)
            throw new UserNotFoundException();

        // Throw a specific exception if the user role is not "owner"
        if(!(owner instanceof Owner))
            throw new UnauthorizedUserException();

        return (Owner)owner;
    }
}
