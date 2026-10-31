package be.cohabitapi.cohabitapi.DTO;

import be.cohabitapi.cohabitapi.Models.Roomie;

// A roomie assigned to a task, as the front sees it
// We never send the real model because it contains the password hash
public class TaskRoomieResponse {

    private Integer id;
    // Same names as in UserResponse, so the front uses the same names everywhere
    private String firstName;
    private String lastName;

    // Empty constructor: Jackson needs it
    public TaskRoomieResponse() {
    }

    // Builds the answer from the model
    public TaskRoomieResponse(Roomie roomie) {
        this.id = roomie.getIdPerson();
        this.firstName = roomie.getFirstName();
        this.lastName = roomie.getLastName();
    }

    // Only getters: Jackson only needs them to turn the object into JSON
    public Integer getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}