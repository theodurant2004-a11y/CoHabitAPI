package be.cohabitapi.cohabitapi.EXCEPTION;

// Thrown when the user exists but his role cannot do the action, for ex : an owner cannot join a roomshare
// The API turns it into a 403 error
public class ForbiddenRoleException extends RuntimeException {

    public ForbiddenRoleException() {
        super("This type of user cannot do this action.");
    }
}