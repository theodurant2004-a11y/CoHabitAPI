package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DTO.SigninRequest;
import be.cohabitapi.cohabitapi.DTO.UserResponse;
import be.cohabitapi.cohabitapi.Models.Owner;
import be.cohabitapi.cohabitapi.Models.Person;
import be.cohabitapi.cohabitapi.Models.Roomie;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.internal.inject.ParamConverters;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Path("/users")
public class PersonAPI {

    private final PersonDAO dao;
    private ParamConverters.StringConstructor stringConstructor;

    //It's used when the server creates the API
    public PersonAPI(){
        this(new PersonDAO());
    }

    // It's used to provide mock for unit tests
    public PersonAPI(PersonDAO dao){
        this.dao = dao;
    }

    @Inject
    public PersonAPI(ParamConverters.StringConstructor stringConstructor) {
        this.stringConstructor = stringConstructor;
    }

    @POST
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

        if (name.isEmpty() || firstname.isEmpty()
                || email.isEmpty() || password.isEmpty()
                || role.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap(
                            "message", "Tous les champs sont obligatoires."
                    ))
                    .build();
        }

        if (name.length() > 50 || firstname.length() > 50) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap(
                            "message", "Nom et prénom : maximum 50 caractères."
                    ))
                    .build();
        }

        if (email.length() > 254
                || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap(
                            "message", "Email invalide."
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
                            "Le mot de passe doit contenir au moins 8 caractères, "
                                    + "une majuscule, une minuscule, un chiffre "
                                    + "et un caractère spécial."
                    ))
                    .build();
        }

        // Reject passwords exceeding bcrypt's limit.
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap(
                            "message", "Mot de passe trop long."
                    ))
                    .build();
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
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Collections.singletonMap("message", "JSON invalid."))
                    .build();
        }

        //take data
        String email = req.getEmail();
        String password = req.getPassword();

        Person personLogin = Person.login(email, password, dao);

        if(personLogin == null){
            return Response.status(Response.Status.BAD_REQUEST)
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