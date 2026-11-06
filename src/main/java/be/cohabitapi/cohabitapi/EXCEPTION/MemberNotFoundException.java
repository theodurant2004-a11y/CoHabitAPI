package be.cohabitapi.cohabitapi.EXCEPTION;

//--------------- REMOVE A ROOMIE -------------------
// Thrown when the owner wants to remove a roomie who is not a member of his roomshare:
//   - nobody has this id, or the id is not a roomie
//   - the roomie is in the roomshare of ANOTHER owner
//   - the roomie is not accepted (no roomshare, or his request is still pending)
// The API turns it into a 404 error
// Same error for every case on purpose: an owner cannot find out what happens in the other roomshares
public class MemberNotFoundException extends RuntimeException {

    public MemberNotFoundException() {
        // The message sent to the front
        super("This roomie is not a member of your roomshare.");
    }
}