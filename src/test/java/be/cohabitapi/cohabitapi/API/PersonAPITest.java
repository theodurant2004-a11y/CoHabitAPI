package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DTO.SignupRequest;
import be.cohabitapi.cohabitapi.DTO.UserResponse;
import be.cohabitapi.cohabitapi.Models.Owner;
import be.cohabitapi.cohabitapi.Models.Person;
import be.cohabitapi.cohabitapi.Models.Roomie;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import org.mindrot.jbcrypt.BCrypt;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PersonAPITest {

    private PersonDAO dao;
    private PersonAPI api;

    @BeforeEach // Run this setup before each test.
    void setUp() {
        // Use a fake DAO so no database operations are performed.
        dao = mock(PersonDAO.class);
        api = new PersonAPI(dao); // Give the fake DAO to the real API.
    }

    private SignupRequest validRequest(String role) {
        // Prepare a valid signup request (the DTO that Jackson would build from the JSON).
        SignupRequest req = new SignupRequest();
        req.setLastName("Martin"); // Set a field of the request.
        req.setFirstname("Alice");
        req.setEmail("alice.test@example.com");
        req.setPassword("TestCompte123!");
        req.setConfirmPassword("TestCompte123!"); // Must be identical to the password.
        req.setRole(role);
        return req;
    }

    private void assertRejected(SignupRequest req) {
        // Invalid requests must be rejected before calling the DAO.
        try (Response response = api.createAccount(req)) { // Call the API directly; the response is closed after this block.
            assertEquals(400, response.getStatus()); // Expect a bad request.
            assertNotNull(((Map<?, ?>) response.getEntity()).get("message")); // Check that an error message exists.
        }

        verifyNoInteractions(dao); // Check that the DAO received no calls.
    }

    @Test
    void shouldRejectNullRequest() {
        try (Response response = api.createAccount(null)) {
            assertEquals(400, response.getStatus()); // Expect a bad request.
        }

        verifyNoInteractions(dao);
    }

    // Note: the "JSON array" test was removed. Jackson builds the DTO before the method is called,
    // so a broken or wrong JSON never reaches the API (it is handled by JsonExceptionMapper).

    @Test
    void shouldRejectDifferentPasswords() {
        SignupRequest req = validRequest("owner");
        req.setConfirmPassword("Autre123!x"); // Different from the password.

        assertRejected(req);
    }

    @Test
    void shouldRejectMissingConfirmPassword() {
        SignupRequest req = validRequest("owner");
        req.setConfirmPassword(null); // The field was not sent.

        assertRejected(req);
    }

    @Test
    void shouldAcceptPasswordsDifferingOnlyBySpaces() {
        // Both are trimmed the same way, so "TestCompte123! " equals "TestCompte123!".
        when(dao.existsByEmail("alice.test@example.com")).thenReturn(false);

        SignupRequest req = validRequest("owner");
        req.setConfirmPassword("TestCompte123! ");

        try (Response response = api.createAccount(req)) {
            assertEquals(201, response.getStatus());
        }
    }

    @ParameterizedTest // Run this test once for each supplied value or row.
    @NullAndEmptySource // Run once with null and once with "" (missing field and empty field).
    void shouldRejectBlankName(String value) {
        SignupRequest req = validRequest("owner");
        req.setLastName(value);

        assertRejected(req);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectBlankFirstname(String value) {
        SignupRequest req = validRequest("owner");
        req.setFirstname(value);

        assertRejected(req);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectBlankEmail(String value) {
        SignupRequest req = validRequest("owner");
        req.setEmail(value);

        assertRejected(req);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectBlankPassword(String value) {
        SignupRequest req = validRequest("owner");
        req.setPassword(value);
        req.setConfirmPassword(value); // Identical, so only the business rule can fail.

        assertRejected(req);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectBlankRole(String value) {
        SignupRequest req = validRequest(value);

        assertRejected(req);
    }

    @ParameterizedTest // Run this test once for each supplied value or row.
    @ValueSource(strings = {"name", "firstname"})
    void shouldRejectNameLongerThan50Characters(String field) {
        SignupRequest req = validRequest("owner");
        String tooLong = repeat('A', 51); // Exceed the 50-character limit.

        if ("name".equals(field)) {
            req.setLastName(tooLong);
        } else {
            req.setFirstname(tooLong);
        }

        assertRejected(req);
    }

    @ParameterizedTest // Run this test once for each supplied value or row.
    @ValueSource(strings = {
            "pas-un-email",
            "alice.example.com",
            "alice@",
            "alice @example.com"
    })
    void shouldRejectInvalidEmail(String email) {
        SignupRequest req = validRequest("owner");
        req.setEmail(email);

        assertRejected(req);
    }

    @Test
    void shouldRejectEmailLongerThan254Characters() {
        SignupRequest req = validRequest("owner");
        req.setEmail(repeat('a', 243) + "@example.com"); // Build a 255-character email.

        assertRejected(req);
    }

    @ParameterizedTest // Run this test once for each supplied value or row.
    @ValueSource(strings = {
            "Ab1!", // Too short.
            "testcompte123!", // No uppercase letter.
            "TESTCOMPTE123!", // No lowercase letter.
            "TestCompte!", // No digit.
            "TestCompte123" // No special character.
    })
    void shouldRejectInvalidPassword(String password) {
        SignupRequest req = validRequest("owner");
        req.setPassword(password);
        req.setConfirmPassword(password); // Identical, so the test fails for the business rule, not the mismatch.

        assertRejected(req);
    }

    @Test
    void shouldRejectPasswordLongerThan72Bytes() {
        // Accented characters use more than one byte in UTF-8.
        String longPassword = "Aa1!" + repeat('é', 35); // 74 bytes, despite having only 39 characters.

        SignupRequest req = validRequest("owner");
        req.setPassword(longPassword);
        req.setConfirmPassword(longPassword);

        assertRejected(req);
    }

    @Test
    void shouldRejectInvalidRole() {
        SignupRequest req = validRequest("admin"); // Only owner and roomie are allowed.

        assertRejected(req);
    }

    @Test
    void shouldRejectAlreadyUsedEmail() {
        // Simulate an email already present in the database.
        when(dao.existsByEmail("alice.test@example.com"))
                .thenReturn(true); // The mock reports that the email exists.

        try (Response response =
                     api.createAccount(validRequest("owner"))) {
            assertEquals(409, response.getStatus()); // Expect an email conflict.
            assertEquals(
                    "Email already used.",
                    ((Map<?, ?>) response.getEntity()).get("message")
            );
        }

        verify(dao).existsByEmail("alice.test@example.com");
        verify(dao, never()).create(any(Person.class)); // No Person should be saved.
    }

    @ParameterizedTest // Run this test once for each supplied value or row.
    @CsvSource({ // Each row supplies a role and its expected class.
            "owner, Owner",
            "roomie, Roomie"
    })
    void shouldCreateAccount(String role, String expectedClass) {
        when(dao.existsByEmail("alice.test@example.com"))
                .thenReturn(false); // The mock reports that the email is available.

        // Simulate the ID normally generated by the database.
        doAnswer(invocation -> {
            Person person = invocation.getArgument(0); // Get the Person passed to create().
            person.setIdPerson(42); // Assign a fake ID without inserting into the database.
            return null; // create() is a void method, so there is no result.
        }).when(dao).create(any(Person.class)); // Apply this behavior to any Person.

        SignupRequest req = validRequest(role);
        // Add spaces and uppercase letters to check normalization.
        req.setLastName(" Martin ");
        req.setFirstname(" Alice ");
        req.setEmail(" ALICE.TEST@EXAMPLE.COM ");

        try (Response response = api.createAccount(req)) {
            assertEquals(201, response.getStatus()); // Expect a successful creation.

            // Inspect the object passed to the DAO.
            ArgumentCaptor<Person> captor =
                    ArgumentCaptor.forClass(Person.class);

            verify(dao).create(captor.capture()); // Capture the object sent to the DAO.
            Person person = captor.getValue(); // Read the captured Person.

            // Check that the role produces the correct subclass.
            if ("Owner".equals(expectedClass)) {
                assertTrue(person instanceof Owner);
            } else {
                assertTrue(person instanceof Roomie);
            }

            // Check that surrounding spaces were removed and the email was lowercased (done by Person).
            assertEquals("Martin", person.getLastName());
            assertEquals("Alice", person.getFirstName());
            assertEquals("alice.test@example.com", person.getEmail());

            // Check that the password was correctly hashed.
            assertNotEquals(
                    "TestCompte123!",
                    person.getPasswordHash()
            );
            // Verify that the hash matches the original password (so it was hashed only once).
            assertTrue(BCrypt.checkpw(
                    "TestCompte123!",
                    person.getPasswordHash()
            ));

            // Check the public response: the body contains a UserResponse DTO, not a Map.
            Map<?, ?> body = (Map<?, ?>) response.getEntity(); // Read the response body as a map.
            UserResponse user = (UserResponse) body.get("user"); // Read the nested public user data.

            assertEquals(Integer.valueOf(42), user.getId_person());
            assertEquals("Martin", user.getLastName());
            assertEquals("Alice", user.getFirstName());
            assertEquals("alice.test@example.com", user.getEmail());
            assertEquals(role, user.getRole()); // The role is sent back to the front.
        }

        verify(dao).existsByEmail("alice.test@example.com");
        verifyNoMoreInteractions(dao); // Reject any extra DAO calls not already verified.
    }

    private static String repeat(char character, int count) {
        // Build test values without requiring String.repeat().
        char[] characters = new char[count];
        java.util.Arrays.fill(characters, character); // Fill the array with the same character.
        return new String(characters);
    }
}