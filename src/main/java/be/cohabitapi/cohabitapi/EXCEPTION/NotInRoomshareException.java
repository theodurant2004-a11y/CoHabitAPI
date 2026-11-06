package be.cohabitapi.cohabitapi.EXCEPTION;

// Thrown when a roomie has no roomshare or when the owner did not accept him yet
// Only an accepted member can manage the tasks and the expenses
// The API turns it into a 403
public class NotInRoomshareException extends RuntimeException {

    public NotInRoomshareException() {
        super("You must be an accepted member of a roomshare.");
    }
}