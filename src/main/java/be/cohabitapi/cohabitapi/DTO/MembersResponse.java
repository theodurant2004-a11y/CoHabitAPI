package be.cohabitapi.cohabitapi.DTO;

// One member of the roomshare (the owner or an accepted roomie) = one line of the Members page
// We never send the real model because it contains the password hash
// Example: { "id": 2, "firstName": "Bob", "lastName": "Martin", "email": "bob@mail.be", "role": "roomie" }
public class MembersResponse {

    private int id;

    private String firstName;

    private String lastName;

    // Shown under the name, like in the mockup. The front also uses it to find "Me" in the list
    private String email;

    // "owner" or "roomie": the front shows the badge "Owner" with it
    private String role;

    // Getters and setters: Jackson uses the getters to turn the object into JSON
    // (getId/setId, getFirstName/setFirstName, getLastName/setLastName, getEmail/setEmail, getRole/setRole
    //  écrits sur plusieurs lignes comme dans les autres DTO)

    // Used by the API to build one line of the list
    public MembersResponse(int id, String firstName, String lastName, String email, String role) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
    }
}