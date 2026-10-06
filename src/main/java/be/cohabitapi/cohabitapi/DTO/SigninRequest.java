package be.cohabitapi.cohabitapi.DTO;

public class SigninRequest {

    private String email;
    private String password;

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

    public SigninRequest(){}

    public SigninRequest(String _email, String _password){
        this.email = _email;
        this.password = _password;
    }
}
