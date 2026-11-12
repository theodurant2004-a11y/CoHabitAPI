package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.ItemDAO;
import be.cohabitapi.cohabitapi.DAO.ReceiptScanner;
import be.cohabitapi.cohabitapi.EXCEPTION.ReceiptScanUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// These tests check the scan of a receipt: RoomShare.scanReceipt and the rules of ReceiptScan
// The scanner is a mock: no real call to the AI is made, each test decides what the AI "reads" on the receipt
// The ItemDAO is a mock too: nothing is saved in the db
@ExtendWith(MockitoExtension.class)
class ReceiptScanTest {

    // Any text is enough: the mock does not read the photo
    private static final String IMAGE = "cGhvdG8=";

    @Mock
    private ReceiptScanner scanner;

    @Mock
    private ItemDAO itemDAO;

    private RoomShare roomshare;

    @BeforeEach
    void createRoomshare() {
        Owner owner = new Owner("Doe", "Alice", "alice@test.be", "Test1234!");
        roomshare = new RoomShare("ABC123", "Belleville", owner);
    }

    // Adds an unchecked item to the list of the roomshare
    // The product is a REAL object. It receives its id by hand, like the db would do
    private void addItem(int productId, String name) {
        Product product = new Product(name, "Food");
        product.setId(productId);

        roomshare.addItem(product, "1", itemDAO);
    }


    // ------------------------- scanReceipt: the ids --------------------------------------------------

    @Test
    @DisplayName("scanReceipt: ids of the list found by the AI -> kept, with the total")
    void scanReceipt_idsOfTheList_areKept() {
        // Arrange: two items to buy, the AI finds both and reads the total
        addItem(1, "Milk");
        addItem(2, "Bread");
        when(scanner.scan(anyString(), anyList())).thenReturn(new ReceiptScan(Arrays.asList(1, 2), 23.45));

        // Act
        ReceiptScan scan = roomshare.scanReceipt(IMAGE, scanner);

        // Assert
        assertEquals(Arrays.asList(1, 2), scan.getItemIds());
        assertEquals(23.45, scan.getTotal());
    }

    @Test
    @DisplayName("scanReceipt: id that is not in the list -> ignored")
    void scanReceipt_unknownId_isIgnored() {
        // Arrange: the AI answers with the id 99, which is not an item of this roomshare
        addItem(1, "Milk");
        addItem(2, "Bread");
        when(scanner.scan(anyString(), anyList())).thenReturn(new ReceiptScan(Arrays.asList(1, 99), 23.45));

        // Act
        ReceiptScan scan = roomshare.scanReceipt(IMAGE, scanner);

        // Assert: only the id of the list is kept
        assertEquals(Arrays.asList(1), scan.getItemIds());
    }

    @Test
    @DisplayName("scanReceipt: id of an item already checked -> ignored")
    void scanReceipt_idOfACheckedItem_isIgnored() {
        // Arrange: the bread is already checked, but the AI answers with its id anyway
        addItem(1, "Milk");
        addItem(2, "Bread");
        roomshare.modifyItem(2, null, true, itemDAO);
        when(scanner.scan(anyString(), anyList())).thenReturn(new ReceiptScan(Arrays.asList(1, 2), null));

        // Act
        ReceiptScan scan = roomshare.scanReceipt(IMAGE, scanner);

        // Assert: the scan only proposes items that are still to buy
        assertEquals(Arrays.asList(1), scan.getItemIds());
    }

    @Test
    @DisplayName("scanReceipt: same id twice -> kept once")
    void scanReceipt_sameIdTwice_isKeptOnce() {
        // Arrange
        addItem(1, "Milk");
        when(scanner.scan(anyString(), anyList())).thenReturn(new ReceiptScan(Arrays.asList(1, 1), null));

        // Act
        ReceiptScan scan = roomshare.scanReceipt(IMAGE, scanner);

        // Assert
        assertEquals(Arrays.asList(1), scan.getItemIds());
    }

    @Test
    @DisplayName("scanReceipt: the scanner receives the photo and only the unchecked items")
    void scanReceipt_sendsOnlyTheUncheckedItems() {
        // Arrange: the bread is already checked
        addItem(1, "Milk");
        addItem(2, "Bread");
        roomshare.modifyItem(2, null, true, itemDAO);
        when(scanner.scan(anyString(), anyList())).thenReturn(new ReceiptScan(new ArrayList<>(), null));

        // Act
        roomshare.scanReceipt(IMAGE, scanner);

        // Assert: one item was sent, the milk
        // argThat: the argument must make this condition true
        verify(scanner).scan(eq(IMAGE), argThat(items ->
                items.size() == 1 && items.get(0).getProduct().getId() == 1));
    }


    // ------------------------- scanReceipt: the total --------------------------------------------------

    @Test
    @DisplayName("scanReceipt: negative total -> null")
    void scanReceipt_negativeTotal_becomesNull() {
        // Arrange: the AI read a total that cannot be a real amount
        addItem(1, "Milk");
        when(scanner.scan(anyString(), anyList())).thenReturn(new ReceiptScan(Arrays.asList(1), -5.0));

        // Act
        ReceiptScan scan = roomshare.scanReceipt(IMAGE, scanner);

        // Assert: the total is forgotten, but the items are still proposed
        assertNull(scan.getTotal());
        assertEquals(Arrays.asList(1), scan.getItemIds());
    }

    @Test
    @DisplayName("scanReceipt: total that could not be read -> null")
    void scanReceipt_noTotal_staysNull() {
        // Arrange
        addItem(1, "Milk");
        when(scanner.scan(anyString(), anyList())).thenReturn(new ReceiptScan(Arrays.asList(1), null));

        // Act
        ReceiptScan scan = roomshare.scanReceipt(IMAGE, scanner);

        // Assert
        assertNull(scan.getTotal());
    }


    // ------------------------- scanReceipt: the AI is not called or cannot answer ----------------------

    @Test
    @DisplayName("scanReceipt: the scanner is unavailable -> ReceiptScanUnavailableException")
    void scanReceipt_scannerUnavailable_throws() {
        // Arrange: the AI is down, the quota is exceeded or the answer cannot be read
        addItem(1, "Milk");
        when(scanner.scan(anyString(), anyList())).thenThrow(new ReceiptScanUnavailableException());

        // Act + Assert: the exception goes up to the API, which answers 503
        assertThrows(ReceiptScanUnavailableException.class, () -> roomshare.scanReceipt(IMAGE, scanner));
    }

    @Test
    @DisplayName("scanReceipt: empty list -> empty proposal, the AI is not called")
    void scanReceipt_emptyList_doesNotCallTheScanner() {
        // Act: the roomshare has no item
        ReceiptScan scan = roomshare.scanReceipt(IMAGE, scanner);

        // Assert
        assertTrue(scan.getItemIds().isEmpty());
        assertNull(scan.getTotal());
        // verifyNoInteractions: no method of the mock was called
        verifyNoInteractions(scanner);
    }

    @Test
    @DisplayName("scanReceipt: every item already checked -> empty proposal, the AI is not called")
    void scanReceipt_everythingChecked_doesNotCallTheScanner() {
        // Arrange: the only item of the list is checked
        addItem(1, "Milk");
        roomshare.modifyItem(1, null, true, itemDAO);

        // Act
        ReceiptScan scan = roomshare.scanReceipt(IMAGE, scanner);

        // Assert
        assertTrue(scan.getItemIds().isEmpty());
        assertNull(scan.getTotal());
        verifyNoInteractions(scanner);
    }

    // @NullAndEmptySource gives null and "", @ValueSource adds an image with only spaces
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("scanReceipt: no image -> IllegalArgumentException, the AI is not called")
    void scanReceipt_noImage_throws(String image) {
        // Arrange
        addItem(1, "Milk");

        // Act + Assert: the API turns it into a 400
        assertThrows(IllegalArgumentException.class, () -> roomshare.scanReceipt(image, scanner));
        verifyNoInteractions(scanner);
    }


    // ------------------------- ReceiptScan: the rules of its setters -----------------------------------

    @Test
    @DisplayName("ReceiptScan: valid values -> kept")
    void receiptScan_validValues_areKept() {
        ReceiptScan scan = new ReceiptScan(Arrays.asList(1, 4), 23.45);

        assertEquals(Arrays.asList(1, 4), scan.getItemIds());
        assertEquals(23.45, scan.getTotal());
    }

    @Test
    @DisplayName("ReceiptScan: total 0 -> kept (only a negative total is refused)")
    void receiptScan_zeroTotal_isKept() {
        ReceiptScan scan = new ReceiptScan(new ArrayList<>(), 0.0);

        assertEquals(0.0, scan.getTotal());
    }

    @Test
    @DisplayName("ReceiptScan: negative total -> null")
    void receiptScan_negativeTotal_becomesNull() {
        ReceiptScan scan = new ReceiptScan(new ArrayList<>(), -0.01);

        assertNull(scan.getTotal());
    }

    @Test
    @DisplayName("ReceiptScan: total that is not a number (NaN) -> null")
    void receiptScan_notANumberTotal_becomesNull() {
        ReceiptScan scan = new ReceiptScan(new ArrayList<>(), Double.NaN);

        assertNull(scan.getTotal());
    }

    @Test
    @DisplayName("ReceiptScan: no list of ids -> empty list")
    void receiptScan_nullIds_becomesEmptyList() {
        ReceiptScan scan = new ReceiptScan(null, null);

        assertTrue(scan.getItemIds().isEmpty());
    }

    @Test
    @DisplayName("ReceiptScan: the list of ids is read only")
    void receiptScan_itemIds_cannotBeChangedFromOutside() {
        // Arrange
        List<Integer> ids = new ArrayList<>(Arrays.asList(1, 4));
        ReceiptScan scan = new ReceiptScan(ids, null);

        // Act: the list given to the constructor is changed afterwards
        ids.add(99);

        // Assert: the scan kept its own copy, and its getter gives a list that cannot be changed
        assertEquals(Arrays.asList(1, 4), scan.getItemIds());
        assertThrows(UnsupportedOperationException.class, () -> scan.getItemIds().add(99));
    }
}
