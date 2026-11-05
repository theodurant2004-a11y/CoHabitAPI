package be.cohabitapi.cohabitapi.DTO;

// The balance of a roomie of the roomshare
public class BalanceResponse {

    private String name;
    private String color;
    private Double amount;
    private Integer percent;  // width of the bar, the biggest balance has 100

    public BalanceResponse() {
    }

    public BalanceResponse(String name, String color, Double amount, Integer percent) {
        this.name = name;
        this.color = color;
        this.amount = amount;
        this.percent = percent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Integer getPercent() {
        return percent;
    }

    public void setPercent(Integer percent) {
        this.percent = percent;
    }
}
