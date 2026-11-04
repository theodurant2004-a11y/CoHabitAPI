package be.cohabitapi.cohabitapi.DTO;

import be.cohabitapi.cohabitapi.Models.Roomie;
import be.cohabitapi.cohabitapi.Models.Task;

import java.util.ArrayList;
import java.util.List;

// The answer of the tasks list: the tasks of the roomshare + the roomies a task can be assigned to
// Both lists are needed by the tasks page (the list to display, and the checkboxes of the form)
public class TaskListResponse {
    // Empty by default: the front gets [] and never null
    private List<TaskResponse> tasks = new ArrayList<>();
    private List<TaskRoomieResponse> roomies = new ArrayList<>();

    // Empty constructor: Jackson needs it
    public TaskListResponse() {
    }

    // Builds the answer from the model objects
    public TaskListResponse(List<Task> taskList, List<Roomie> roomieList) {
        // Each Task becomes a TaskResponse (same format as after an add)
        for (Task task : taskList) {
            tasks.add(new TaskResponse(task));
        }

        // Each Roomie becomes a small TaskRoomieResponse
        for (Roomie roomie : roomieList) {
            roomies.add(new TaskRoomieResponse(roomie));
        }
    }

    // Only getters: Jackson only needs them to turn the object into JSON
    public List<TaskResponse> getTasks() {
        return tasks;
    }

    public List<TaskRoomieResponse> getRoomies() {
        return roomies;
    }
}
