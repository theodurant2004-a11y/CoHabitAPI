package be.cohabitapi.cohabitapi.Models;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// The state of a roomie, and i use an enum for it
public enum MembershipStatus {

    // Each value carries the number that the database stores in roomie_status
    NONE(1),     // no roomshare yet
    PENDING(2),  // the owner didn't answer yet
    ACCEPTED(3); // have a roommshare

    // The number stored in the database for this status, and it never change
    private final int code;

    // The constructor , called once for each value above
    MembershipStatus(int code) {
        this.code = code;
    }

    // The getter to go from the java to the base
    public int getCode() {
        return code;
    }

    // Used by the converter to read the status from the database and give the value of the enum
    public static MembershipStatus fromCode(int code) {
        // values() returns the three statuses so we look for the one with this number
        for (MembershipStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        // It should never happen because the db only accepts 1, 2 and 3
        throw new IllegalArgumentException("Unknown membership status code: " + code);
    }


    // Technical helper and not a model clas. This tells JPA how to store this enum in the roomie_status column (a number).
    // it lives inside the enum so that the model package
    // only contains the classes of the class diagram
    @Converter
    public static class DbConverter implements AttributeConverter<MembershipStatus, Integer> {

        // Java -> db: called by JPA when it saves a roomie (PENDING becomes 2)
        @Override
        public Integer convertToDatabaseColumn(MembershipStatus status) {
            // null stays null
            return status == null ? null : status.getCode();
        }

        // db -> Java: called by JPA when it loads a roomie (2 becomes PENDING)
        @Override
        public MembershipStatus convertToEntityAttribute(Integer code) {
            return code == null ? null : MembershipStatus.fromCode(code);
        }
    }
}