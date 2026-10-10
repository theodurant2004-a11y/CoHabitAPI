package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.DAO;
import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import jakarta.persistence.*;
import org.mindrot.jbcrypt.BCrypt;
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

    //==============================CONSTRUCTOR==============================
    // Required by JPA/Hibernate to load a Person from the database.
    // Not used for JSON anymore (DTOs handle that).
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

    //==============================METHODS==============================
    public void create(PersonDAO dao){
            dao.create(this);
    }

    public static boolean existsByEmail(String email, PersonDAO dao){
        return dao.existsByEmail(email);
    }

    public static Person findByEmail(String email, PersonDAO dao){
        return dao.findByEmail(email);
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

    public static Person login(String email, String password, PersonDAO dao){

        if(email == null || password == null){
            return null;
        }

        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        String cleanPassword = password.trim();


        Person person = findByEmail(cleanEmail, dao);
        //If no user found in DB
        if(person == null){
            return null;
        }

        //password gestion
        if (!BCrypt.checkpw(cleanPassword, person.getPasswordHash())) {
            return null;
        }

        // verything is OK
        return person;

    }
}
