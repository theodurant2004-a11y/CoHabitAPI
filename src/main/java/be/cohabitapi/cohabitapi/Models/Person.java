package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import jakarta.persistence.*;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;

@Entity
@Table(name = "AppUser")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer Person_id; //Integer allows us to have null values because before signup we don't have an id
    @Column(name = "user_lastname", nullable = false, length = 50)
    private String lastName;

    @Column(name = "user_firstname", nullable = false, length = 50)
    private String firstName;

    @Column(name = "user_email", nullable = false, length = 254)
    private String email;

    @Column(name = "user_password_hash", nullable = false, length = 255)
    private String passwordHash;

    public Integer getIdPerson() {
        return Person_id;
    }

    public void setIdPerson(Integer idPerson) {
        this.Person_id = Person_id;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        if(lastName.length() > 50){
            // Envoyer Exception
            throw new IllegalArgumentException("Lastname is maximum 50 characters");
        }
        if(lastName.isEmpty()) {
            // Envoyer Exception
            throw new IllegalArgumentException("All fields must be filled.");
        }
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        if(firstName.length() > 50){
            // Envoyer Exception
            throw new IllegalArgumentException("Firstname is maximum 50 characters.");
        }
        if(firstName.isEmpty()) {
            // Envoyer Exception
            throw new IllegalArgumentException("All fields must be filled");
        }
        this.firstName = firstName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if(email.isEmpty()){
            // Envoyer Exception
            throw new IllegalArgumentException("All fields must be filled.");
        }
        if(email.length() > 254) {
            // Envoyer Exception
            throw new IllegalArgumentException("Email too long");
        }
        if(!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            // Envoyer Exception
            throw new IllegalArgumentException("An email must contain an @.");
        }
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        if (passwordHash.length() < 8
                || !passwordHash.matches("(?s).*[A-Z].*")
                || !passwordHash.matches("(?s).*[a-z].*")
                || !passwordHash.matches("(?s).*[0-9].*")
                || !passwordHash.matches("(?s).*[^A-Za-z0-9].*")) {
            // Envoyer Exception
            throw new IllegalArgumentException("Password must contain at least 8 characters, a uppercase, a lowercase and a special character.");
        }
        if(passwordHash.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Pasword's bytes too long.");
        }
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
        this.Person_id = idPerson;
    }

    public void hashPassword(){
        String passwordHashed =
                BCrypt.hashpw(this.passwordHash, BCrypt.gensalt(12));

        this.passwordHash = passwordHashed;
    }

    public void create(PersonDAO dao){
            dao.create(this);
    }

    public static boolean existsByEmail(String email, PersonDAO dao){
        return dao.existsByEmail(email);
    }
}
