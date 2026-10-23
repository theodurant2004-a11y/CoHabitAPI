package be.cohabitapi.cohabitapi.Models;

import java.io.Serializable;
import java.util.Objects;

public class ItemId implements Serializable {
    private Integer product;
    private Integer shoppingList;

    public ItemId() {}

    public ItemId(Integer product, Integer shoppingList) {
        this.product = product;
        this.shoppingList = shoppingList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemId itemId = (ItemId) o;
        return Objects.equals(product, itemId.product) &&
                Objects.equals(shoppingList, itemId.shoppingList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(product, shoppingList);
    }
}