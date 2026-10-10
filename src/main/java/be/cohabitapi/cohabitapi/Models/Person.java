package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.EXCEPTION.InvalidCredentialsException;
import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import jakarta.persistence.*;
import org.mindrot.jbcrypt.BCrypt;
import java.util.Locale;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Entity
@Table(name = "AppUser")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Person {

    //==============================Attributs==============================
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


    //==============================GETTER/SETTER==============================
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
        this.passwordHash = hashpassword(plainPassword);
    }
    //==============================CONSTRUCTOR==============================
    // Required by JPA/Hibernate to load a Person from the database.
    // Not used for JSON anymore (DTOs handle that).
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

    //==============================METHODS==============================
    public void create(PersonDAO dao){
        dao.create(this);
    }

    public static String hashpassword(String password){

        //Define a cost factor => Default is 10
        //The higher the value, the longer the hashing time and the more secure the hash
        //https://medium.com/@singhalabhay19/understanding-bcrypt-in-java-a-deep-dive-into-password-hashing-1b4362ccae94
        int log = 12;

        // Generate the salt
        //A salt is a random string of characters added to a password before it is encrypted or hashed.
        String salt = BCrypt.gensalt(log);

        //hash the password
        return BCrypt.hashpw(password, salt);

    }

    private static String normalizeEmail(String email){

        if(email == null || email.trim().isEmpty()){
            throw new IllegalArgumentException("Email is required");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizePassword(String password){

        if(password == null || password.trim().isEmpty()){
            throw new IllegalArgumentException("Password is required");
        }
        return password.trim();
    }

    public static boolean existsByEmail(String email, PersonDAO dao){
        return dao.existsByEmail(email);
    }

    public static Person findByEmail(String email, PersonDAO dao){
        return dao.findByEmail(email);
    }



    public static Person login(String email, String password, PersonDAO dao){

        String cleanEmail;
        String cleanPassword;

        // 1. Clean the inputs (empty → same error as a wrong login)
        try {
            cleanEmail = normalizeEmail(email);
            cleanPassword = normalizePassword(password);
        } catch (IllegalArgumentException e) {
            //I’m creating my own exception to return the same error message directly
            throw new InvalidCredentialsException();
        }

        // 2. Find the person and check the password
        Person person = findByEmail(cleanEmail, dao);

        if (person == null || !BCrypt.checkpw(cleanPassword, person.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        // 3. Everything is OK
        return person;
    }
}
