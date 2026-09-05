package com.isovalidator.iso.service;

import com.isovalidator.iso.DTO.MessageGenerationRequest;
import com.isovalidator.iso.DTO.MessageGenerationResponse;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class Pacs008Generator implements MessageGenerator {

    private static final String VERSION = "001.14";

    @Override
    public String getMessageType() {
        return "pacs.008";
    }

    @Override
    public MessageGenerationResponse generate(
            MessageGenerationRequest request,
            String geography) {

        MessageGenerationResponse response =
                new MessageGenerationResponse();

        response.setMessageType("pacs.008");
        response.setGeography(geography);
        response.setVersion(VERSION);

        try {

            validateRequest(request);

            String msgId =
                    "MSG-" + System.currentTimeMillis();

            String uetr =
                    UUID.randomUUID().toString();

            String creationDateTime =
                    OffsetDateTime.now(ZoneOffset.UTC)
                            .format(
                                    DateTimeFormatter.ISO_OFFSET_DATE_TIME
                            );

            String endToEndId =
                    request.getEndToEndId();

            if (endToEndId == null ||
                    endToEndId.isBlank()) {

                endToEndId = "NOTPROVIDED";
            }

            String xml = """
                    <?xml version="1.0" encoding="UTF-8"?>

                    <Document xmlns="%s">

                        <FIToFICstmrCdtTrf>

                            <GrpHdr>

                                <MsgId>%s</MsgId>

                                <CreDtTm>%s</CreDtTm>

                                <NbOfTxs>1</NbOfTxs>

                            </GrpHdr>

                            <CdtTrfTxInf>

                                <PmtId>

                                    <InstrId>%s</InstrId>

                                    <EndToEndId>%s</EndToEndId>

                                    <UETR>%s</UETR>

                                </PmtId>

                                <IntrBkSttlmAmt Ccy="%s">%s</IntrBkSttlmAmt>

                                <Dbtr>

                                    <Nm>%s</Nm>

                                </Dbtr>

                                <DbtrAcct>

                                    <Id>

                                        <IBAN>%s</IBAN>

                                    </Id>

                                </DbtrAcct>

                                <DbtrAgt>

                                    <FinInstnId>

                                        <BICFI>%s</BICFI>

                                    </FinInstnId>

                                </DbtrAgt>

                                <CdtrAgt>

                                    <FinInstnId>

                                        <BICFI>%s</BICFI>

                                    </FinInstnId>

                                </CdtrAgt>

                                <Cdtr>

                                    <Nm>%s</Nm>

                                </Cdtr>

                                <CdtrAcct>

                                    <Id>

                                        <IBAN>%s</IBAN>

                                    </Id>

                                </CdtrAcct>

                            </CdtTrfTxInf>

                        </FIToFICstmrCdtTrf>

                    </Document>
                    """.formatted(

                    getNamespace(geography),

                    msgId,

                    creationDateTime,

                    msgId,

                    escapeXml(endToEndId),

                    uetr,

                    request.getCurrency(),

                    request.getAmount(),

                    escapeXml(request.getDebtorName()),

                    escapeXml(request.getDebtorIban()),

                    escapeXml(request.getDebtorBic()),

                    escapeXml(request.getCreditorBic()),

                    escapeXml(request.getCreditorName()),

                    escapeXml(request.getCreditorIban())
            );

            response.setSuccess(true);
            response.setXml(xml);

        } catch (Exception e) {

            response.setSuccess(false);
            response.setError(e.getMessage());
        }

        return response;
    }


    private void validateRequest(
            MessageGenerationRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Request cannot be null"
            );
        }

        if (isBlank(request.getDebtorName())) {
            throw new IllegalArgumentException(
                    "Debtor name is required"
            );
        }

        if (isBlank(request.getDebtorIban())) {
            throw new IllegalArgumentException(
                    "Debtor IBAN is required"
            );
        }

        if (isBlank(request.getDebtorBic())) {
            throw new IllegalArgumentException(
                    "Debtor BIC is required"
            );
        }

        if (isBlank(request.getCreditorName())) {
            throw new IllegalArgumentException(
                    "Creditor name is required"
            );
        }

        if (isBlank(request.getCreditorIban())) {
            throw new IllegalArgumentException(
                    "Creditor IBAN is required"
            );
        }

        if (isBlank(request.getCreditorBic())) {
            throw new IllegalArgumentException(
                    "Creditor BIC is required"
            );
        }

        if (isBlank(request.getAmount())) {
            throw new IllegalArgumentException(
                    "Amount is required"
            );
        }

        if (isBlank(request.getCurrency())) {
            throw new IllegalArgumentException(
                    "Currency is required"
            );
        }
    }


    private String getNamespace(String geography) {

        return "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.14";
    }


    private boolean isBlank(String value) {

        return value == null ||
                value.trim().isEmpty();
    }


    private String escapeXml(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}