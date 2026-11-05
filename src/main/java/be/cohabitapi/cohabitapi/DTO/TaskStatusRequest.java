package be.cohabitapi.cohabitapi.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// The shape of the JSON sent by the front to check or uncheck a task (PUT /api/tasks/{id}/status)
// Only the status changes, so only the status is sent. The id is in the URL
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskStatusRequest {

    // "completed" to check the task, "pending" to uncheck it
    private String status;
    // TO DO : remove it when the session system is efficient
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
