package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.*;
import be.cohabitapi.cohabitapi.EXCEPTION.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RoomShareTest {

    RoomShare rs = new RoomShare();


    // ------------------------ EQUALS & HASHCODE ------------------------

    @Test
    void equalsAndHashCode_Valid() {
        Owner owner = new Owner("lastname", "firstname", "owner@email.com", "P@ssword123word123");
        RoomShare rs1 = new RoomShare("123456", "Name 1", owner);
        RoomShare rs2 = new RoomShare("123456", "Name 2", owner);
        RoomShare rs3 = new RoomShare("654321", "Name 1", owner);

        assertEquals(rs1, rs2, "Two room shares with the same invitation key must be equal.");
        assertEquals(rs1.hashCode(), rs2.hashCode(), "Two equal room shares must have the same hash code.");
        assertNotEquals(rs1, rs3, "Room shares with different invitation keys must not be equal.");
    }


    // ------------------------ SET AN INVITATION KEY ------------------------

    @Test // If you try a right invitation key
    void setInvitationKey_Valid() {
        Owner owner = new Owner("lastname", "firstname", "email@email.com", "P@ssword123word123");
        RoomShare roomShare = new RoomShare("123456", "name", owner);

        assertEquals("123456", roomShare.getInvitationKey(), "La clé d'invitation doit être correctement assignée et récupérable.");
    }

    @Test // If you try an invitation key with a null value
    void setInvitationKey_WithNullValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            new RoomShare(null, "name", new Owner("lastname",  "firstname", "email@email.com", "P@ssword123word123"));
        }, "Le constructeur doit lever une IllegalArgumentException si la clé d'invitation est null");
    }

    @Test // If you try an invitation key with an empty value
    void setInvitationKey_WithEmptyValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            new RoomShare("", "name", new Owner("lastname",  "firstname", "email@email.com", "P@ssword123word123"));
        }, "Le constructeur doit lever une IllegalArgumentException si la clé d'invitation est vide");
    }

    @Test // If you try an invitation key with a too long value
    void setInvitationKey_WithTooLongValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            new RoomShare("123456789101112131415161718192021222324252627282930", "name", new Owner("lastname",  "firstname", "email@email.com", "P@ssword123word123"));
        }, "Le constructeur doit lever une IllegalArgumentException si la clé d'invitation est trop longue");
    }


    // ------------------------ SET A NAME ------------------------

    @Test // If you try a right name
    void setName_Valid() {
        rs.setName("name");

        assertEquals("name", rs.getName(), "Le nom doit être correctement assigné et récupérable.");
    }

    @Test // If you try a name with a null value
    void setName_WithNullValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            rs.setName(null);
        }, "Le setter doit lever une IllegalArgumentException si le nom est null");
    }

    @Test // If you try a name with an empty value
    void setName_WithEmptyValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            rs.setName("");
        }, "Le setter doit lever une IllegalArgumentException si le nom est vide");
    }

    @Test // If you try a name with a too long value
    void setName_WithTooLongValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            rs.setName("12345678910111213141516171819202122232425262728293031323334353637383940414243444546474849505152535455");
        }, "Le setter doit lever une IllegalArgumentException si le nom est trop long");
    }


    // ------------------------ SET AN OWNER ------------------------

    @Test // If you try a right owner
    void setOwner_Valid() {
        Owner owner = new Owner("lastname", "firstname", "email@email.com", "P@ssword123word123");
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
        Owner owner = new Owner("lastname", "firstname", "email@email.com", "P@ssword123word123");
        RoomShare roomShare = new RoomShare("123456", "name", owner);

        assertEquals(LocalDate.now(), roomShare.getCreationDate(), "The creation date must be set to today's date upon instantiation.");
    }


    // ------------------------ ADD & MANAGE ROOMIES ------------------------

    @Test // If you try to add a valid roomie
    void addRoomie_Valid() {
        Roomie roomie = new Roomie("Doe", "John", "john.doe@email.com", "P@ssword123word123");
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
        Roomie roomie1 = new Roomie("Doe", "John", "john.doe@email.com", "P@ssword123word123");
        rs.addRoomie(roomie1);

        Roomie roomie2 = new Roomie("Doe", "Johnny", "john.doe@email.com", "P@ssword123word456");

        assertThrows(IllegalArgumentException.class, () -> {
            rs.addRoomie(roomie2);
        }, "The method must throw an IllegalArgumentException if a roomie with the same email is already present");
    }

    @Test
    void getAcceptedRoomies_ReturnsOnlyAccepted() {
        Owner owner = new Owner("lastname", "firstname", "email@email.com", "P@ssword123word123");
        RoomShare roomShare = new RoomShare("123456", "name", owner);

        Roomie pendingRoomie = new Roomie("Doe", "Jane", "jane@email.com", "P@ssword123word123");
        pendingRoomie.sendJoinRequest(roomShare);

        Roomie acceptedRoomie = new Roomie("Doe", "John", "john@email.com", "P@ssword123word123", roomShare);

        List<Roomie> accepted = roomShare.getAcceptedRoomies();

        assertEquals(1, accepted.size(), "Only accepted roomies should be returned.");
        assertTrue(accepted.contains(acceptedRoomie), "The accepted roomie must be in the list.");
        assertFalse(accepted.contains(pendingRoomie), "The pending roomie must NOT be in the list.");
    }

    @Test
    void findRoomie_Found_Valid() {
        Owner owner = new Owner("lastname", "firstname", "email@email.com", "P@ssword123word123");
        RoomShare roomShare = new RoomShare("123456", "name", owner);

        Roomie acceptedRoomie = new Roomie("Doe", "John", "john@email.com", "P@ssword123word123", roomShare);
        acceptedRoomie.setIdPerson(99);

        assertEquals(acceptedRoomie, roomShare.findRoomie(99), "The method must return the corresponding accepted roomie.");
    }

    @Test
    void findRoomie_NotFound_Invalid() {
        Owner owner = new Owner("lastname", "firstname", "email@email.com", "P@ssword123word123");
        RoomShare roomShare = new RoomShare("123456", "name", owner);

        assertThrows(IllegalArgumentException.class, () -> {
            roomShare.findRoomie(99);
        }, "The method must throw an IllegalArgumentException if the roomie is not found in the accepted list.");
    }


    // ------------------------ GENERATE INVITATION KEY ------------------------

    @Test
    void generateInvitationKey_UniqueKey_Valid() {
        RoomShareDAO mockDao = mock(RoomShareDAO.class);
        when(mockDao.keyIsAlreadyUsed(anyString())).thenReturn(false);

        String generatedKey = RoomShare.generateInvitationKey(mockDao);

        assertNotNull(generatedKey);
        assertEquals(6, generatedKey.length());
        assertTrue(generatedKey.matches("[A-Z0-9]+"));
        verify(mockDao, times(1)).keyIsAlreadyUsed(generatedKey);
    }

    @Test
    void generateInvitationKey_FirstKeyUsed_Valid() {
        RoomShareDAO mockDao = mock(RoomShareDAO.class);
        when(mockDao.keyIsAlreadyUsed(anyString()))
                .thenReturn(true)
                .thenReturn(false);

        String generatedKey = RoomShare.generateInvitationKey(mockDao);

        assertNotNull(generatedKey);
        assertEquals(6, generatedKey.length());
        verify(mockDao, times(2)).keyIsAlreadyUsed(anyString());
    }


    // ------------------------ MANAGE SHOPPING LIST ITEMS ------------------------

    @Test
    void addItem_Valid() {
        ItemDAO mockItemDao = mock(ItemDAO.class);
        RoomShare roomShare = new RoomShare("123456", "name", new Owner("L", "F", "o@e.com", "P@ssword123"));

        Product mockProduct = mock(Product.class);
        when(mockProduct.getId()).thenReturn(1);

        roomShare.addItem(mockProduct, "2 kg", mockItemDao);

        assertEquals(1, roomShare.getItems().size(), "The item must be added to the list.");
        verify(mockItemDao, times(1)).create(any(Item.class));
    }

    @Test
    void addItem_AlreadyExists_Invalid() {
        ItemDAO mockItemDao = mock(ItemDAO.class);
        RoomShare roomShare = new RoomShare("123456", "name", new Owner("L", "F", "o@e.com", "P@ssword123"));

        Product mockProduct = mock(Product.class);
        when(mockProduct.getId()).thenReturn(1);

        roomShare.addItem(mockProduct, "2 kg", mockItemDao);

        assertThrows(ItemAlreadyExistsException.class, () -> {
            roomShare.addItem(mockProduct, "3 kg", mockItemDao);
        }, "Should throw an exception if the product is already in the list.");
    }

    @Test
    void modifyItem_Valid() {
        ItemDAO mockItemDao = mock(ItemDAO.class);
        RoomShare roomShare = new RoomShare("123456", "name", new Owner("L", "F", "o@e.com", "P@ssword123"));

        Product mockProduct = mock(Product.class);
        when(mockProduct.getId()).thenReturn(1);
        roomShare.addItem(mockProduct, "2 kg", mockItemDao);

        roomShare.modifyItem(1, "5 kg", true, mockItemDao);

        Item modifiedItem = roomShare.getItems().get(0);
        assertEquals("5 kg", modifiedItem.getQuantity(), "Quantity must be updated.");
        assertTrue(modifiedItem.getStatus(), "Status must be updated to true.");
        verify(mockItemDao, times(1)).update(any(Item.class));
    }

    @Test
    void removeItem_Valid() {
        ItemDAO mockItemDao = mock(ItemDAO.class);
        RoomShare roomShare = new RoomShare("123456", "name", new Owner("L", "F", "o@e.com", "P@ssword123"));

        Product mockProduct = mock(Product.class);
        when(mockProduct.getId()).thenReturn(1);
        roomShare.addItem(mockProduct, "2 kg", mockItemDao);

        roomShare.removeItem(1, mockItemDao);

        assertTrue(roomShare.getItems().isEmpty(), "The item must be removed from the memory list.");
        verify(mockItemDao, times(1)).delete(any(Item.class));
    }


    // ------------------------ CONFIRM PURCHASES ------------------------

    @Test
    void confirmShoppingListPurchase_Valid() {
        ItemDAO mockItemDao = mock(ItemDAO.class);
        ExpenseDAO mockExpenseDao = mock(ExpenseDAO.class);

        RoomShare roomShare = new RoomShare("123456", "name", new Owner("L", "F", "o@e.com", "P@ssword123"));
        roomShare.setId(10);

        Roomie buyer = new Roomie("Doe", "John", "1@e.com", "P@ssword123word123", roomShare);
        buyer.setIdPerson(99);

        Roomie flatmate = new Roomie("Doe", "Jane", "2@e.com", "P@ssword123word123", roomShare);
        flatmate.setIdPerson(100);

        Product mockProduct = mock(Product.class);
        when(mockProduct.getId()).thenReturn(1);

        roomShare.addItem(mockProduct, "2 kg", mockItemDao);
        roomShare.modifyItem(1, null, true, mockItemDao);

        when(mockItemDao.deleteCheckedItems(10)).thenReturn(1);

        roomShare.confirmShoppingListPurchase(99, "Courses", 50.0, mockExpenseDao, mockItemDao);

        assertTrue(roomShare.getItems().isEmpty(), "The checked item must be cleared from the list.");
        assertEquals(1, buyer.getExpensesToReceive().size(), "The buyer must receive the part of the other roomie.");
        assertEquals(1, flatmate.getExpensesToPay().size(), "The other roomie must owe his part to the buyer.");
        verify(mockExpenseDao, times(1)).createAll(anyList());
        verify(mockItemDao, times(1)).deleteCheckedItems(10);
    }

    @Test
    void confirmShoppingListPurchase_NoItemChecked_Invalid() {
        ExpenseDAO mockExpenseDao = mock(ExpenseDAO.class);
        ItemDAO mockItemDao = mock(ItemDAO.class);
        RoomShare roomShare = new RoomShare("123456", "name", new Owner("L", "F", "o@e.com", "P@ssword123"));

        assertThrows(NoItemCheckedException.class, () -> {
            roomShare.confirmShoppingListPurchase(99, "Courses", 50.0, mockExpenseDao, mockItemDao);
        }, "Should throw an exception if the list of checked items is empty.");
    }


    // ------------------------ MANAGE EVENTS ------------------------

    @Test
    void addEvent_Valid() {
        EventDAO mockEventDao = mock(EventDAO.class);
        RoomShare roomShare = new RoomShare("123456", "name", new Owner("L", "F", "o@e.com", "P@ssword123"));
        Event mockEvent = mock(Event.class);

        roomShare.addEvent(mockEvent, mockEventDao);

        assertEquals(1, roomShare.getEvents().size(), "Event must be added to the roomshare's list.");
        verify(mockEvent, times(1)).setRoomshare(roomShare);
        verify(mockEvent, times(1)).create(mockEventDao);
    }

    @Test
    void findEvents_Valid() {
        EventDAO mockEventDao = mock(EventDAO.class);
        RoomShare roomShare = new RoomShare("123456", "name", new Owner("L", "F", "o@e.com", "P@ssword123"));

        Event mockEvent1 = mock(Event.class);
        when(mockEvent1.getDate()).thenReturn(LocalDate.parse("2026-11-15"));

        Event mockEvent2 = mock(Event.class);
        when(mockEvent2.getDate()).thenReturn(LocalDate.parse("2026-11-25"));

        roomShare.addEvent(mockEvent1, mockEventDao);
        roomShare.addEvent(mockEvent2, mockEventDao);

        List<Event> found = roomShare.findEvents("2026-11-10", "2026-11-20");

        assertEquals(1, found.size(), "Only events within the dates should be returned.");
        assertEquals(mockEvent1, found.get(0));
    }


    // ------------------------ FIND MEMBERS ------------------------

    @Test // If the user who asks is the owner himself
    void findMembers_ByOwner_Valid() {
        PersonDAO mockPersonDao = mock(PersonDAO.class);

        Owner owner = new Owner("lastname", "firstname", "owner@email.com", "P@ssword123word123");
        owner.setIdPerson(1);

        Roomie acceptedRoomie = new Roomie("Doe", "John", "john@email.com", "P@ssword123word123");
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

        Owner owner = new Owner("lastname", "firstname", "owner@email.com", "P@ssword123word123");
        owner.setIdPerson(1);
        RoomShare mockRs = new RoomShare("123456", "name", owner);

        Roomie roomie = new Roomie("Doe", "John", "john@email.com", "P@ssword123word123", mockRs);

        when(mockPersonDao.findByEmail("john@email.com")).thenReturn(roomie);
        when(mockPersonDao.findAcceptedRoomiesByOwner(1)).thenReturn((List) Collections.singletonList(roomie));

        List<Person> result = RoomShare.findMembers("john@email.com", mockPersonDao);

        assertEquals(2, result.size(), "The list must contain the owner and the roomie himself.");
        assertTrue(result.contains(owner), "The owner must be the first in the members list.");
    }

    @Test // If the user who asks is a roomie but is not accepted (e.g. PENDING or NONE)
    void findMembers_ByPendingRoomie_Invalid() {
        PersonDAO mockPersonDao = mock(PersonDAO.class);

        Owner owner = new Owner("lastname", "firstname", "owner@email.com", "P@ssword123word123");
        RoomShare mockRs = new RoomShare("123456", "name", owner);

        Roomie pendingRoomie = new Roomie("Doe", "John", "john@email.com", "P@ssword123word123");
        pendingRoomie.sendJoinRequest(mockRs);

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