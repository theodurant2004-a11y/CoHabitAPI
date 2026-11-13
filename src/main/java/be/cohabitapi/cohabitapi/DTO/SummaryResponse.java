package be.cohabitapi.cohabitapi.DTO;

// The amounts of the roomie who is signed in
public class SummaryResponse {

    private double toPay;
    private double toReceive;
    private double balance;

    public SummaryResponse() {
    }

    public SummaryResponse(double toPay, double toReceive, double balance) {
        this.toPay = toPay;
        this.toReceive = toReceive;
        this.balance = balance;
    }

    public double getToPay() {
        return toPay;
    }

    public void setToPay(double toPay) {
        this.toPay = toPay;
    }

    public double getToReceive() {
        return toReceive;
    }

    public void setToReceive(double toReceive) {
        this.toReceive = toReceive;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
