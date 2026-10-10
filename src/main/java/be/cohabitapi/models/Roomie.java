package be.cohabitapi.models;

import jakarta.persistence.*;

@Entity
@Table(name = "Roomie")
@PrimaryKeyJoinColumn(name = "user_id", referencedColumnName = "user_id")
public class Roomie extends Person{

    public Roomie() {
    }

    public Roomie(String lastName, String firstName, String email, String passwordHash) {
        super(lastName, firstName, email, passwordHash);
    }

    public Roomie(Integer idPerson, String lastName, String firstName, String email, String passwordHash) {
        super(idPerson, lastName, firstName, email, passwordHash);
    }

    @Override
    public String getRole() {
        return "roomie";
    }
}
