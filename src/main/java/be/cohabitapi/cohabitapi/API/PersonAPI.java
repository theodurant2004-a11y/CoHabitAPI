package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DTO.SigninRequest;
import be.cohabitapi.cohabitapi.DTO.UserResponse;
import be.cohabitapi.cohabitapi.Models.Owner;
import be.cohabitapi.cohabitapi.Models.Person;
import be.cohabitapi.cohabitapi.Models.Roomie;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.mindrot.jbcrypt.BCrypt;

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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAccount(SignupRequest req) {

        if (req == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("message", "JSON invalid."))
                    .build();
        }

        // Confirm password: same trim as in Person, so both rules stay consistent
        String password = req.getPassword() == null ? null : req.getPassword().trim();
        String confirmPassword = req.getConfirmPassword() == null ? null : req.getConfirmPassword().trim();

        if (password == null || !password.equals(confirmPassword)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("message", "Passwords do not match."))
                    .build();
        }

        String role = req.getRole() == null ? "" : req.getRole().trim();

        try {
            // The constructors call the setters, so the business rules run here
            Person person;
            if ("owner".equals(role)) {
                person = new Owner(req.getName(), req.getFirstname(), req.getEmail(), req.getPassword());
            } else if ("roomie".equals(role)) {
                person = new Roomie(req.getName(), req.getFirstname(), req.getEmail(), req.getPassword());
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Collections.singletonMap("message", "Invalid role."))
                        .build();
            }

            // The email is already normalized by Person.setEmail
            if (Person.existsByEmail(person.getEmail(), dao)) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(Collections.singletonMap("message", "Email already used."))
                        .build();
            }

            person.create(dao);

            UserResponse user = new UserResponse(
                    person.getIdPerson(),
                    person.getFirstName(),
                    person.getLastName(),
                    person.getEmail(),
                    person.getRole());

            return Response.status(Response.Status.CREATED)
                    .entity(Collections.singletonMap("user", user))
                    .build();

        } catch (IllegalArgumentException e) {
            // Business rule broken in Person (name, email, password...)
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("message", e.getMessage()))
                    .build();
        }
    }


        if (!"owner".equals(role) && !"roomie".equals(role)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap(
                            "message", "Rôle invalide."
                    ))
                    .build();
        }

        if (Person.existsByEmail(email, dao)) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Collections.singletonMap(
                            "message", "Cet email est déjà utilisé."
                    ))
                    .build();
        }

        // Hash the password.
        String passwordHash =
                BCrypt.hashpw(password, BCrypt.gensalt(12));

        Person person;

        if ("owner".equals(role)) {
            person = new Owner(name, firstname, email, passwordHash);
        } else {
            person = new Roomie(name, firstname, email, passwordHash);
        }

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

    //SignIn gestion
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(SigninRequest req){

        // Check whether the JSON is null or is not an object.
        if (req == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("message", "JSON invalid."))
                    .build();
        }

        //take data
        String email = req.getEmail();
        String password = req.getPassword();

        Person personLogin = Person.login(email, password, dao);

        if(personLogin == null){
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Collections.singletonMap(
                            "message", "Email or password is wrong"
                    ))
                    .build();
        }

        UserResponse userResponse = new UserResponse(personLogin.getIdPerson(), personLogin.getFirstName(), personLogin.getLastName(), personLogin.getEmail(), personLogin.getRole());
        return Response.ok(
                Collections.singletonMap("user", userResponse))
                .build();
    }
}