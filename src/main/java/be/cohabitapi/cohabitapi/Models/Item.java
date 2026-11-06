package be.cohabitapi.cohabitapi.Models;

import static be.cohabitapi.cohabitapi.Utils.VerificationOperation.normalizeStringAttribute;

import be.cohabitapi.cohabitapi.DAO.ItemDAO;
import jakarta.persistence.*;
import org.glassfish.jersey.uri.PathPattern;

import java.util.List;
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
    @JoinColumn(name = "roomshare_id", nullable = false)
    private RoomShare roomShare;

    @Column(name = "item_quantity", length = 50)
    private String quantity;

    @Column(name = "item_is_found", nullable = false)
    // Converts a boolean (true/false) in Java to a numeric boolean (1/0) in SQL Oracle
    @Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
    // It's a boolean and not a Boolean because I don't want to have a possible null value
    private boolean status = false;


    // ------------------------- Getters and Setters --------------------------------------------------

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        Objects.requireNonNull(product);
        this.product = product;
    }

    public RoomShare getRoomShare() {
        return roomShare;
    }

    public void setRoomShare(RoomShare roomShare) {
        Objects.requireNonNull(roomShare);
        this.roomShare = roomShare;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = normalizeStringAttribute(quantity, "quantity", 50);
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean found) { this.status = found; }


    // ------------------------- Constructors --------------------------------------------------

    protected Item() {}

    public Item(Product product, RoomShare roomShare, String quantity) {
        setProduct(product);
        setRoomShare(roomShare);
        setQuantity(quantity);
    }


    // ------------------------- Utility methods --------------------------------------------------



    // ------------------------- DAO methods --------------------------------------------------

    public void create(ItemDAO dao) { dao.create(this); }

    public static Item findById(ItemDAO dao, int productId, int roomShareId) { return dao.findById(productId, roomShareId); }

    public static List<Item> findAll(ItemDAO dao, int roomShareId) { return dao.findAll(roomShareId); }

    public void update(ItemDAO dao) { dao.update(this); }

    public void delete(ItemDAO dao) { dao.delete(this); }

    public static void deleteCheckedItems(ItemDAO dao, int roomShareId) { dao.deleteCheckedItems(roomShareId); }

}
