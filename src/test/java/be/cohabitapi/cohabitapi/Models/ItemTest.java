package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.ItemDAO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemTest {

    Item item = new Item();
    Product product = new Product("Apple", "Food");
    RoomShare roomShare = new RoomShare();


    // ------------------------ SET A PRODUCT ------------------------

    @Test // If you try a right product
    void setProduct_Valid() {
        item.setProduct(product);
        assertEquals(product, item.getProduct(), "The product must be correctly assigned and retrievable.");
    }

    @Test // If you try a product with a null value
    void setProduct_WithNullValue_Invalid() {
        assertThrows(NullPointerException.class, () -> {
            item.setProduct(null);
        }, "The setter must throw a NullPointerException if the product is null");
    }


    // ------------------------ SET A ROOM SHARE ------------------------

    @Test // If you try a right room share
    void setRoomShare_Valid() {
        item.setRoomShare(roomShare);
        assertEquals(roomShare, item.getRoomShare(), "The room share must be correctly assigned and retrievable.");
    }

    @Test // If you try a room share with a null value
    void setRoomShare_WithNullValue_Invalid() {
        assertThrows(NullPointerException.class, () -> {
            item.setRoomShare(null);
        }, "The setter must throw a NullPointerException if the room share is null");
    }


    // ------------------------ SET A QUANTITY ------------------------

    @Test // If you try a right quantity
    void setQuantity_Valid() {
        item.setQuantity("2 Liters");
        assertEquals("2 Liters", item.getQuantity(), "The quantity must be correctly assigned and retrievable.");
    }

    @Test // If you try a quantity with a null value
    void setQuantity_WithNullValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            item.setQuantity(null);
        }, "The setter must throw an IllegalArgumentException if the quantity is null");
    }

    @Test // If you try a quantity with an empty value
    void setQuantity_WithEmptyValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            item.setQuantity("");
        }, "The setter must throw an IllegalArgumentException if the quantity is empty");
    }

    @Test // If you try a quantity with a too long value
    void setQuantity_WithTooLongValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            item.setQuantity("123456789012345678901234567890123456789012345678901");
        }, "The setter must throw an IllegalArgumentException if the quantity is too long");
    }


    // ------------------------ SET A STATUS ------------------------

    @Test // If you try a right status
    void setStatus_Valid() {
        assertFalse(item.getStatus(), "By default, the status must be false upon instantiation.");

        item.setStatus(true);
        assertTrue(item.getStatus(), "The status must be correctly assigned to true.");

        item.setStatus(false);
        assertFalse(item.getStatus(), "The status must be able to go back to false.");
    }


    // ------------------------ CONSTRUCTOR ------------------------

    @Test // If you instantiate with all valid parameters
    void constructor_Valid() {
        Item newItem = new Item(product, roomShare, "3 packs");

        assertEquals(product, newItem.getProduct(), "The product must be correctly assigned by the constructor.");
        assertEquals(roomShare, newItem.getRoomShare(), "The room share must be correctly assigned by the constructor.");
        assertEquals("3 packs", newItem.getQuantity(), "The quantity must be correctly assigned by the constructor.");
        assertFalse(newItem.getStatus(), "The status must be false by default.");
    }

    @Test // If you instantiate with a null product
    void constructor_WithNullProduct_Invalid() {
        assertThrows(NullPointerException.class, () -> {
            new Item(null, roomShare, "3 packs");
        }, "The constructor must throw a NullPointerException if the product is null");
    }

    @Test // If you instantiate with a null room share
    void constructor_WithNullRoomShare_Invalid() {
        assertThrows(NullPointerException.class, () -> {
            new Item(product, null, "3 packs");
        }, "The constructor must throw a NullPointerException if the room share is null");
    }

    @Test // If you instantiate with a null quantity
    void constructor_WithNullQuantity_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Item(product, roomShare, null);
        }, "The constructor must throw an IllegalArgumentException if the quantity is null");
    }

    @Test // If you instantiate with an empty quantity
    void constructor_WithEmptyQuantity_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Item(product, roomShare, "");
        }, "The constructor must throw an IllegalArgumentException if the quantity is empty");
    }


    // ------------------------ DAO METHODS ------------------------

    @Test // If you create the item in the db
    void create_Valid() {
        ItemDAO mockItemDao = mock(ItemDAO.class);
        Item newItem = new Item(product, roomShare, "3 packs");

        newItem.create(mockItemDao);

        verify(mockItemDao, times(1)).create(newItem);
    }

    @Test // If you update the item in the db
    void update_Valid() {
        ItemDAO mockItemDao = mock(ItemDAO.class);
        Item newItem = new Item(product, roomShare, "3 packs");

        newItem.update(mockItemDao);

        verify(mockItemDao, times(1)).update(newItem);
    }

    @Test // If you delete the item from the db
    void delete_Valid() {
        ItemDAO mockItemDao = mock(ItemDAO.class);
        Item newItem = new Item(product, roomShare, "3 packs");

        newItem.delete(mockItemDao);

        verify(mockItemDao, times(1)).delete(newItem);
    }


    // ------------------------ EQUALS & HASHCODE ------------------------

    @Test // An item is identified by its product and its room share (the two ids)
    void equalsAndHashCode_Valid() {
        Product apple = new Product("Apple", "Food");
        apple.setId(1);
        Product sameApple = new Product("Green apple", "Fruit");
        sameApple.setId(1);
        Product milk = new Product("Milk", "Dairy");
        milk.setId(2);

        RoomShare rs1 = new RoomShare();
        rs1.setId(10);
        RoomShare rs2 = new RoomShare();
        rs2.setId(20);

        Item item1 = new Item(apple, rs1, "1 kg");
        Item item2 = new Item(sameApple, rs1, "5 kg");
        item2.setStatus(true);
        Item item3 = new Item(milk, rs1, "1 kg");
        Item item4 = new Item(apple, rs2, "1 kg");

        assertEquals(item1, item2, "Two items with the same product and the same room share must be equal, whatever the quantity or the status.");
        assertEquals(item1.hashCode(), item2.hashCode(), "Two equal items must have the same hash code.");
        assertNotEquals(item1, item3, "Items with different products must not be equal.");
        assertNotEquals(item1, item4, "Items with different room shares must not be equal.");
    }

    @Test // If you compare an item with itself, with null or with something that is not an item
    void equals_WithNullOrOtherClass_Valid() {
        Item newItem = new Item(product, roomShare, "3 packs");

        assertEquals(newItem, newItem, "An item must be equal to itself.");
        assertNotEquals(null, newItem, "An item must never be equal to null.");
        assertNotEquals("3 packs", newItem, "An item must never be equal to an object of another class.");
    }
}