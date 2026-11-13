package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.EXCEPTION.InvalidCredentialsException;
import be.cohabitapi.cohabitapi.Models.Owner;
import be.cohabitapi.cohabitapi.Models.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class loginTest {

    private static final String EMAIL = "alice@test.be";
    private static final String PASSWORD = "Test1234!";

    @Mock
    private PersonDAO dao;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // Builds a real Owner: the constructor calls setPassword, which hashes the plain password
    private Person alice() {
        return new Owner("Doe", "Alice", EMAIL, PASSWORD);
    }

    //==============================SUCCESS==============================

    @Test
    @DisplayName("login: good email and password -> returns the person")
    void login_validCredentials_returnsPerson() {
        // Arrange
        Person alice = alice();
        when(dao.findByEmail(EMAIL)).thenReturn(alice);

        // Act
        Person result = Person.login(EMAIL, PASSWORD, dao);

        // Assert
        assertSame(alice, result);
        verify(dao, times(1)).findByEmail(EMAIL);
    }

    @Test
    @DisplayName("login: keeps the real subclass (Owner), no role attribute needed")
    void login_validCredentials_returnsRightSubclass() {
        // Arrange
        when(dao.findByEmail(EMAIL)).thenReturn(alice());

        // Act
        Person result = Person.login(EMAIL, PASSWORD, dao);

        // Assert
        assertInstanceOf(Owner.class, result);
    }

    @Test
    @DisplayName("login: email in uppercase with spaces -> DAO called with the normalized email")
    void login_emailUppercaseAndSpaces_isNormalized() {
        // Arrange
        when(dao.findByEmail(EMAIL)).thenReturn(alice());

        // Act
        Person result = Person.login("  ALICE@Test.BE  ", PASSWORD, dao);

        // Assert
        assertNotNull(result);
        verify(dao).findByEmail(EMAIL);
    }

    @Test
    @DisplayName("login: password with spaces around -> trimmed, login works (current behavior)")
    void login_passwordWithSpaces_isTrimmed() {
        // Arrange
        when(dao.findByEmail(EMAIL)).thenReturn(alice());

        // Act
        Person result = Person.login(EMAIL, "  " + PASSWORD + "  ", dao);

        // Assert
        assertNotNull(result);
    }

    //==============================WRONG CREDENTIALS==============================

    @Test
    @DisplayName("login: unknown email -> InvalidCredentialsException")
    void login_unknownEmail_throws() {
        // Arrange
        when(dao.findByEmail("unknown@test.be")).thenReturn(null);

        // Act + Assert
        assertThrows(InvalidCredentialsException.class,
                () -> Person.login("unknown@test.be", PASSWORD, dao));
        verify(dao).findByEmail("unknown@test.be");
    }

    @Test
    @DisplayName("login: wrong password -> InvalidCredentialsException")
    void login_wrongPassword_throws() {
        // Arrange
        when(dao.findByEmail(EMAIL)).thenReturn(alice());

        // Act + Assert
        assertThrows(InvalidCredentialsException.class,
                () -> Person.login(EMAIL, "Wrong1234!", dao));
    }

    @Test
    @DisplayName("login: password with wrong case -> InvalidCredentialsException")
    void login_passwordWrongCase_throws() {
        // Arrange
        when(dao.findByEmail(EMAIL)).thenReturn(alice());

        // Act + Assert
        assertThrows(InvalidCredentialsException.class,
                () -> Person.login(EMAIL, "test1234!", dao));
    }

    @Test
    @DisplayName("login: unknown email and wrong password give the same message")
    void login_unknownEmailAndWrongPassword_sameMessage() {
        // Arrange
        when(dao.findByEmail("unknown@test.be")).thenReturn(null);
        when(dao.findByEmail(EMAIL)).thenReturn(alice());

        // Act
        InvalidCredentialsException unknownEmail = assertThrows(InvalidCredentialsException.class,
                () -> Person.login("unknown@test.be", PASSWORD, dao));
        InvalidCredentialsException wrongPassword = assertThrows(InvalidCredentialsException.class,
                () -> Person.login(EMAIL, "Wrong1234!", dao));

        // Assert
        assertEquals(unknownEmail.getMessage(), wrongPassword.getMessage());
    }

    //==============================EMPTY INPUTS (DAO never called)==============================

    @Test
    @DisplayName("login: null email -> InvalidCredentialsException, DAO never called")
    void login_nullEmail_throws() {
        assertThrows(InvalidCredentialsException.class,
                () -> Person.login(null, PASSWORD, dao));
        verify(dao, never()).findByEmail(any());
    }

    @Test
    @DisplayName("login: empty email -> InvalidCredentialsException, DAO never called")
    void login_emptyEmail_throws() {
        assertThrows(InvalidCredentialsException.class,
                () -> Person.login("", PASSWORD, dao));
        verify(dao, never()).findByEmail(any());
    }

    @Test
    @DisplayName("login: email with only spaces -> InvalidCredentialsException, DAO never called")
    void login_blankEmail_throws() {
        assertThrows(InvalidCredentialsException.class,
                () -> Person.login("   ", PASSWORD, dao));
        verify(dao, never()).findByEmail(any());
    }

    @Test
    @DisplayName("login: null password -> InvalidCredentialsException, DAO never called")
    void login_nullPassword_throws() {
        assertThrows(InvalidCredentialsException.class,
                () -> Person.login(EMAIL, null, dao));
        verify(dao, never()).findByEmail(any());
    }

    @Test
    @DisplayName("login: empty password -> InvalidCredentialsException, DAO never called")
    void login_emptyPassword_throws() {
        assertThrows(InvalidCredentialsException.class,
                () -> Person.login(EMAIL, "", dao));
        verify(dao, never()).findByEmail(any());
    }

    @Test
    @DisplayName("login: password with only spaces -> InvalidCredentialsException, DAO never called")
    void login_blankPassword_throws() {
        assertThrows(InvalidCredentialsException.class,
                () -> Person.login(EMAIL, "   ", dao));
        verify(dao, never()).findByEmail(any());
    }

    @Test
    @DisplayName("login: both null -> InvalidCredentialsException, DAO never called")
    void login_bothNull_throws() {
        assertThrows(InvalidCredentialsException.class,
                () -> Person.login(null, null, dao));
        verify(dao, never()).findByEmail(any());
    }

}
