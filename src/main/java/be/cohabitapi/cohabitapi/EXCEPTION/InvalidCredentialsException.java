package be.cohabitapi.cohabitapi.EXCEPTION;

public class InvalidCredentialsException extends RuntimeException{

//--------------- LOGIN -------------------
    //Password/email exption
    public InvalidCredentialsException() {
        super("Email or password is wrong");
    }
}
