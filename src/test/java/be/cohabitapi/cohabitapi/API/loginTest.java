package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.Models.Owner;
import be.cohabitapi.cohabitapi.Models.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

class loginTest {

    @Mock
    private PersonDAO dao;

    private Owner alice;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        alice = new Owner();
        alice.setEmail("alice@test.com");
        alice.setPasswordHash(BCrypt.hashpw("Test1234!", BCrypt.gensalt(4)));
    }

    @Test
    void login_withValidCredentials_returnsPerson() {
        // Arrange
        when(dao.findByEmail("alice@test.com")).thenReturn(alice);

        // Act
        Person result = Person.login("alice@test.com", "Test1234!", dao);

        // Assert
        assertSame(alice, result);
    }

}
