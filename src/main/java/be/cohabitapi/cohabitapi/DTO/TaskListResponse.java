package be.cohabitapi.cohabitapi.DTO;

import java.util.List;

// The answer of the tasks list: the tasks of the roomshare + the roomies a task can be assigned to
// Both lists are needed by the tasks page (the list to display, and the checkboxes of the form)
public class TaskListResponse {

    private List<TaskResponse> tasks;
    private List<TaskRoomieResponse> roomies;

    // Empty constructor: Jackson needs it
    public TaskListResponse() {
    }

    public TaskListResponse(List<TaskResponse> tasks, List<TaskRoomieResponse> roomies) {
        this.tasks = tasks;
        this.roomies = roomies;
    }

    public List<TaskResponse> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskResponse> tasks) {
        this.tasks = tasks;
    }

    public List<TaskRoomieResponse> getRoomies() {
        return roomies;
    }

    public void setRoomies(List<TaskRoomieResponse> roomies) {
        this.roomies = roomies;
    }
}