package be.cohabitapi.cohabitapi.DTO;

public class UserResponse {

    private Integer id_person;
    private String firstName;
    private String lastName;
    private String email;
    private String role;

    public Integer getId_person() {
        return id_person;
    }

    public void setId_person(Integer id_person) {
        this.id_person = id_person;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public UserResponse(){}

    public UserResponse(Integer _id_person, String _firstName, String _lastName, String _email, String _role){
        this.id_person = _id_person;
        this.firstName = _firstName;
        this.lastName = _lastName;
        this.email = _email;
        this.role = _role;
    }
}
