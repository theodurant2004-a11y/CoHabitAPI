package be.cohabitapi.cohabitapi.DTO;

import java.util.List;

// This class is used to answer the page Expenses of a roomie
public record ExpensesResponse(
        Integer currentMemberId,            // the id of the roomie who is signed in
        List<MemberResponse> members,       // the list of the roomies of the roomshare
        List<CategoryResponse> categories,  // the list of categories for the form
        SummaryResponse summary,            // the two cards: what I owe / what I am owed
        List<ExpenseResponse> expenses,     // the  list of debts that concern me
        List<BalanceResponse> balances,     // the list of  balance of each roomie
        List<RefundResponse> refunds) {     // the list of  suggested refunds

    public record MemberResponse(Integer id, String name, String color) {}

    public record CategoryResponse(String code, String label, String icon) {}

    public record SummaryResponse(Double toPay, Double toReceive, boolean hasToPay, boolean hasToReceive) {}

    // One line = one debt: debtor owes amount to creditor
    // status = "pending" or "settled"
    public record ExpenseResponse(Integer id, String description, Double amount, String date,
                                  String debtor, String creditor, String icon, String status) {}

    // percent = width of the bar,the biggest balance has 100
    public record BalanceResponse(String name, String color, Double amount, Integer percent, String sign) {}

    public record RefundResponse(String from, String to, Double amount) {}
}