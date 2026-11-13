package be.cohabitapi.cohabitapi.EXCEPTION;

// Thrown when the owner publishes a rent or a bill that already exists
// (same name, same category and same due date). The API turns it into a 409
public class ChargeAlreadyExistsException extends RuntimeException {

    public ChargeAlreadyExistsException() {
        super("A charge with this name and this due date already exists.");
    }
}
