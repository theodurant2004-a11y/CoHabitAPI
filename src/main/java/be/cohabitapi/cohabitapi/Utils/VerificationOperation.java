package be.cohabitapi.cohabitapi.Utils;

public class VerificationOperation {
    // Trim any string attribute and checks if they are null, empty or upper than the max length
    public static String normalizeStringAttribute(String attribute, String attributeName, int maxLength){
        if (attribute == null)
            throw new IllegalArgumentException("The " + attributeName + " cannot be null");

        String trimmed = attribute.trim();

        if (trimmed.isEmpty())
            throw new IllegalArgumentException("The " + attributeName + " cannot be empty");
        if (trimmed.length() > maxLength)
            throw new IllegalArgumentException("The " + attributeName + " cannot be longer than " + maxLength + " characters");

        return trimmed;
    }
}
