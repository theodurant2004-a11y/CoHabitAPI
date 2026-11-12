package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.EXCEPTION.EmailAlreadyUsedException;
import be.cohabitapi.cohabitapi.EXCEPTION.InvalidCredentialsException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import jakarta.persistence.*;
import org.mindrot.jbcrypt.BCrypt;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

import java.nio.charset.StandardCharsets;


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

    // The expenses he has to receive
    @OneToMany(mappedBy = "creditor")
    private List<Expense> expensesToReceive = new ArrayList<>();


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
        String v = normalizeEmail(email);
        if(v.length() > 254 || !v.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Invalid email.");
        }
        this.email = v;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public List<Expense> getExpensesToReceive() {
        return expensesToReceive;
    }



    public void setPassword(String plainPassword) {
        plainPassword = normalizePassword(plainPassword);
        if (plainPassword.length() < 8
                || !plainPassword.matches("(?s).*[A-Z].*")
                || !plainPassword.matches("(?s).*[a-z].*")
                || !plainPassword.matches("(?s).*[0-9].*")
                || !plainPassword.matches("(?s).*[^A-Za-z0-9].*")) {
            throw new IllegalArgumentException("Password is not strong enough.");
        }
        if (plainPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password too long.");
        }
        this.passwordHash = hashPassword(plainPassword);
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
        setPassword(plainPassword);
    }

    // SIGNUP: same as above but also checks the confirm password
    protected Person(String lastName, String firstName, String email, String plainPassword, String confirmPassword){
        this(lastName, firstName, email, plainPassword);

        String cleanConfirm = confirmPassword == null ? "" : confirmPassword.trim();
        if(!plainPassword.trim().equals(cleanConfirm)){
            throw new IllegalArgumentException("Passwords do not match.");
        }
    }

    // a method to check if the field is blank
    private static String requireNotBlank(String value, String fieldName){
        if(value == null || value.trim().isEmpty()){
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    //==============================METHODS==============================
    // this method is to signup, we don't check the values because we already did it in the constructor
    public void create(PersonDAO dao){
        if(dao.existsByEmail(this.email)){
            throw new EmailAlreadyUsedException();
        }
        dao.create(this);
    }



    // What the others have to give me
    public double calculateToReceive() {
        return sumToReceiveInCents() / 100.0;
    }

    // Adds the expenses of my own list, in cents (whole numbers: no rounding problem)
    // Each expense says what it still counts for, so the settled ones are not added
    protected long sumToReceiveInCents() {
        long cents = 0;

        for (Expense expense : expensesToReceive) {
            cents += expense.getPendingSumInCents();
        }
        return cents;
    }

    // Abstract because a roomie and an owner don't calculate their balance the same way
    public abstract double calculateBalance();

    // the roomshare of this user (roomie or owner) with his events
    public abstract RoomShare findRoomshareWithEvents(RoomShareDAO dao);

    // Used when a new expense is created
    // Adds the expense to the list of what this person has to receive: he becomes its creditor
    // Always use this method to connect an expense and its creditor
    // the list and the expense are changed together, so they always say the same thing
    public void addExpenseToReceive(Expense expense) {
        if (expense == null)
            throw new IllegalArgumentException("The expense cannot be null");
        if (expense.getCreditor() != null)
            throw new IllegalArgumentException("The expense already has a creditor");
        // Nobody owes money to himself
        if (this.equals(expense.getDebtor()))
            throw new IllegalArgumentException("The debtor and the creditor cannot be the same person");

        expensesToReceive.add(expense);
        expense.setCreditor(this);
    }

    private static String hashPassword(String password){

        int log = 12;
        String salt = BCrypt.gensalt(log);
        return BCrypt.hashpw(password, salt);

    }

    protected static String normalizeEmail(String email){

        if(email == null || email.trim().isEmpty()){
            throw new IllegalArgumentException("Email is required");
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    protected static String normalizePassword(String password){

        if(password == null || password.trim().isEmpty()){
            throw new IllegalArgumentException("Password is required");
        }
        return password.trim();
    }

    public static Person findByEmail(String email, PersonDAO dao){
        return dao.findByEmail(email);
    }

    // static because it returns the user, the first object
    public static Person checkRightPerson(String email, PersonDAO dao){

        Person user = findByEmail(normalizeEmail(email), dao);

        if (user == null)
            throw new UserNotFoundException();

        return user;
    }

    public static Person findById(PersonDAO dao, int id) { return dao.findById(id); }

    public static Person login(String email, String password, PersonDAO dao){

        String cleanEmail;
        String cleanPassword;

        // Empty inputs give the same error as a wrong login
        try {
            cleanEmail = normalizeEmail(email);
            cleanPassword = normalizePassword(password);
        } catch (IllegalArgumentException e) {
            throw new InvalidCredentialsException();
        }

        Person person = findByEmail(cleanEmail, dao);

        if (person == null || !BCrypt.checkpw(cleanPassword, person.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return person;
    }

    public String getFullName() {
        return getFirstName() + " " + getLastName();
    }


    //--------------------------- OVERLOADS ---------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Person person = (Person) o;

        // A person is identified by the email
        return java.util.Objects.equals(this.email, person.email);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(email);
    }
}
