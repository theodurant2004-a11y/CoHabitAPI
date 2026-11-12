package be.cohabitapi.cohabitapi.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScanReceiptReq {

    // The photo of the receipt: a JPEG encoded in base64
    private String image;

    public String getImage() { return image; }

    public void setImage(String image) { this.image = image; }

    public ScanReceiptReq() {
    }
}
