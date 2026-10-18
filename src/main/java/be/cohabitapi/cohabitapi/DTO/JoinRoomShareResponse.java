package be.cohabitapi.cohabitapi.DTO;

// The answer sent to the front. We never send the real model because it contains the password hash
// The invitation key is not sent either
public class JoinRoomShareResponse {

    private String status;

    // The name of the roomshare
    private String roomshareName;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRoomshareName() {
        return roomshareName;
    }

    public void setRoomshareName(String roomshareName) {
        this.roomshareName = roomshareName;
    }

    // Empty constructor: Jackson needs it to turn the object into JSON
    public JoinRoomShareResponse() {
    }

    // Used by the API to build the answer in one line
    public JoinRoomShareResponse(String status, String roomshareName) {
        this.status = status;
        this.roomshareName = roomshareName;
    }
}