package be.cohabitapi.cohabitapi.EXCEPTION;

public class UnauthorizedUserException extends RuntimeException{

//--------------- CREATE A ROOMSHARE -------------------
    // Role exception
    public UnauthorizedUserException() {
        super("Type of user unauthorized to browse here");
    }
}
