package be.cohabitapi.cohabitapi.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AddPurchaseReq {
    private String description;
    private Double amount;
    private Integer paidBy;

    public String getDescription() { return description; }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Integer getPaidBy() { return paidBy; }

    public void setPaidBy(Integer paidBy) { this.paidBy = paidBy; }

    public AddPurchaseReq() {
    }
}
