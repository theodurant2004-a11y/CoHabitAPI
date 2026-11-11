package be.cohabitapi.cohabitapi.DTO;

// The event sent back to the front
// The date and the times are Strings because Jackson doesn't know LocalDate / LocalTime here
public class EventResponse {

    private Integer id;
    private String title;
    private String description;

    // in the format 2026-11-05
    private String date;

    // in format 18:00 or null if the event has no time
    private String startTime;
    private String endTime;

    // The code of the type, the front chooses the icon and the color from it
    private String type;

    // Empty constructor because Jackson needs it
    public EventResponse() {
    }

    public EventResponse(Integer id, String title, String description, String date,
                         String startTime, String endTime, String type) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.type = type;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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
}