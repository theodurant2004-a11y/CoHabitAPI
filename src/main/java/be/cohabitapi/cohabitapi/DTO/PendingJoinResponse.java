package be.cohabitapi.cohabitapi.DTO;

// One request waiting for the answer of the owner
// = one line of "Waiting for approval" in the Members page, with the buttons "Accept" and "Refuse"
// We never send the real model (Roomie) because it contains the password hash
// An owner has only one roomshare, so we don't need to send its name: all the requests are for the same roomshare
public class PendingJoinResponse {

    // The id of the roomie: the front sends it back in the URL when the owner clicks on "Accept" or "Refuse"
    private int roomieId;

    private String firstName;

    private String lastName;

    // Shown under the name, like in the mockup
    private String email;

    // Getters and setters: Jackson uses the getters to turn the object into JSON
    public int getRoomieId() {
        return roomieId;
    }

    public void setRoomieId(int roomieId) {
        this.roomieId = roomieId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Used by the API to build one line of the list
    public PendingJoinResponse(int roomieId, String firstName, String lastName, String email) {
        this.roomieId = roomieId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }
}