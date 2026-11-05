package be.cohabitapi.cohabitapi.EXCEPTION;

public class NoItemInTheListException extends RuntimeException {
    //--------------- GET SHOPPING LIST -------------------
    // If no item is present in the list
    public NoItemInTheListException() { super("No items is present in the shopping list."); }
}
