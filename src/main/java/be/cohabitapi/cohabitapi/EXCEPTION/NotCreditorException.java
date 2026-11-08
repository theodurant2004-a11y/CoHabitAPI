package be.cohabitapi.cohabitapi.EXCEPTION;

// Thrown when someone who is not owed the money tries to settle the expense. The API turns it into a 403
public class NotCreditorException extends RuntimeException {

    public NotCreditorException() {
        super("Only the person who is owed can mark this expense as paid.");
    }
}