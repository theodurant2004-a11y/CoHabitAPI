package be.cohabitapi.cohabitapi.DTO;

import be.cohabitapi.cohabitapi.Models.Roomie;
import be.cohabitapi.cohabitapi.Models.Task;

import java.util.ArrayList;
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
    private List<TaskRoomieResponse> roomies = new ArrayList<>();

    // Empty constructor: Jackson needs it
    public TaskResponse() {
    }

    // Builds the answer from the model (reused by add, list and update)
    public TaskResponse(Task task) {
        this.id = task.getIdTask();
        this.name = task.getName();
        this.description = task.getDescription();
        // LocalDate -> "2026-10-27", or null if there is no due date
        this.dueDate = task.getDueDate() == null ? null : task.getDueDate().toString();
        // PENDING -> "pending"
        this.status = task.getStatusTask().toLabel();

        // Each Roomie object becomes a small TaskRoomieResponse
        for (Roomie roomie : task.getRoomies()) {
            roomies.add(new TaskRoomieResponse(roomie));
        }
    }

    // Only getters: Jackson only needs them to turn the object into JSON
    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDueDate() {
        return dueDate;
    }

    public String getStatus() {
        return status;
    }

    public List<TaskRoomieResponse> getRoomies() {
        return roomies;
    }
}