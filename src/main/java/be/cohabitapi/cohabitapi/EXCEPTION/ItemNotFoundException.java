package be.cohabitapi.cohabitapi.EXCEPTION;

public class ItemNotFoundException extends RuntimeException {

    //--------------- CHECK AN ITEM -------------------
    // If the combo of the room share id and the product id returns nothing
    public ItemNotFoundException() {
        super("The item received does not exist in the list.");
    }
}
