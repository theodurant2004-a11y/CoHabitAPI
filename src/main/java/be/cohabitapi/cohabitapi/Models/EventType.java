package be.cohabitapi.cohabitapi.Models;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// The types of event of the calendar
// "code" is the value stored in the db and sent in the JSON (ex: "social")
// "label" is the text shown to the user (ex: "Social")
public enum EventType {
    SOCIAL("social", "Social"),
    MEETING("meeting", "Meeting"),
    VISIT("visit", "Visit"),
    TASK("task", "Task"),
    OTHER("other", "Other");

    private final String code;
    private final String label;

    EventType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    // GET A TYPE FROM ITS CODE: "social" (or "SOCIAL") gives SOCIAL
    // Static because it creates the value of the enum from a text
    public static EventType fromCode(String code) {
        if (code == null || code.trim().isEmpty())
            throw new IllegalArgumentException("The event type is required.");
        String clean = code.trim();
        for (EventType type : values()) {
            if (type.code.equalsIgnoreCase(clean)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid event type.");
    }

    // Technical helper: stores an EventType as its code in the db and reads it back
    @Converter
    public static class DbConverter implements AttributeConverter<EventType, String> {
        @Override
        public String convertToDatabaseColumn(EventType type) { return type == null ? null : type.getCode(); }
        @Override
        public EventType convertToEntityAttribute(String code) { return code == null ? null : EventType.fromCode(code); }
    }
}