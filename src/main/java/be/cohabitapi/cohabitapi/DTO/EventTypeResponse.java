package be.cohabitapi.cohabitapi.DTO;

// One type of event for the buttons of the pop-up "New event"
public class EventTypeResponse {

    // What the front sends back when the user chooses this type: "social"
    private String code;

    // The text shown on the button: "Social"
    private String label;

    // Empty constructor for Jackson
    public EventTypeResponse() {
    }

    public EventTypeResponse(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}