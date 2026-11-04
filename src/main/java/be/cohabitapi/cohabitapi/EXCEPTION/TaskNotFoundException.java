package be.cohabitapi.cohabitapi.EXCEPTION;

public class TaskNotFoundException extends RuntimeException {
    // Trhow an exception when the task isn't found or when it's not in the roomshare
    public TaskNotFoundException() {

        super("The received task does not exist.");
    }
}
