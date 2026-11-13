package be.cohabitapi.cohabitapi.DTO;

import java.util.List;

public class ScanReceiptResponse {

    // The ids of the products of the list that were found on the receipt
    private List<Integer> itemIds;

    // The total read on the receipt (null if it could not be read)
    private Double total;

    public List<Integer> getItemIds() { return itemIds; }

    public void setItemIds(List<Integer> itemIds) { this.itemIds = itemIds; }

    public Double getTotal() { return total; }

    public void setTotal(Double total) { this.total = total; }

    public ScanReceiptResponse() {}

    public ScanReceiptResponse(List<Integer> itemIds, Double total) {
        this.itemIds = itemIds;
        this.total = total;
    }
}
