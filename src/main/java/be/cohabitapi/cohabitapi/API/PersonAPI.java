package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DTO.SigninRequest;
import be.cohabitapi.cohabitapi.DTO.SignupRequest;
import be.cohabitapi.cohabitapi.DTO.UserResponse;
import be.cohabitapi.cohabitapi.EXCEPTION.EmailAlreadyUsedException;
import be.cohabitapi.cohabitapi.EXCEPTION.InvalidCredentialsException;
import be.cohabitapi.cohabitapi.Models.Owner;
import be.cohabitapi.cohabitapi.Models.Person;
import be.cohabitapi.cohabitapi.Models.Roomie;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.inject.Inject;
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

        //Verify if one DTO exist
        if (req == null) return error(Response.Status.BAD_REQUEST, "JSON invalid.");

        try {
            // Ask the model: it checks the passwords, the role and the email, then saves the person
            Person person = Person.signup(
                    req.getLastname(),
                    req.getFirstname(),
                    req.getEmail(),
                    req.getPassword(),
                    req.getConfirmPassword(),
                    req.getRole(),
                    dao);

            // Take the role of user (same way as in the login)
            String role = person.getClass().getSimpleName();

            // api response (the role is sent back to the front)
            UserResponse userResponse = new UserResponse(
                    person.getIdPerson(),
                    person.getFirstName(),
                    person.getLastName(),
                    person.getEmail(),
                    role.toLowerCase(Locale.ROOT));

            return Response.status(Response.Status.CREATED)
                    .entity(Collections.singletonMap("user", userResponse))
                    .build();

        } catch (EmailAlreadyUsedException e) {
            //error 409 CONFLICT
            return error(Response.Status.CONFLICT, e.getMessage());

        } catch (IllegalArgumentException e) {
            //error 400 BAD_REQUEST: a business rule is broken in the model (passwords, role, name, email...)
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (RuntimeException e) {
            //error 500 INTERNAL_SERVER_ERROR (for example the database is not reachable)
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
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