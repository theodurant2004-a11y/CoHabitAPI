package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import jakarta.persistence.*;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

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
        String v = requireNotBlank(lastName, "Lastname");
        if(v.length() > 50){
            throw new IllegalArgumentException("Lastname is maximum 50 characters.");
        }
        this.lastName = v;
    }

    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        String v = requireNotBlank(firstName, "Firstname");
        if(v.length() > 50) {
            throw new IllegalArgumentException("Firstname is maximum 50 characters.");
        }
        this.firstName = v;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        String v = requireNotBlank(email, "email");
        if(v.length() > 254 || !v.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Invalid email.");
        }
        this.email = v.toLowerCase(Locale.ROOT);
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String plainPassword) {
        plainPassword = requireNotBlank(plainPassword, "Password");
        if (plainPassword.length() < 8
                || !plainPassword.matches("(?s).*[A-Z].*")
                || !plainPassword.matches("(?s).*[a-z].*")
                || !plainPassword.matches("(?s).*[0-9].*")
                || !plainPassword.matches("(?s).*[^A-Za-z0-9].*")) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters, an uppercase letter, "
                            + "a lowercase letter, a number and a special character.");
        }
        if (plainPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password too long.");
        }
        this.passwordHash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }
    protected Person() {
    }

    protected Person(String lastName, String firstName, String email, String plainPassword) {
        setLastName(lastName);
        setFirstName(firstName);
        setEmail(email);
        setPasswordHash(plainPassword);
    }

    // We use this constructor when we take from DB so the paswword is already hashed, we don't need to verify it.
    protected Person(Integer idPerson, String lastName, String firstName, String email, String passwordHash){
        this.idPerson = idPerson;
        setLastName(lastName);
        setFirstName(firstName);
        setEmail(email);
        this.passwordHash = passwordHash;
    }

    // a methode to check if the field is blank
    private static String requireNotBlank(String value, String fieldName){
        if(value == null || value.trim().isEmpty()){
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    public void create(PersonDAO dao){
        dao.create(this);
    }

    public static boolean existsByEmail(String email, PersonDAO dao){
        return dao.existsByEmail(email);
    }

    public static Person findByEmail(String email, PersonDAO dao){
        return dao.findByEmail(email);
    }

    public static Person login(String email, String password, PersonDAO dao){
        if (email == null || password == null) return null;

        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        String cleanPassword = password.trim();

        Person person = findByEmail(cleanEmail, dao);
        if (person == null) return null;

        if (!BCrypt.checkpw(cleanPassword, person.getPasswordHash())) return null;
        return person;
    }

    public abstract String getRole();
}
