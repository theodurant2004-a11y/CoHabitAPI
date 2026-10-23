package be.cohabitapi.cohabitapi.EXCEPTION;


// Thrown when a roomie asks to join a roomshare but already has one
// The API turns it into a 409 error
// RuntimeException: it is not mandatory to declare it with "throws" in every method.
public class AlreadyInRoomshareException extends RuntimeException {
    public AlreadyInRoomshareException() {
        super("The roomie already belongs to a roomshare or has a pending request.");
    }
}
