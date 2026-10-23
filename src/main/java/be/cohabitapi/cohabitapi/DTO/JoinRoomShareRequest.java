package be.cohabitapi.cohabitapi.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// A DTO is the shape of the JSON sent by the front
// ignoreUnknown = true: if the front sends an extra field it is ignored instead of causing an error
@JsonIgnoreProperties(ignoreUnknown = true)
public class JoinRoomShareRequest {

    // Must have the same name as in the JSON
    private String invitationKey;

    // TO DO : remove it when the session system is efficient
    // For now the API has no cookie to know who is calling, so the front sends his email
    private String roomieEmail;

    public String getInvitationKey() {
        return invitationKey;
    }

    public void setInvitationKey(String invitationKey) {
        this.invitationKey = invitationKey;
    }

    public String getRoomieEmail() {
        return roomieEmail;
    }

    public void setRoomieEmail(String roomieEmail) {
        this.roomieEmail = roomieEmail;
    }

    // Jackson needs a empty constructor to  create the object
    public JoinRoomShareRequest() {
    }
}