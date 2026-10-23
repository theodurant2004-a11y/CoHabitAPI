package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.ShoppingListDAO;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "shopping_list")
public class ShoppingList {

    // ------------------------- Attributes and JPA Annotations --------------------------------------------------

    // @Id specifies a primary key
    @Id
    // @GeneratedValue specifies the auto-increment constraint
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // @Column specifies which column of the table is involved
    @Column(name = "shopping_list_id")
    private Integer id;

    // Foreign key
    @OneToOne // Specifies the relation like "One ShoppingList has One RoomShare"
    @JoinColumn(name = "roomshare_id")
    // Return the entire object, not only the id
    private RoomShare roomShare;


    // ------------------------- Getters and Setters --------------------------------------------------

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public RoomShare getRoomShare() {
        return roomShare;
    }

    public void setRoomShare(RoomShare roomShare) {
        Objects.requireNonNull(roomShare);
        this.roomShare = roomShare;
    }


    // ------------------------- Constructors --------------------------------------------------

    public ShoppingList() {
    }


    // ------------------------- DAO methods --------------------------------------------------

    public void create(ShoppingListDAO dao) { dao.create(this); }
}
