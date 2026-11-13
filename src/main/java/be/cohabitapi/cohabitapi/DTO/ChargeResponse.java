package be.cohabitapi.cohabitapi.DTO;

import java.util.List;

// One rent or one bill of the owner, with the part of each roomie
public class ChargeResponse {

    private String name;
    private String category;  // "rent" or "bills"
    private Double total;
    private String dueDate;   // "2026-11-28"
    private List<ExpenseLineResponse> parts;  // one line for each roomie who has to pay

    public ChargeResponse() {
    }

    public ChargeResponse(String name, String category, Double total, String dueDate, List<ExpenseLineResponse> parts) {
        this.name = name;
        this.category = category;
        this.total = total;
        this.dueDate = dueDate;
        this.parts = parts;
    }

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

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public List<ExpenseLineResponse> getParts() {
        return parts;
    }

    public void setParts(List<ExpenseLineResponse> parts) {
        this.parts = parts;
    }
}
