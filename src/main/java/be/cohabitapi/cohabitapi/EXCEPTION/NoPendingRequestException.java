package be.cohabitapi.cohabitapi.EXCEPTION;

//--------------- ACCEPT A ROOMIE -------------------
// Thrown when the owner answers a request that is not waiting anymore
// for example: he clicks two times on "Accept", or the roomie is already a member
// The API turns it into a 409 error (conflict with the current state of the data)
// RuntimeException: it is not mandatory to declare it with "throws" in every method

public class NoPendingRequestException extends RuntimeException {
    public NoPendingRequestException() {
        super("This join request is not pending anymore.");
    }
}
