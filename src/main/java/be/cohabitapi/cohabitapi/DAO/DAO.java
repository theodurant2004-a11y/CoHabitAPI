package be.cohabitapi.cohabitapi.DAO;

import be.cohabitapi.cohabitapi.Models.Item;
import be.cohabitapi.cohabitapi.Models.Product;

public abstract class DAO <T> {

    public abstract void create(T object);
}
