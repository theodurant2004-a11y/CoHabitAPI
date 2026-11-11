package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.EXCEPTION.NotInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.RoomShareNotFoundException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RoomShareTest {

    RoomShare rs = new RoomShare();


    // ------------------------ SET AN INVITATION KEY ------------------------

    @Test // If you try a right invitation key
    void setInvitationKey_Valid() {
        Owner owner = new Owner("lastname", "firstname", "email@email.com", "P@ssword123");
        RoomShare roomShare = new RoomShare("123456", "name", owner);

        assertEquals("123456", roomShare.getInvitationKey(), "The invitation key must be correctly assigned and retrievable.");
    }

    @Test // If you try an invitation key with a null value
    void setInvitationKey_WithNullValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            new RoomShare(null, "name", new Owner("lastname",  "firstname", "email@email.com", "P@ssword123"));
        }, "The constructor must throw an IllegalArgumentException if the invitation key is null");
    }

    @Test // If you try an invitation key with an empty value
    void setInvitationKey_WithEmptyValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            new RoomShare("", "name", new Owner("lastname",  "firstname", "email@email.com", "P@ssword123"));
        }, "The constructor must throw an IllegalArgumentException if the invitation key is empty");
    }

    @Test // If you try an invitation key with a too long value
    void setInvitationKey_WithTooLongValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            new RoomShare("123456789101112131415161718192021222324252627282930", "name", new Owner("lastname",  "firstname", "email@email.com", "P@ssword123"));
        }, "The constructor must throw an IllegalArgumentException if the invitation key is too long");
    }


    // ------------------------ SET A NAME ------------------------

    @Test // If you try a right name
    void setName_Valid() {
        rs.setName("name");

        assertEquals("name", rs.getName(), "The name must be correctly assigned and retrievable.");
    }

    @Test // If you try a name with a null value
    void setName_WithNullValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            rs.setName(null);
        }, "The setter must throw an IllegalArgumentException if the name is null");
    }

    @Test // If you try a name with an empty value
    void setName_WithEmptyValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            rs.setName("");
        }, "The setter must throw an IllegalArgumentException if the name is empty");
    }

    @Test // If you try a name with a too long value
    void setName_WithTooLongValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            rs.setName("12345678910111213141516171819202122232425262728293031323334353637383940414243444546474849505152535455");
        }, "The setter must throw an IllegalArgumentException if the name is too long");
    }


    // ------------------------ SET AN OWNER ------------------------

    @Test // If you try a right owner
    void setOwner_Valid() {
        Owner owner = new Owner("lastname", "firstname", "email@email.com", "P@ssword123");
        rs.setOwner(owner);

        assertEquals(owner, rs.getOwner(), "The owner must be correctly assigned and retrievable.");
    }

    @Test // If you try an owner with a null value
    void setOwner_WithNullValue_Invalid() {
        assertThrows(NullPointerException.class, () -> {
            rs.setOwner(null);
        }, "The setter must throw a NullPointerException if the owner is null");
    }


    // ------------------------ SET A CREATION DATE ------------------------

    @Test // If you try a right creation date
    void setCreationDate_Valid() {
        Owner owner = new Owner("lastname", "firstname", "email@email.com", "P@ssword123");
        RoomShare roomShare = new RoomShare("123456", "name", owner);

        assertEquals(LocalDate.now(), roomShare.getCreationDate(), "The creation date must be set to today's date upon instantiation.");
    }


    // ------------------------ ADD A ROOMIE ------------------------

    @Test // If you try to add a valid roomie
    void addRoomie_Valid() {
        Roomie roomie = new Roomie("Doe", "John", "john.doe@email.com", "P@ssword123");
        rs.addRoomie(roomie);

        assertTrue(rs.getRoomies().contains(roomie), "The roomie must be added to the roomies list.");
        assertEquals(rs, roomie.getRoomshare(), "The room share must be assigned to the roomie to maintain the bidirectional link.");
    }

    @Test // If you try to add a null roomie
    void addRoomie_WithNullValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            rs.addRoomie(null);
        }, "The method must throw an IllegalArgumentException if the roomie is null");
    }

    @Test // If you try to add a roomie already in the list
    void addRoomie_AlreadyExists_ByValue_Invalid() {
        Roomie roomie1 = new Roomie("Doe", "John", "john.doe@email.com", "P@ssword123");
        rs.addRoomie(roomie1);

        // Create a second roomie with the same identifier (the email)
        Roomie roomie2 = new Roomie("Doe", "Johnny", "john.doe@email.com", "P@ssword456");

        assertThrows(IllegalArgumentException.class, () -> {
            rs.addRoomie(roomie2);
        }, "The method must throw an IllegalArgumentException if a roomie with the same email is already present");
    }

    @Test // If you try to add a roomie who already belongs to another room share
    void addRoomie_AlreadyBelongsToAnotherRoomShare_Invalid() {
        RoomShare otherRS = new RoomShare("123456", "otherName", new Owner("lastname", "firstname", "email@email.com", "P@ssword123"));
        Roomie roomie = new Roomie("Doe", "John", "john.doe@email.com", "P@ssword123");

        // There, the roomie belongs to a room share
        otherRS.addRoomie(roomie);

        assertThrows(IllegalArgumentException.class, () -> {
            rs.addRoomie(roomie);
        }, "The method must throw an IllegalArgumentException if the roomie already belongs to another room share");
    }


    // ------------------------ GENERATE INVITATION KEY ------------------------

    @Test // If the first generated key is unique and returned directly
    void generateInvitationKey_UniqueKey_Valid() {
        RoomShareDAO mockDao = mock(RoomShareDAO.class);
        // By default, Mockito returns false for boolean, meaning keyIsAlreadyUsed = false
        when(mockDao.keyIsAlreadyUsed(anyString())).thenReturn(false);

        String generatedKey = RoomShare.generateInvitationKey(mockDao);

        assertNotNull(generatedKey, "The generated key must not be null.");
        assertEquals(6, generatedKey.length(), "The generated key must be exactly 6 characters long.");
        assertTrue(generatedKey.matches("[A-Z0-9]+"), "The generated key must only contain uppercase letters and numbers.");

        // Check that the DAO was called exactly once to check the key
        verify(mockDao, times(1)).keyIsAlreadyUsed(generatedKey);
    }

    @Test // If the first key is already taken, it loops and finds a second unique key
    void generateInvitationKey_FirstKeyUsed_Valid() {
        RoomShareDAO mockDao = mock(RoomShareDAO.class);

        // We force the DAO to say "true" (used) on the first call, and "false" (free) on the second call
        when(mockDao.keyIsAlreadyUsed(anyString()))
                .thenReturn(true)
                .thenReturn(false);

        String generatedKey = RoomShare.generateInvitationKey(mockDao);

        assertNotNull(generatedKey, "The generated key must not be null.");
        assertEquals(6, generatedKey.length(), "The generated key must be exactly 6 characters long.");

        // Check the DAO was called exactly 2 times because of the collision
        verify(mockDao, times(2)).keyIsAlreadyUsed(anyString());
    }


    // ------------------------ FIND BY ID ------------------------

//    @Test // If the roomshare is successfully found
//    void findById_Found_Valid() {
//        RoomShareDAO mockDao = mock(RoomShareDAO.class);
//        when(mockDao.findById(1)).thenReturn(rs);
//
//        RoomShare result = RoomShare.findByIdW(mockDao, 1);
//
//        assertEquals(rs, result, "The method must return the room share if it exists in the database.");
//    }

//    @Test // If the roomshare is not found
//    void findById_NotFound_Invalid() {
//        RoomShareDAO mockDao = mock(RoomShareDAO.class);
//        when(mockDao.findById(1)).thenReturn(null);
//
//        assertThrows(RoomShareNotFoundException.class, () -> {
//            RoomShare.findById(mockDao, 1);
//        }, "The method must throw a RoomShareNotFoundException if the DAO returns null.");
//    }


    // ------------------------ FIND MEMBERS ------------------------

    @Test // If the user who asks is the owner himself
    void findMembers_ByOwner_Valid() {
        PersonDAO mockPersonDao = mock(PersonDAO.class);

        Owner owner = new Owner("lastname", "firstname", "owner@email.com", "P@ssword123");
        owner.setIdPerson(1);

        // Some accepted roomies given by the DB
        Roomie acceptedRoomie = new Roomie("Doe", "John", "john@email.com", "P@ssword123");
        List<Person> dbRoomiesList = Collections.singletonList(acceptedRoomie);

        when(mockPersonDao.findByEmail("owner@email.com")).thenReturn(owner);
        when(mockPersonDao.findAcceptedRoomiesByOwner(1)).thenReturn((List) dbRoomiesList);

        List<Person> result = RoomShare.findMembers("owner@email.com", mockPersonDao);

        assertEquals(2, result.size(), "The list must contain the owner and the accepted roomies.");
        assertTrue(result.contains(owner), "The owner must be in the members list.");
        assertTrue(result.contains(acceptedRoomie), "The accepted roomies must be in the members list.");
    }

    @Test // If the user who asks is an accepted roomie of the roomshare
    void findMembers_ByAcceptedRoomie_Valid() {
        PersonDAO mockPersonDao = mock(PersonDAO.class);

        Owner owner = new Owner("lastname", "firstname", "owner@email.com", "P@ssword123");
        owner.setIdPerson(1);
        RoomShare mockRs = new RoomShare("123456", "name", owner);

        // A roomie who belongs to the roomshare and is ACCEPTED
        Roomie roomie = new Roomie("Doe", "John", "john@email.com", "P@ssword123", mockRs);

        when(mockPersonDao.findByEmail("john@email.com")).thenReturn(roomie);
        when(mockPersonDao.findAcceptedRoomiesByOwner(1)).thenReturn((List) Collections.singletonList(roomie));

        List<Person> result = RoomShare.findMembers("john@email.com", mockPersonDao);

        assertEquals(2, result.size(), "The list must contain the owner and the roomie himself.");
        assertTrue(result.contains(owner), "The owner must be the first in the members list.");
    }

    @Test // If the user who asks is a roomie but is not accepted (e.g. PENDING or NONE)
    void findMembers_ByPendingRoomie_Invalid() {
        PersonDAO mockPersonDao = mock(PersonDAO.class);

        Owner owner = new Owner("lastname", "firstname", "owner@email.com", "P@ssword123");
        RoomShare mockRs = new RoomShare("123456", "name", owner);

        Roomie pendingRoomie = new Roomie("Doe", "John", "john@email.com", "P@ssword123");
        pendingRoomie.sendJoinRequest(mockRs); // Changes status to PENDING

        when(mockPersonDao.findByEmail("john@email.com")).thenReturn(pendingRoomie);

        assertThrows(NotInRoomshareException.class, () -> {
            RoomShare.findMembers("john@email.com", mockPersonDao);
        }, "The method must throw a NotInRoomshareException if the user is a roomie but not accepted by the owner.");
    }

    @Test // If the user who asks is not found in the DB at all
    void findMembers_UserNotFound_Invalid() {
        PersonDAO mockPersonDao = mock(PersonDAO.class);
        when(mockPersonDao.findByEmail("ghost@email.com")).thenReturn(null);

        assertThrows(UserNotFoundException.class, () -> {
            RoomShare.findMembers("ghost@email.com", mockPersonDao);
        }, "The method must throw a UserNotFoundException if the email does not correspond to anyone in the DB.");
    }
}