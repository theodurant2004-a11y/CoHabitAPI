package be.cohabitapi.cohabitapi.DTO;

import be.cohabitapi.cohabitapi.Models.RoomShare;

public class CreateRoomShareResponse {
    private int id;
    private String invitationKey;
    private String name;
    private String image;
    private String creationDate;

    // Now, we give a complete Owner (without the pwd)
    // To the client, but when the needs of him will be
    // Clearer in my mind, I could refine this
    private UserResponse owner;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getInvitationKey() {
        return invitationKey;
    }

    public void setInvitationKey(String invitationKey) {
        this.invitationKey = invitationKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public UserResponse getOwner() {
        return owner;
    }

    public void setOwner(UserResponse owner) {
        this.owner = owner;
    }

    public CreateRoomShareResponse() {
    }

    // Call the complete object of the model
    // in the DTO constructor
    public CreateRoomShareResponse(RoomShare model) {
        this.id = model.getId();
        this.invitationKey = model.getInvitationKey();
        this.name = model.getName();
        this.image = model.getImage();
        // In this version of Java, Jackson doesn't know the LocalDate type
        // So we have to parse it in a string
        this.creationDate = model.getCreationDate().toString();
        this.owner = new UserResponse(model.getOwner());
    }

}
