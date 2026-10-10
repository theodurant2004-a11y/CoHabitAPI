package be.cohabitapi.cohabitapi.DTO;

public class CreateRoomShareRequest {
    private String name;

    // The same name as the JSON request to make the match
    private UserResponse owner;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserResponse getOwner() {
        return owner;
    }

    public void setOwner(UserResponse owner) {
        this.owner = owner;
    }

    public CreateRoomShareRequest() {
    }

    public CreateRoomShareRequest(String name, UserResponse ownerDTO) {
        this.name = name;
        this.owner = ownerDTO;
    }
}
