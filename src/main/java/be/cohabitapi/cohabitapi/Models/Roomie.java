package be.cohabitapi.cohabitapi.Models;

import jakarta.persistence.*;

@Entity
@Table(name = "Roomie")
@PrimaryKeyJoinColumn(name = "user_id", referencedColumnName = "user_id")
public class Roomie extends Person{

    @ManyToOne
    @JoinColumn(name = "roomshare_id")
    private RoomShare roomshare;

    public RoomShare getRoomshare() {
        return roomshare;
    }

    public void setRoomshare(RoomShare roomshare) {
        this.roomshare = roomshare;
    }

    public Roomie() {
    }

    public Roomie(String lastName, String firstName, String email, String password) {
        super(lastName, firstName, email, password);
    }

    // Same as the constructor above but with the possibility to join directly a room share
    public Roomie(String lastName, String firstName, String email, String passwordHash, RoomShare roomshare) {
        super(lastName, firstName, email, passwordHash);
        roomshare.addRoomie(this); // Add the roomie to the room share list but add the roomshare to the roomie too
    }
}
