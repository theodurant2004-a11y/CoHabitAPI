package be.cohabitapi.cohabitapi.DTO;

import java.time.LocalDate;

public class CreateRoomShareResponse {
    private Integer id;
    private String invitationKey;
    private String name;
    private String creationDate;
    private Integer ownerId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public Integer getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
    }

    public CreateRoomShareResponse() {
    }

    public CreateRoomShareResponse(Integer id, String invitationKey, String name, LocalDate creationDate, Integer ownerId) {
        this.id = id;
        this.invitationKey = invitationKey;
        this.name = name;
        // In this version of Java, Jackson doesn't know the LocalDate type
        // So we have to parse it in a string
        this.creationDate = creationDate.toString();
        this.ownerId = ownerId;
    }

}
