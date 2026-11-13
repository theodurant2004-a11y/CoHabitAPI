package be.cohabitapi.cohabitapi.Models;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

// The state of a task (enumeration TaskStatus in the class diagram)
// I use an enum for it, same idea as MembershipStatus
public enum TaskStatus {

    // Each value carries the number that the database stores in task_status
    PENDING(1),    // the task is not done yet
    COMPLETED(2);  // the task is done

    // The number stored in the database for this status, it never changes
    private final int code;

    // The constructor, called once for each value above
    TaskStatus(int code) {
        this.code = code;
    }

    // The getter to go from the java to the base
    public int getCode() {
        return code;
    }

    // Used by the converter to read the status from the database
    public static TaskStatus fromCode(int code) {
        for (TaskStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        // It should never happen because the db only accepts 1 and 2
        throw new IllegalArgumentException("Unknown task status code: " + code);
    }

    // Used to read the status sent by the front ("pending" or "completed")
    // I don't use valueOf() because its error message is not clear for the front
    public static TaskStatus fromLabel(String label) {
        if (label == null || label.trim().isEmpty())
            throw new IllegalArgumentException("The status is required.");

        // Locale.ROOT makes the result the same on every computer
        String clean = label.trim().toUpperCase(Locale.ROOT);

        for (TaskStatus status : values()) {
            if (status.name().equals(clean)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid status.");
    }

    // Used to answer the front: PENDING becomes "pending"
    public String toLabel() {
        return name().toLowerCase(Locale.ROOT);
    }

    // Technical helper: tells JPA how to store this enum in the task_status column (a number)
    @Converter
    public static class DbConverter implements AttributeConverter<TaskStatus, Integer> {

        // Java -> db: PENDING becomes 1
        @Override
        public Integer convertToDatabaseColumn(TaskStatus status) {
            return status == null ? null : status.getCode();
        }

        // db -> Java: 1 becomes PENDING
        @Override
        public TaskStatus convertToEntityAttribute(Integer code) {
            return code == null ? null : TaskStatus.fromCode(code);
        }
    }
}
