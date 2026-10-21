package be.cohabitapi.cohabitapi.Models;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

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
}