package be.cohabitapi.cohabitapi.EXCEPTION;

// Thrown when no roomshare uses the invitation key sent by the roomie
// The API turns it into a 404 error
public class InvalidInvitationKeyException extends RuntimeException {

    public InvalidInvitationKeyException() {
        super("No roomshare uses this invitation key.");
    }
}