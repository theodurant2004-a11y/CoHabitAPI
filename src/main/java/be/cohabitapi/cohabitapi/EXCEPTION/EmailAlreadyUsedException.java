package be.cohabitapi.cohabitapi.EXCEPTION;

public class EmailAlreadyUsedException extends RuntimeException {

    //--------------- SIGNUP -------------------
    // The email is already in the database
    public EmailAlreadyUsedException() {
        super("Email already used.");
    }
}
