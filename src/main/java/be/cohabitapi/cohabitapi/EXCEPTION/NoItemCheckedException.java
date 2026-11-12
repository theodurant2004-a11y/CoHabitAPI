package be.cohabitapi.cohabitapi.EXCEPTION;

public class ProductNotFoundException extends RuntimeException {

    //--------------- CHECK AN ITEM -------------------
    // If the id of the product returns nothing
    public ProductNotFoundException() {
        super("The product received does not exist in the list.");
    }
}
