package be.cohabitapi.cohabitapi.Models;

import java.util.Objects;

// A refund suggested to the roomies: it is calculated, never saved in the db
public class Refund {

    private Roomie from;
    private Roomie to;
    private double amount;

    public Refund(Roomie from, Roomie to, double amount) {
        setFrom(from);
        setTo(to);
        setAmount(amount);
    }

    public Roomie getFrom() {
        return from;
    }

    private void setFrom(Roomie from) {
        if (from == null)
            throw new IllegalArgumentException("The roomie who pays is required.");
        this.from = from;
    }

    public Roomie getTo() {
        return to;
    }

    private void setTo(Roomie to) {
        if (to == null)
            throw new IllegalArgumentException("The roomie who receives is required.");
        this.to = to;
    }

    public double getAmount() {
        return amount;
    }

    private void setAmount(double amount) {
        if (!(amount > 0))
            throw new IllegalArgumentException("The amount must be greater than 0.");
        this.amount = amount;
    }


    // ------------------------- Overloads -------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Refund refund = (Refund) o;

        return Double.compare(refund.amount, amount) == 0 &&
                Objects.equals(from, refund.from) &&
                Objects.equals(to, refund.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, amount);
    }
}
