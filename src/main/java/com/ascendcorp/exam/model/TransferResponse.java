package com.ascendcorp.exam.model;

import lombok.Data;

@Data
public class TransferResponse {
    private String responseCode;
    private String description;
    private String referenceCode1;
    private String referenceCode2;
    private String amount;
    private String bankTransactionID;

    public String getBalance() {
        return amount;
    }
}
