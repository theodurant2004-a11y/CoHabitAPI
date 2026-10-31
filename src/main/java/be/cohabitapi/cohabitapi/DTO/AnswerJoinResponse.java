package be.cohabitapi.cohabitapi.DTO;

// The answer sent to the front after the owner accepted or refused a request
// We never send the real model (Roomie) because it contains the password hash
// Example: { "roomieId": 2, "firstName": "Bob", "lastName": "Durand", "status": "accepted" }
public class AnswerJoinResponse {

    // The id of the roomie, so the front knows which line of the list to update
    private int roomieId;

    private String firstName;

    private String lastName;

    // The new status of the roomie: "accepted" or "none" (refused)
    private String status;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Used by the API to build the answer in one line
    public AnswerJoinResponse(int roomieId, String firstName, String lastName, String status) {
        this.roomieId = roomieId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = status;
    }
}