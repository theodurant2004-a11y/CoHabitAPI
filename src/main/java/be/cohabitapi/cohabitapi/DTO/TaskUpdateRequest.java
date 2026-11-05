package be.cohabitapi.cohabitapi.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// The shape of the JSON sent by the front to update a task (PUT /api/tasks/{id})
// PUT = everything is replaced, so the front sends the whole task
// The id is not here: it is in the URL
// ignoreUnknown = true: if the front sends an extra field it is ignored instead of causing an error
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskUpdateRequest {
    private String name;
    private String description;


    private String dueDate;
    private String status;

    private List<Integer> roomieIds;

    // TO DO : remove it when the session system is efficient
    private String roomieEmail;

    public TaskUpdateRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Integer> getRoomieIds() {
        return roomieIds;
    }

    public void setRoomieIds(List<Integer> roomieIds) {
        this.roomieIds = roomieIds;
    }

    public String getRoomieEmail() {
        return roomieEmail;
    }

    public void setRoomieEmail(String roomieEmail) {
        this.roomieEmail = roomieEmail;
    }
}
