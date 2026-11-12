package be.cohabitapi.cohabitapi.EXCEPTION;

public class NoItemCheckedException extends RuntimeException {

    //---------------  ------------CONFIRM THE PURCHASES-------
    // If no item in the list has the status as found
    public NoItemCheckedException() { super("No item is checked."); }
}
