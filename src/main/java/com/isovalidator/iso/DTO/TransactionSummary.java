package com.isovalidator.iso.DTO;

import lombok.Data;

@Data
public class TransactionSummary {

    private String transactionId;

    private String uetr;

    private String amount;

    private String currency;

    private PartySummary debtor;

    private PartySummary creditor;

}
