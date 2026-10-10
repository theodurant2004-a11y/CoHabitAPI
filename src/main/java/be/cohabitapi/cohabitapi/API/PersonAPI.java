package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DTO.SigninRequest;
import be.cohabitapi.cohabitapi.DTO.SignupRequest;
import be.cohabitapi.cohabitapi.DTO.UserResponse;
import be.cohabitapi.cohabitapi.EXCEPTION.InvalidCredentialsException;
import be.cohabitapi.cohabitapi.Models.Owner;
import be.cohabitapi.cohabitapi.Models.Person;
import be.cohabitapi.cohabitapi.Models.Roomie;

<<<<<<< HEAD
=======
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.inject.Inject;
>>>>>>> origin/feature/login
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

    @Inject
    private PersonDAO dao;

    @POST
    //@Path("/signup")
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
                person = new Owner(req.getLastname(), req.getFirstname(), req.getEmail(), req.getPassword());
            } else if ("roomie".equals(role)) {
                person = new Roomie(req.getLastname(), req.getFirstname(), req.getEmail(), req.getPassword());
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

    //--------------------- SignIn gestion -------------------------------
    // Builds an error response
    private Response error(Response.Status status, String message) {
        return Response.status(status)
                .entity(Collections.singletonMap(
                        "message", message
                ))
                .build();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(SigninRequest req){


        //Verify if one DTO exist
        if (req == null) return error(Response.Status.BAD_REQUEST, "JSON invalid.");

        try {
            // Ask the model
            Person personLogin = Person.login(req.getEmail(), req.getPassword(), dao);

            // Take the role of user
            String role = personLogin.getClass().getSimpleName();

            // api response
            UserResponse userResponse = new UserResponse(personLogin.getIdPerson(), personLogin.getFirstName(), personLogin.getLastName(), personLogin.getEmail(), role.toLowerCase(Locale.ROOT));
            return Response.ok(
                            Collections.singletonMap("user", userResponse))
                    .build();

        } catch (InvalidCredentialsException e) {
            //error 401 UNAUTHORIZED
            return error(Response.Status.UNAUTHORIZED, e.getMessage());

        } catch (RuntimeException e) {
            //error 500 INTERNAL_SERVER_ERROR
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }
}