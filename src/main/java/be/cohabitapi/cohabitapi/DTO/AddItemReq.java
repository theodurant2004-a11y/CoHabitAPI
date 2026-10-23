package be.cohabitapi.cohabitapi.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AddItemReq {

    private String name;

    private String category;

    private String quantity;

    // To find the roomshare's shopping list to add the item
    private Integer roomshareId;

    // We don't need the current user except to check if it's a roomie or no
    private String roomieEmail;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public Integer getRoomshareId() {
        return roomshareId;
    }

    public void setRoomshareId(Integer roomshareId) {
        this.roomshareId = roomshareId;
    }

    public String getRoomieEmail() {
        return roomieEmail;
    }

    public void setRoomieEmail(String roomieEmail) {
        this.roomieEmail = roomieEmail;
    }

    public AddItemReq() {}
}
