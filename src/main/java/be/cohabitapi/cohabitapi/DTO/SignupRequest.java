package be.cohabitapi.cohabitapi.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

//For take the exeption when jackson doesn't know the role of person
@JsonIgnoreProperties(ignoreUnknown = true)
public class SignupRequest {
    private String lastname;
    private String firstname;
    private String email;
    private String password;
    private String confirmPassword;
    private String role;

    public SignupRequest() {
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastName(String lastname) {
        this.lastname = lastname;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
