package be.cohabitapi.cohabitapi.Models; // same package as the class

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*; // to write assertEquals instead of Assertions.assertEquals

class MembershipStatusTest { // junit doesn't need the class to be public

    private final MembershipStatus.DbConverter converter = new MembershipStatus.DbConverter(); // it meams enum = number used by jpa

    // The code :

    @ParameterizedTest // ‘This test runs several times, that saves you from having to copy out the same test three times.’
    @CsvSource({"NONE, 1", "PENDING, 2", "ACCEPTED, 3"}) // Each string is a line of data, separated by commas. So this test runs three times
    // TGive it a name other than that of the method
    @DisplayName("getCode: each status -> its database code")
    void getCode_returnsDatabaseCode(MembershipStatus status, int expectedCode) {
        // assertEquals(expected, actual)
        // The code for NONE must be 1
        assertEquals(expectedCode, status.getCode());
    }

    // Here we give the number and we expect the enum
    @ParameterizedTest
    @CsvSource({"1, NONE", "2, PENDING", "3, ACCEPTED"})
    @DisplayName("fromCode: valid code -> matching status")
    void fromCode_validCode_returnsStatus(int code, MembershipStatus expected) {
        // Act + Assert
        // assertSame checks that it is exactly the same object (==)
        // An enum value exists only once, so assertSame is stricter and better than assertEquals here
        assertSame(expected, MembershipStatus.fromCode(code));
    }


    @ParameterizedTest
    // @ValueSource: one single value per run there is no columns, unlike @CsvSource

    @ValueSource(ints = {0, 4, -1, 99}) // 0 is not allowed by the constraint (1, 2, 3)
    @DisplayName("fromCode: unknown code -> IllegalArgumentException")
    void fromCode_unknownCode_throws(int code) {
        // assertThrows checks that the code throws the given exception
        // "() ->" is a lambda expression: it is the code we try to run
        // If nothing is thrown, or another exception is detected the test fails
        // assertThrows also returns the exception, so we can check the  message
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> MembershipStatus.fromCode(code));

        // Assert
        assertEquals("Unknown membership status code: " + code, e.getMessage());
    }

    //CONVERTER
    // These tests check what Hibernate calls when it writes or reads the roomie_status column

    @Test  // @Test: one method = one test, it runs once
    @DisplayName("converter: status -> database code")
    void converter_toDatabase() {
        // Pending must be 2
        assertEquals(2, converter.convertToDatabaseColumn(MembershipStatus.PENDING));
    }

    @Test
    @DisplayName("converter: database code -> status")
    void converter_toEntity() {
        // 3 from the db must become ACCEPTED
        assertSame(MembershipStatus.ACCEPTED, converter.convertToEntityAttribute(3));
    }

    @Test
    @DisplayName("converter: null stays null in both directions")
    void converter_nullIsNull() {
        // The column can be empty in the db so the converter must not crash on null
        // assertNull checks that the value is null
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }
}