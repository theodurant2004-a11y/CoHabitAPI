package be.cohabitapi.cohabitapi.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// The shape of the JSON sent by the front to add an event
// Everything is a String because Jackson doesn't know LocalDate / LocalTime so the model parses them
// ignoreUnknown = true: if the front sends an extra field it is ignored instead of causing an error
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateEventRequest {

    // Must have the same names as in the JSON
    private String title;
    private String description;

    // in YYYY-MM-DD
    private String date;

    // in HH:MM or an empty text if the user did not choose a time, we can let it empty
    private String startTime;
    private String endTime;

    // The code of the type
    private String type;

    private String roomieEmail;

    // Jackson needs an empty constructor to create the object
    public CreateEventRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoomieEmail() {
        return roomieEmail;
    }

    public void setRoomieEmail(String roomieEmail) {
        this.roomieEmail = roomieEmail;
    }
}