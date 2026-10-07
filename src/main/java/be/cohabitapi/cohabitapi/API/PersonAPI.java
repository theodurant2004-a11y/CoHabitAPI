package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.Models.Owner;
import be.cohabitapi.cohabitapi.Models.Person;
import be.cohabitapi.cohabitapi.Models.Roomie;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.mindrot.jbcrypt.BCrypt;

import java.lang.annotation.Repeatable;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Path("/users")
public class PersonAPI {

    private final PersonDAO dao;

    //It's used when the server creates the API
    public PersonAPI(){
        this(new PersonDAO());
    }

    // It's used to provide mock for unit tests
    public PersonAPI(PersonDAO dao){
        this.dao = dao;
    }

    @POST
    @Path("/signup")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAccount(JsonNode json) {

        // Check whether the JSON is null or is not an object.
        if (json == null || !json.isObject()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap(
                            "message", "JSON invalide."
                    ))
                    .build();


        }

        // Read each value as text, using an empty string if missing or null.
        String name = json.path("name").asText("").trim();
        String firstname = json.path("firstname").asText("").trim();

        // Normalize the email to match the SQL constraint.
        String email = json.path("email").asText("")
                .trim().toLowerCase(Locale.ROOT);

        String password = json.path("password").asText("");
        String role = json.path("role").asText("");

        Person person;

        try {
            if ("owner".equals(role)) {
                person = new Owner(name, firstname, email, password);
            } else {
                person = new Roomie(name, firstname, email, password);
            }
        } catch(IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap(
                            "message", e.getMessage()
                    ))
                    .build();
        }


        if (name.isEmpty() || firstname.isEmpty()
                || email.isEmpty() || password.isEmpty()
                || role.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap(
                            "message", "All fields must been filled."
                    ))
                    .build();
        }

        if (name.length() > 50 || firstname.length() > 50) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap(
                            "message", "Lastname and firstname are maximum 50 characters."
                    ))
                    .build();
        }

        if (email.length() > 254
                || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap(
                            "message", "Invalid Email."
                    ))
                    .build();
        }

        if (password.length() < 8
                || !password.matches("(?s).*[A-Z].*")
                || !password.matches("(?s).*[a-z].*")
                || !password.matches("(?s).*[0-9].*")
                || !password.matches("(?s).*[^A-Za-z0-9].*")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap(
                            "message",
                            "Password must contain at least 8 characters, "
                                    + "a uppercase letter, a lowercase letter, a number "
                                    + "and a special character."
                    ))
                    .build();
        }

        // Reject passwords exceeding bcrypt's limit.
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap(
                            "message", "Password too long."
                    ))
                    .build();
        }


        if (!"owner".equals(role) && !"roomie".equals(role)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap(
                            "message", "Invalid role."
                    ))
                    .build();
        }

        if (Person.existsByEmail(email, dao)) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Collections.singletonMap(
                            "message", "Email already used."
                    ))
                    .build();
        }

        // Hash the password.
        String passwordHash =
                BCrypt.hashpw(password, BCrypt.gensalt(12));


        person.create(dao);

        // Prepare public user information for the frontend.
        Map<String, Object> user = new HashMap<>();
        user.put("id", person.getIdPerson());
        user.put("name", person.getLastName());
        user.put("firstname", person.getFirstName());
        user.put("email", person.getEmail());
        user.put("role", role);

        return Response.status(Response.Status.CREATED)
                .entity(Collections.singletonMap("user", user))
                .build();
    }
}