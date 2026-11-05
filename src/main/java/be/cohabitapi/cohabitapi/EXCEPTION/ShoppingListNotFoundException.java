package be.cohabitapi.cohabitapi.EXCEPTION;

public class ShoppingListNotFoundException extends RuntimeException {

    //--------------- CHECK AN ITEM -------------------
    // If the shopping list id returns nothing
    public ShoppingListNotFoundException() {
        super("The shopping list received does not exist.");
    }
}
