package be.cohabitapi.cohabitapi.DTO;

import java.util.List;

// The answer of GET /api/expenses/owner
public class OwnerExpensesResponse {

    private List<MemberResponse> members;  // the accepted roomies, for the "People concerned" of the forms
    private List<ChargeResponse> charges;  // the rents and the bills

    public OwnerExpensesResponse() {
    }

    public OwnerExpensesResponse(List<MemberResponse> members, List<ChargeResponse> charges) {
        this.members = members;
        this.charges = charges;
    }

    public List<MemberResponse> getMembers() {
        return members;
    }

    public void setMembers(List<MemberResponse> members) {
        this.members = members;
    }

    public List<ChargeResponse> getCharges() {
        return charges;
    }

    public void setCharges(List<ChargeResponse> charges) {
        this.charges = charges;
    }
}
