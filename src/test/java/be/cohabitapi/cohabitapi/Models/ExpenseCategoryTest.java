package be.cohabitapi.cohabitapi.Models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// These tests check the enum ExpenseCategory: the code sent by the front and the converter used by JPA
class ExpenseCategoryTest {

    private final ExpenseCategory.DbConverter converter = new ExpenseCategory.DbConverter();


    // ------------------------- fromCode : what the front sends --------------------------------------------------

    @Test
    void fromCode_knownCode_givesTheCategory() {
        assertEquals(ExpenseCategory.FOOD, ExpenseCategory.fromCode("food"));
        assertEquals(ExpenseCategory.RENT, ExpenseCategory.fromCode("rent"));
    }

    @Test
    void fromCode_spacesAndUpperCase_areAccepted() {
        assertEquals(ExpenseCategory.BILLS, ExpenseCategory.fromCode("  Bills "));
    }

    @Test
    void fromCode_unknownOrEmptyCode_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> ExpenseCategory.fromCode("unknown"));
        assertThrows(IllegalArgumentException.class, () -> ExpenseCategory.fromCode("   "));
        assertThrows(IllegalArgumentException.class, () -> ExpenseCategory.fromCode(null));
    }

    @Test
    void everyCategory_isFoundWithItsOwnCode() {
        for (ExpenseCategory category : ExpenseCategory.values()) {
            assertEquals(category, ExpenseCategory.fromCode(category.getCode()));
        }
    }


    // ------------------------- DbConverter : what JPA saves and reads --------------------------------------------------

    @Test
    void converter_categoryBecomesItsCodeInTheDb() {
        assertEquals("food", converter.convertToDatabaseColumn(ExpenseCategory.FOOD));
    }

    @Test
    void converter_codeOfTheDbBecomesTheCategory() {
        assertEquals(ExpenseCategory.LEISURE, converter.convertToEntityAttribute("leisure"));
    }

    @Test
    void converter_nullStaysNullInBothDirections() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void converter_unknownTypeInTheDb_givesOtherInsteadOfFailing() {
        // An old row with a type that is not in the enum must not crash the page
        assertEquals(ExpenseCategory.OTHER, converter.convertToEntityAttribute("something old"));
    }
}