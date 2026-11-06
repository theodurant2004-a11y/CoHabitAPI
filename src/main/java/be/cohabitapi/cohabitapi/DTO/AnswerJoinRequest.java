package be.cohabitapi.cohabitapi.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// A DTO is the shape of the JSON sent by the front
// Here: the JSON sent when the owner clicks on "Accept" or "Refuse"
// Example: { "ownerEmail": "alice@mail.be", "accept": true }
// ignoreUnknown = true: if the front sends an extra field it is ignored instead of causing an error
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnswerJoinRequest {

    // TO DO : remove it when the session system is efficient
    // For now the API has no cookie to know who is calling, so the front sends the email of the owner
    private String ownerEmail;

    // true = accept the roomie, false = refuse him
    // Boolean (object) and not boolean (primitive):
    // if the front forgets the field we get null and the model answers 400,
    // with boolean we would get false and the roomie would be refused by mistake
    private Boolean accept;

    // Getters and setters: Jackson uses the setters to fill the object with the JSON
    // So the names must be the same as in the JSON
    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public Boolean getAccept() {
        return accept;
    }

    public void setAccept(Boolean accept) {
        this.accept = accept;
    }

    // Jackson needs an empty constructor to create the object
    public AnswerJoinRequest() {
    }

    // Used by the tests to build a request in one line
    public AnswerJoinRequest(String ownerEmail, Boolean accept) {
        this.ownerEmail = ownerEmail;
        this.accept = accept;
    }
}