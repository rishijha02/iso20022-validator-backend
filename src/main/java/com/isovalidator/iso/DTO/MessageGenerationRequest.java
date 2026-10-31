package com.isovalidator.iso.DTO;

import lombok.Data;
import java.util.Map;

@Data
public class MessageGenerationRequest {

    private String messageType;

    // Requested ISO 20022 message version, e.g. 001.14
    private String version;

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

    // Additional schema-driven fields, used by generators with message-specific inputs.
    private Map<String, String> fields;

}
