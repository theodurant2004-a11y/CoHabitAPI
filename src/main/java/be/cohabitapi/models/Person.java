package be.cohabitapi.models;

import be.cohabitapi.dao.PersonDAO;
import jakarta.persistence.*;

@Entity
@Table(name = "AppUser")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer idPerson; //Integer allows us to have null values because before signup we don't have an id
    @Column(name = "user_lastname", nullable = false, length = 50)
    private String lastName;

    @Column(name = "user_firstname", nullable = false, length = 50)
    private String firstName;

    @Column(name = "user_email", nullable = false, length = 254)
    private String email;

    @Column(name = "user_password_hash", nullable = false, length = 255)
    private String passwordHash;

    public Integer getIdPerson() {
        return idPerson;
    }

    public void setIdPerson(Integer idPerson) {
        this.idPerson = idPerson;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }


    protected Person() {
    }

    protected Person(String lastName, String firstName, String email, String passwordHash) {
        this.lastName = lastName;
        this.firstName = firstName;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    protected Person(Integer idPerson, String lastName, String firstName, String email, String passwordHash){
        this(lastName, firstName, email, passwordHash);
        this.idPerson = idPerson;
    }

    public void create(PersonDAO dao){
            dao.create(this);
    }

    public static boolean existsByEmail(String email, PersonDAO dao){
        return dao.existsByEmail(email);
    }
}
