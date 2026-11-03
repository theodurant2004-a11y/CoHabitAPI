package be.cohabitapi.cohabitapi.EXCEPTION;

public class TaskNotFoundException extends RuntimeException {
    // GETTASKS
    public TaskNotFoundException() {

        super("The received task does not exist.");
    }
}
