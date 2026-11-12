package be.cohabitapi.cohabitapi.EXCEPTION;

public class ReceiptScanUnavailableException extends RuntimeException {

    //--------------- SCAN A RECEIPT -------------------
    // If the AI cannot be reached, refuses the request (quota exceeded) or gives an answer that cannot be read
    // The message stays general: the roomie only needs to know that he has to fill the form himself
    public ReceiptScanUnavailableException() {
        super("Receipt scan is unavailable, please fill it in manually.");
    }
}
