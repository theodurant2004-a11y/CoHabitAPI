package be.cohabitapi.api;

import be.cohabitapi.dao.PersonDAO;
import be.cohabitapi.models.Owner;
import be.cohabitapi.models.Person;
import be.cohabitapi.models.Roomie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    private ObjectMapper mapper;

    @BeforeEach // Run this setup before each test.
    void setUp() {
        // Use a fake DAO so no database operations are performed.
        dao = mock(PersonDAO.class);
        api = new PersonAPI(dao); // Give the fake DAO to the real API.
        mapper = new ObjectMapper(); // Used to create JSON objects.
    }

    private ObjectNode validRequest(String role) {
        // Prepare a valid signup request.
        ObjectNode json = mapper.createObjectNode();
        json.put("name", "Martin"); // Add a field to the JSON request.
        json.put("firstname", "Alice");
        json.put("email", "alice.test@example.com");
        json.put("password", "TestCompte123!");
        json.put("role", role);
        return json;
    }

    private void assertRejected(ObjectNode json) {
        // Invalid requests must be rejected before calling the DAO.
        try (Response response = api.createAccount(json)) { // Call the API directly; the response is closed after this block.
            assertEquals(400, response.getStatus()); // Expect a bad request.
            assertNotNull(((Map<?, ?>) response.getEntity()).get("message")); // Check that an error message exists.
        }

        verifyNoInteractions(dao); // Check that the DAO received no calls.
    }

    @Test
    void shouldRejectNullJson() {
        try (Response response = api.createAccount(null)) {
            assertEquals(400, response.getStatus()); // Expect a bad request.
        }

        verifyNoInteractions(dao);
    }

    @Test
    void shouldRejectJsonArray() { // Reject an array such as [], because an object is required.
        try (Response response =
                     api.createAccount(mapper.createArrayNode())) {
            assertEquals(400, response.getStatus()); // Expect a bad request.
        }

        verifyNoInteractions(dao);
    }

    @ParameterizedTest // Run this test once for each supplied value or row.
    @ValueSource(strings = { // Run once for each missing field.
            "name", "firstname", "email", "password", "role"
    })
    void shouldRejectMissingField(String field) {
        ObjectNode json = validRequest("owner");
        json.remove(field); // Remove one required field.

        assertRejected(json);
    }

    @ParameterizedTest // Run this test once for each supplied value or row.
    @ValueSource(strings = {
            "name", "firstname", "email", "password", "role"
    })
    void shouldRejectEmptyField(String field) {
        ObjectNode json = validRequest("owner");
        json.put(field, ""); // Keep the field, but make its value empty.

        assertRejected(json);
    }

    @ParameterizedTest // Run this test once for each supplied value or row.
    @ValueSource(strings = {"name", "firstname"})
    void shouldRejectNameLongerThan50Characters(String field) {
        ObjectNode json = validRequest("owner");
        json.put(field, repeat('A', 51)); // Exceed the 50-character limit.

        assertRejected(json);
    }

    @ParameterizedTest // Run this test once for each supplied value or row.
    @ValueSource(strings = {
            "pas-un-email",
            "alice.example.com",
            "alice@",
            "alice @example.com"
    })
    void shouldRejectInvalidEmail(String email) {
        ObjectNode json = validRequest("owner");
        json.put("email", email);

        assertRejected(json);
    }

    @Test
    void shouldRejectEmailLongerThan254Characters() {
        ObjectNode json = validRequest("owner");
        json.put("email", repeat('a', 243) + "@example.com"); // Build a 255-character email.

        assertRejected(json);
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
        ObjectNode json = validRequest("owner");
        json.put("password", password);

        assertRejected(json);
    }

    @Test
    void shouldRejectPasswordLongerThan72Bytes() {
        ObjectNode json = validRequest("owner");

        // Accented characters use more than one byte in UTF-8.
        json.put("password", "Aa1!" + repeat('é', 35)); // 74 bytes, despite having only 39 characters.

        assertRejected(json);
    }

    @Test
    void shouldRejectInvalidRole() {
        ObjectNode json = validRequest("admin"); // Only owner and roomie are allowed.

        assertRejected(json);
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
                    "Cet email est déjà utilisé.",
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

        ObjectNode json = validRequest(role);
        // Add spaces and uppercase letters to check normalization.
        json.put("name", " Martin ");
        json.put("firstname", " Alice ");
        json.put("email", " ALICE.TEST@EXAMPLE.COM ");

        try (Response response = api.createAccount(json)) {
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

            // Check that surrounding spaces were removed and the email was lowercased.
            assertEquals("Martin", person.getLastName());
            assertEquals("Alice", person.getFirstName());
            assertEquals("alice.test@example.com", person.getEmail());

            // Check that the password was correctly hashed.
            assertNotEquals(
                    "TestCompte123!",
                    person.getPasswordHash()
            );
            // Verify that the hash matches the original password.
            assertTrue(BCrypt.checkpw(
                    "TestCompte123!",
                    person.getPasswordHash()
            ));

            // Check the public response.
            Map<?, ?> body = (Map<?, ?>) response.getEntity(); // Read the response body as a map.
            Map<?, ?> user = (Map<?, ?>) body.get("user"); // Read the nested public user data.

            assertEquals(42, user.get("id"));
            assertEquals("Martin", user.get("name"));
            assertEquals("Alice", user.get("firstname"));
            assertEquals("alice.test@example.com", user.get("email"));
            assertEquals(role, user.get("role"));
            assertEquals(5, user.size()); // Expect exactly the five public fields.
            // Passwords and hashes must not appear in the response.
            assertFalse(user.containsKey("password"));
            assertFalse(user.containsKey("passwordHash"));
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