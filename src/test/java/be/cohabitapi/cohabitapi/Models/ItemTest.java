package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.ItemDAO;
import be.cohabitapi.cohabitapi.EXCEPTION.ItemNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
    }


    // ------------------------ CONSTRUCTOR ------------------------

    @Test // If you instantiate with all valid parameters
    void constructor_Valid() {
        Item newItem = new Item(product, roomShare, "3 packs");

        assertEquals(product, newItem.getProduct());
        assertEquals(roomShare, newItem.getRoomShare());
        assertEquals("3 packs", newItem.getQuantity());
        assertFalse(newItem.getStatus());
    }


    // ------------------------ FIND BY ID ------------------------

    @Test // If the item is successfully found
    void findById_Found_Valid() {
        ItemDAO mockDao = mock(ItemDAO.class);
        when(mockDao.findById(1, 2)).thenReturn(item);

        Item result = Item.findById(mockDao, 1, 2);

        assertEquals(item, result, "The method must return the item if it exists.");
    }

    @Test // If the item is not found
    void findById_NotFound_Invalid() {
        ItemDAO mockDao = mock(ItemDAO.class);
        when(mockDao.findById(1, 2)).thenReturn(null);

        assertThrows(ItemNotFoundException.class, () -> {
            Item.findById(mockDao, 1, 2);
        }, "The method must throw an ItemNotFoundException if the DAO returns null.");
    }


    // ------------------------ FIND ALL ------------------------

    @Test // If the list of items is found (even if empty, it just returns it now)
    void findAll_Valid() {
        ItemDAO mockDao = mock(ItemDAO.class);
        List<Item> mockList = new ArrayList<>();
        when(mockDao.findAll(2)).thenReturn(mockList);

        List<Item> result = Item.findAll(mockDao, 2);

        assertEquals(mockList, result, "The method must return the list of items directly.");
    }


    // ------------------------ VERIFY HAS CHECKED ITEMS ------------------------

    @Test // If there is at least 1 checked item
    void verifyHasCheckedItems_Valid() {
        ItemDAO mockDao = mock(ItemDAO.class);
        when(mockDao.countCheckedItems(2)).thenReturn(1L);

        assertDoesNotThrow(() -> {
            Item.verifyHasCheckedItems(mockDao, 2);
        }, "The method must execute silently if at least 1 item is checked.");
    }

    @Test // If there are 0 checked items (verify method)
    void verifyHasCheckedItems_NoItemsChecked_Invalid() {
        ItemDAO mockDao = mock(ItemDAO.class);
        when(mockDao.countCheckedItems(2)).thenReturn(0L);

        assertThrows(IllegalStateException.class, () -> {
            Item.verifyHasCheckedItems(mockDao, 2);
        }, "The method must throw an IllegalStateException if 0 items are checked.");
    }


    // ------------------------ DELETE CHECKED ITEMS ------------------------

    @Test // If the deletion affects at least 1 item
    void deleteCheckedItems_Valid() {
        ItemDAO mockDao = mock(ItemDAO.class);
        when(mockDao.deleteCheckedItems(2)).thenReturn(3);

        assertDoesNotThrow(() -> {
            Item.deleteCheckedItems(mockDao, 2);
        }, "The method must execute silently if at least 1 item was deleted.");
    }

    @Test // If the deletion affects 0 items
    void deleteCheckedItems_NoItemsDeleted_Invalid() {
        ItemDAO mockDao = mock(ItemDAO.class);
        when(mockDao.deleteCheckedItems(2)).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> {
            Item.deleteCheckedItems(mockDao, 2);
        }, "The method must throw an IllegalStateException if no items were deleted.");
    }
}