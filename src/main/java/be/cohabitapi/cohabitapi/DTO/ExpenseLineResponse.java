package be.cohabitapi.cohabitapi.DTO;

// One debt: the debtor owes the amount to the creditor
public class ExpenseLineResponse {

    private Integer id;
    private String description;
    private Double amount;
    private String date;  // "2026-10-20"
    private Integer debtorId;
    private String debtor;
    private Integer creditorId;
    private String creditor;
    private String icon;
    private boolean settled;

    public ExpenseLineResponse() {
    }

    public ExpenseLineResponse(Integer id, String description, Double amount, String date, Integer debtorId, String debtor, Integer creditorId, String creditor, String icon, boolean settled) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.debtorId = debtorId;
        this.debtor = debtor;
        this.creditorId = creditorId;
        this.creditor = creditor;
        this.icon = icon;
        this.settled = settled;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Integer getDebtorId() {
        return debtorId;
    }

    public void setDebtorId(Integer debtorId) {
        this.debtorId = debtorId;
    }

    public String getDebtor() {
        return debtor;
    }

    public void setDebtor(String debtor) {
        this.debtor = debtor;
    }

    public Integer getCreditorId() {
        return creditorId;
    }

    public void setCreditorId(Integer creditorId) {
        this.creditorId = creditorId;
    }

    public String getCreditor() {
        return creditor;
    }

    public void setCreditor(String creditor) {
        this.creditor = creditor;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public boolean isSettled() {
        return settled;
    }

    public void setSettled(boolean settled) {
        this.settled = settled;
    }
}
