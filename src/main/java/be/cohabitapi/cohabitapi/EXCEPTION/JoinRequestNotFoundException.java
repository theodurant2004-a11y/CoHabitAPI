package be.cohabitapi.cohabitapi.EXCEPTION;

//--------------- ACCEPT A ROOMIE -------------------
// Thrown when the owner answers a join request that does not exist:
//   - nobody has this id
//   - the id is not a roomie (an owner for example)
//   - the roomie asked to join the roomshare of ANOTHER owner
// The API turns it into a 404 error
// Same error for the 3 cases on purpose: an owner cannot find out what happens in the other roomshares

public class JoinRequestNotFoundException extends RuntimeException {
    public JoinRequestNotFoundException() {
        super("This join request does not exist.");
    }
}
