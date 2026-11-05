package be.cohabitapi.cohabitapi.EXCEPTION;

public class NoItemCheckedException extends RuntimeException {
    //--------------- CONFIRM PURCHASES -------------------
    // If no item is checked in the list
    public NoItemCheckedException() { super("No item is checked."); }
}
