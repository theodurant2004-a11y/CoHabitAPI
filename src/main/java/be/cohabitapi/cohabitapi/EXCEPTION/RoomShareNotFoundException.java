package be.cohabitapi.cohabitapi.EXCEPTION;

public class RoomShareNotFoundException extends RuntimeException {

    //--------------- ADD AN ITEM -------------------
    public RoomShareNotFoundException() {
        super("The room share does not exist");
    }
}
