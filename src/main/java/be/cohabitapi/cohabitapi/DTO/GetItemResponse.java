package be.cohabitapi.cohabitapi.DTO;

public class GetItemResponse {
    private Integer productId;
    private String productName;
    private String productCategory;
    private String quantity;
    private Boolean isFound;

    public Integer getProductId() { return productId; }

    public void setProductId(Integer productId) { this.productId = productId; }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(String productCategory) {
        this.productCategory = productCategory;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public Boolean getFound() {
        return isFound;
    }

    public void setFound(Boolean found) {
        isFound = found;
    }

    public GetItemResponse() {}

    public GetItemResponse(Integer productId, String productName, String productCategory, String quantity, boolean isFound) {
        this.productId = productId;
        this.productName = productName;
        this.productCategory = productCategory;
        this.quantity = quantity;
        this.isFound = isFound;
    }
}
