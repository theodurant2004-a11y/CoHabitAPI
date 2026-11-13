package be.cohabitapi.cohabitapi.DTO;

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

    public TaskRoomieResponse(Integer id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}