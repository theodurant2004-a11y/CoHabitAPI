package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.ProductDAO;
import be.cohabitapi.cohabitapi.EXCEPTION.ProductNotFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductTest {

    Product product = new Product();

    // ------------------------ SET AN ID ------------------------

    @Test // If you try a right id
    void setId_Valid() {
        product.setId(1);
        assertEquals(1, product.getId(), "The id must be correctly assigned and retrievable.");
    }


    // ------------------------ SET A NAME ------------------------

    @Test // If you try a right name
    void setName_Valid() {
        product.setName("Apple");
        assertEquals("Apple", product.getName(), "The name must be correctly assigned and retrievable.");
    }

    @Test // If you try a name with a null value
    void setName_WithNullValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            product.setName(null);
        }, "The setter must throw an IllegalArgumentException if the name is null");
    }

    @Test // If you try a name with an empty value
    void setName_WithEmptyValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            product.setName("   ");
        }, "The setter must throw an IllegalArgumentException if the name is empty");
    }

    @Test // If you try a name with a too long value
    void setName_WithTooLongValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            // String with 101 characters
            product.setName("12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901");
        }, "The setter must throw an IllegalArgumentException if the name is too long (over 100 characters)");
    }


    // ------------------------ SET A CATEGORY ------------------------

    @Test // If you try a right category
    void setCategory_Valid() {
        product.setCategory("Food");
        assertEquals("Food", product.getCategory(), "The category must be correctly assigned and retrievable.");
    }

    @Test // If you try a category with a null value
    void setCategory_WithNullValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            product.setCategory(null);
        }, "The setter must throw an IllegalArgumentException if the category is null");
    }

    @Test // If you try a category with an empty value
    void setCategory_WithEmptyValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            product.setCategory("");
        }, "The setter must throw an IllegalArgumentException if the category is empty");
    }

    @Test // If you try a category with a too long value
    void setCategory_WithTooLongValue_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            // String with 101 characters
            product.setCategory("12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901");
        }, "The setter must throw an IllegalArgumentException if the category is too long (over 100 characters)");
    }


    // ------------------------ CONSTRUCTOR ------------------------

    @Test // If you instantiate with all valid parameters
    void constructor_Valid() {
        Product newProduct = new Product("Milk", "Drinks");

        assertEquals("Milk", newProduct.getName());
        assertEquals("Drinks", newProduct.getCategory());
        assertNull(newProduct.getId()); // We check that the ID is null before the db insertion
    }


    // ------------------------ FIND OR CREATE ------------------------

    @Test // If the product is found in db, it returns it without creating a new one
    void findOrCreate_ProductExists_Valid() {
        ProductDAO mockDao = mock(ProductDAO.class);
        Product existingProduct = new Product("Apple", "Food");

        when(mockDao.findByNameAndCategory("Apple", "Food")).thenReturn(existingProduct);

        Product result = Product.findOrCreate(mockDao, "Apple", "Food");

        assertEquals(existingProduct, result, "The method must return the existing product.");

        verify(mockDao, never()).create(any(Product.class)); // Verify that the create method from the DAO was never called
    }

    @Test // If the product is not found in db, it creates it and returns it
    void findOrCreate_ProductDoesNotExist_Valid() {
        ProductDAO mockDao = mock(ProductDAO.class);

        when(mockDao.findByNameAndCategory("Apple", "Food")).thenReturn(null);

        Product result = Product.findOrCreate(mockDao, "Apple", "Food");

        assertNotNull(result, "The method must return a newly created product.");
        assertEquals("Apple", result.getName());
        assertEquals("Food", result.getCategory());

        // Verify that the create method from the DAO was called exactly 1 time
        verify(mockDao, times(1)).create(any(Product.class));
    }


    // ------------------------ FIND BY ID ------------------------

    @Test // If the product is successfully found by its ID
    void findById_Found_Valid() {
        ProductDAO mockDao = mock(ProductDAO.class);
        Product existingProduct = new Product("Apple", "Food");

        when(mockDao.findById(1)).thenReturn(existingProduct);

        Product result = Product.findById(mockDao, 1);

        assertEquals(existingProduct, result, "The method must return the product if it exists.");
    }

    @Test // If the product is not found by its ID
    void findById_NotFound_Invalid() {
        ProductDAO mockDao = mock(ProductDAO.class);

        when(mockDao.findById(1)).thenReturn(null);

        assertThrows(ProductNotFoundException.class, () -> {
            Product.findById(mockDao, 1);
        }, "The method must throw a ProductNotFoundException if the DAO returns null.");
    }
}