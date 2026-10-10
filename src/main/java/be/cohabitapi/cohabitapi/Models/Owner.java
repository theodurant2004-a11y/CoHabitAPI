package be.cohabitapi.cohabitapi.Models;

import jakarta.persistence.*;

@Entity
@Table(name = "Owner")
@PrimaryKeyJoinColumn(name = "user_id", referencedColumnName = "user_id")
public class Owner extends Person{

    public Owner() {
    }

    public Owner(String lastName, String firstName, String email, String passwordHash) {
        super(lastName, firstName, email, passwordHash);
    }

    public Owner(Integer idPerson, String lastName, String firstName, String email, String passwordHash) {
        super(idPerson, lastName, firstName, email, passwordHash);
    }
}
