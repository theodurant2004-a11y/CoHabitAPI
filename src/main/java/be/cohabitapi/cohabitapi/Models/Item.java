package be.cohabitapi.cohabitapi.Models;

import static be.cohabitapi.cohabitapi.Utils.VerificationOperation.normalizeStringAttribute;

import be.cohabitapi.cohabitapi.DAO.ShoppingListDAO;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "item")
@IdClass(ItemId.class)
public class Item {

    // ------------------------- Attributes and JPA Annotations --------------------------------------------------

    // Take the foreign key as a part of the primary key
    @Id
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Take the foreign key as a part of the primary key
    @Id
    @ManyToOne
    @JoinColumn(name = "shopping_list_id", nullable = false)
    private ShoppingList shoppingList;

    @Column(name = "item_quantity", length = 50)
    private String quantity;

    @Column(name = "item_is_found", nullable = false)
    // It's a boolean and not a Boolean because I don't want to have a possible null value
    private boolean isFound = false;


    // ------------------------- Getters and Setters --------------------------------------------------

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        Objects.requireNonNull(product);
        this.product = product;
    }

    public ShoppingList getShoppingList() {
        return shoppingList;
    }

    public void setShoppingList(ShoppingList shoppingList) {
        Objects.requireNonNull(shoppingList);
        this.shoppingList = shoppingList;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = normalizeStringAttribute(quantity, "quantity", 50);
    }

    public boolean getFound() {
        return isFound;
    }


    // ------------------------- Constructors --------------------------------------------------

    protected Item() {}

    public Item(Product product, ShoppingList shoppingList, String quantity) {
        setProduct(product);
        setShoppingList(shoppingList);
        setQuantity(quantity);
    }

    // ------------------------- Utility methods --------------------------------------------------

    // To mark a product as found in the shopping list
    public void checkAnItem() {
        isFound = false;
    }


    // ------------------------- DAO methods --------------------------------------------------

    public void create(ShoppingListDAO dao) { dao.create(this); }
}
