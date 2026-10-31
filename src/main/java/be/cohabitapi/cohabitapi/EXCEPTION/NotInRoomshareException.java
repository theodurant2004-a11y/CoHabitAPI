package be.cohabitapi.cohabitapi.EXCEPTION;

// Thrown when a roomie wants to manage tasks but has no roomshare or his join request is still pending
// The API turns it into a 403 error
public class NotInRoomshareException extends RuntimeException {

    public NotInRoomshareException() {
        super("You must be an accepted member of a roomshare.");
    }
}
