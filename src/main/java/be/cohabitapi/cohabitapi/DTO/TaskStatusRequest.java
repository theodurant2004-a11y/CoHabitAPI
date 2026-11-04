package be.cohabitapi.cohabitapi.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskStatusRequest {

    private String status;
    private String roomieEmail;

    public TaskStatusRequest() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRoomieEmail() {
        return roomieEmail;
    }

    public void setRoomieEmail(String roomieEmail) {
        this.roomieEmail = roomieEmail;
    }
}
