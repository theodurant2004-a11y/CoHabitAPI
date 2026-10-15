package be.cohabitapi.cohabitapi.EXCEPTION;

public class UserNotFoundException extends RuntimeException{

//--------------- CREATE A ROOMSHARE -------------------
    // Request user email exception
    public UserNotFoundException() {
        super("The received user does not exist");
    }
}
