package com.ascendcorp.exam.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class InquiryServiceResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tranID;
    private String namespace;
    private String reasonCode;
    private String reasonDesc;
    private String balance;
    private String ref_no1;
    private String ref_no2;
    private String amount;
    private String accountName;

    @Override
    public String toString() {
        return "InquiryServiceResultDTO {" +
                "tranID='" + tranID + '\'' +
                ", namespace='" + namespace + '\'' +
                ", reasonCode='" + reasonCode + '\'' +
                ", reasonDesc='" + reasonDesc + '\'' +
                ", balance='" + balance + '\'' +
                ", ref_no1='" + ref_no1 + '\'' +
                ", ref_no2='" + ref_no2 + '\'' +
                ", amount='" + amount + '\'' +
                ", accountName='" + accountName + '\'' +
                '}';
    }
}
