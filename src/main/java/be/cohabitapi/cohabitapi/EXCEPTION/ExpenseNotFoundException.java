package be.cohabitapi.cohabitapi.EXCEPTION;

// Thrown when the expense does not exist. The API turns it into a 404
public class ExpenseNotFoundException extends RuntimeException {

    public ExpenseNotFoundException() {
        super("This expense does not exist.");
    }
}