package be.cohabitapi.cohabitapi.EXCEPTION;

// This exception is used when a roomie has no roomshare
// // or when the owner did not accept him yet
// We do this because only an accepted member can see and add expenses
// The API turns it into a 403
public class NotInRoomshareException extends RuntimeException {

    public NotInRoomshareException() {
        super("You must be an accepted member of a roomshare.");
    }
}