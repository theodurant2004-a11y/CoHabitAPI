package be.cohabitapi.cohabitapi.DTO;

import java.util.List;

// The task sent back to the front (after add, in the list, after update)
// We never send the real model: it contains objects with a password hash and lazy lists
public class TaskResponse {

    private Integer id;
    private String name;
    private String description;
    // A String because Jackson doesn't know LocalDate here (same as CreateRoomShareResponse)
    private String dueDate;
    // "pending" or "completed"
    private String status;
    // The assigned roomies, with their names so the front can display them
    private List<TaskRoomieResponse> roomies;

    // Empty constructor: Jackson needs it
    public TaskResponse() {
    }

    public TaskResponse(Integer id, String name, String description, String dueDate, String status, List<TaskRoomieResponse> roomies) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.dueDate = dueDate;
        this.status = status;
        this.roomies = roomies;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public List<TaskRoomieResponse> getRoomies() {
        return roomies;
    }

    public void setRoomies(List<TaskRoomieResponse> roomies) {
        this.roomies = roomies;
    }
}