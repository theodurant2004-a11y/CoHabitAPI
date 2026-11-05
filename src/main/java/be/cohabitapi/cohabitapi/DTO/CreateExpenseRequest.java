package be.cohabitapi.cohabitapi.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// The JSON sent by the front when a roomie adds an expense
// ex: {"description":"Groceries","amount":45,"date":"2026-10-26","paidById":321,
//           "category":"food","sharedWithIds":[321,322,323],"roomieEmail":"..."}
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateExpenseRequest {

    private String description;
    private Double amount;
    private String date;
    private Integer paidById;             // the id of the roomie who paid
    private String category;              // the code of the category (food, rent, ..)
    private List<Integer> sharedWithIds;  // the list of ids of the roomies who share the expense

    // TODO : remove it when the session system is efficient
    private String roomieEmail;

    // Empty constructor because Jackson needs it to build the object from the JSON
    public CreateExpenseRequest() {
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Integer getPaidById() { return paidById; }
    public void setPaidById(Integer paidById) { this.paidById = paidById; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<Integer> getSharedWithIds() { return sharedWithIds; }
    public void setSharedWithIds(List<Integer> sharedWithIds) { this.sharedWithIds = sharedWithIds; }

    public String getRoomieEmail() { return roomieEmail; }
    public void setRoomieEmail(String roomieEmail) { this.roomieEmail = roomieEmail; }
}