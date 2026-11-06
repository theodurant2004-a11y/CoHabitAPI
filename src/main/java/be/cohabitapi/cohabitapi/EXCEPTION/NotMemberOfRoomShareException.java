package be.cohabitapi.cohabitapi.EXCEPTION;

public class NotMemberOfRoomShareException extends RuntimeException {

    //--------------- GET ITEM -------------------
    // If the user member status is not at ACCEPTED
    public NotMemberOfRoomShareException() {
        super("You're not a member of a room share.");
    }
}
