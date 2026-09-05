package com.isovalidator.iso.DTO;

import lombok.Data;

@Data
public class MessageGenerationRequest {

    private String messageType;

    // Debtor
    private String debtorName;
    private String debtorIban;
    private String debtorBic;

    // Creditor
    private String creditorName;
    private String creditorIban;
    private String creditorBic;

    // Payment
    private String amount;
    private String currency;

    // Optional
    private String endToEndId;

}
