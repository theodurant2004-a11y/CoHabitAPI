package be.cohabitapi.cohabitapi.DTO;

// TODO: Vérifier si c les seules infos à renvoyer et si les DTO doivent etre JavaBeans
public class AddItemResp {
    private Integer productId;
    private Integer shoppingListId;

    public AddItemResp() {
    }

    public AddItemResp(Integer productId, Integer shoppingListId) {
        this.productId = productId;
        this.shoppingListId = shoppingListId;
    }
}
