package be.cohabitapi.cohabitapi.EXCEPTION;


// Thrown when we sure that the cause is uniqueness constraint violation
// in the case of the addition of an already existing item in the list
public class ItemAlreadyExistsException extends RuntimeException {
    public ItemAlreadyExistsException() {
        super("This item already exists in the list of the room share. Please, edit it.");
    }
}
