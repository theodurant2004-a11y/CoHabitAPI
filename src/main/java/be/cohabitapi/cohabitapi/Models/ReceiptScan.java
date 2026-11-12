package be.cohabitapi.cohabitapi.Models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// What was read on a receipt: the items of the shopping list found on it and the total paid
// It is only a proposal for the roomie: it is not an entity, so nothing is saved in the db
public class ReceiptScan {

    // ------------------------- Attributes --------------------------------------------------

    // The ids of the products found on the receipt
    private List<Integer> itemIds = new ArrayList<>();

    // The total of the receipt, null when it could not be read
    private Double total;


    // ------------------------- Getters and Setters --------------------------------------------------

    // Read only list, same reason as RoomShare.getItems
    public List<Integer> getItemIds() {
        return Collections.unmodifiableList(itemIds);
    }

    public void setItemIds(List<Integer> itemIds) {
        // No list means that nothing was found
        if (itemIds == null)
            this.itemIds = new ArrayList<>();
        else
            this.itemIds = new ArrayList<>(itemIds);
    }

    public Double getTotal() {
        return total;
    }

    // A total that cannot be a real amount is forgotten: the roomie will type it himself
    public void setTotal(Double total) {
        if (total == null || total.isNaN() || total.isInfinite() || total < 0)
            this.total = null;
        else
            this.total = total;
    }


    // ------------------------- Constructors --------------------------------------------------

    public ReceiptScan(List<Integer> itemIds, Double total) {
        setItemIds(itemIds);
        setTotal(total);
    }
}
