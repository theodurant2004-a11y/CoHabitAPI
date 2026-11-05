package be.cohabitapi.cohabitapi.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// The shape of the JSON sent by the front to add a task (POST) or to update it (PUT)
// Same fields for both: the model decides what is required (the status is required only for a PUT)
// ignoreUnknown = true: if the front sends an extra field it is ignored instead of causing an error
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskRequest {

    // Must have the same names as in the JSON
    private String name;
    private String description;

    // "YYYY-MM-DD": a String because Jackson doesn't know LocalDate here, the model parses it
    private String dueDate;

    // "pending" or "completed"
    private String status;

    // Ids of the roomies assigned to the task (the model loads the real objects)
    private List<Integer> roomieIds;

    // TO DO : remove it when the session system is efficient
    // For now the API has no cookie to know who is calling, so the front sends his email
    private String roomieEmail;

    // Jackson needs an empty constructor to create the object
    public TaskRequest() {
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