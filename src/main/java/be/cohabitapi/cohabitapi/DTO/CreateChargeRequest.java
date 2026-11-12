package be.cohabitapi.cohabitapi.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// The JSON sent by the front when the owner publishes a rent or a bill
// ex: {"name":"Electricity","amount":90,"dueDate":"2026-11-28","category":"bills",
//           "recurrence":"once","roomieIds":[321,322],"ownerEmail":"..."}
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateChargeRequest {

    private String name;             // the title of a bill (a rent has no title, the model names it)
    private Double amount;           // the total, before it is split
    private String dueDate;          // "2026-11-28"
    private String category;         // the code of the category: rent or bills
    private String recurrence;       // once, monthly or yearly
    private List<Integer> roomieIds; // the ids of the roomies who have to pay

    // TODO : remove it when the session system is efficient
    private String ownerEmail;

    // Empty constructor because Jackson needs it to build the object from the JSON
    public CreateChargeRequest() {
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getRecurrence() { return recurrence; }
    public void setRecurrence(String recurrence) { this.recurrence = recurrence; }

    public List<Integer> getRoomieIds() { return roomieIds; }
    public void setRoomieIds(List<Integer> roomieIds) { this.roomieIds = roomieIds; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
}
