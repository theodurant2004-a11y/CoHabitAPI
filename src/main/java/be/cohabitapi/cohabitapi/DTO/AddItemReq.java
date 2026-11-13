package be.cohabitapi.cohabitapi.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AddItemReq {

    private String name;

    private String category;

    private String quantity;

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }

    public void setCategory(String category) { this.category = category; }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) { this.quantity = quantity; }

    public AddItemReq() {}
}
