package be.cohabitapi.cohabitapi.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateRoomShareRequest {
    private String name;

    // The same attributes name as the JSON request to make the match

    // TODO : Replace by a complete DTO when the session system will be efficient
    private String ownerEmail;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String email) {
        this.ownerEmail = email;
    }

    public CreateRoomShareRequest() {
    }

    public CreateRoomShareRequest(String name, String email) {
        this.name = name;
        this.ownerEmail = email;
    }
}
