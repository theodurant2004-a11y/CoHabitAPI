package be.cohabitapi.cohabitapi.DTO;

import java.util.List;

// The answer of GET /api/expenses
public class ExpensesResponse {

    private Integer currentMemberId;  // the roomie who is signed in
    private List<MemberResponse> members;
    private SummaryResponse summary;
    private List<ExpenseLineResponse> expenses;  // the debts that concern the roomie
    private List<BalanceResponse> balances;
    private List<RefundResponse> refunds;

    public ExpensesResponse() {
    }

    public ExpensesResponse(Integer currentMemberId, List<MemberResponse> members, SummaryResponse summary, List<ExpenseLineResponse> expenses, List<BalanceResponse> balances, List<RefundResponse> refunds) {
        this.currentMemberId = currentMemberId;
        this.members = members;
        this.summary = summary;
        this.expenses = expenses;
        this.balances = balances;
        this.refunds = refunds;
    }

    public Integer getCurrentMemberId() {
        return currentMemberId;
    }

    public void setCurrentMemberId(Integer currentMemberId) {
        this.currentMemberId = currentMemberId;
    }

    public List<MemberResponse> getMembers() {
        return members;
    }

    public void setMembers(List<MemberResponse> members) {
        this.members = members;
    }

    public SummaryResponse getSummary() {
        return summary;
    }

    public void setSummary(SummaryResponse summary) {
        this.summary = summary;
    }

    public List<ExpenseLineResponse> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<ExpenseLineResponse> expenses) {
        this.expenses = expenses;
    }

    public List<BalanceResponse> getBalances() {
        return balances;
    }

    public void setBalances(List<BalanceResponse> balances) {
        this.balances = balances;
    }

    public List<RefundResponse> getRefunds() {
        return refunds;
    }

    public void setRefunds(List<RefundResponse> refunds) {
        this.refunds = refunds;
    }
}
