package com.isovalidator.iso.service;

import com.isovalidator.iso.DTO.IsoResponseDTO;
import com.isovalidator.iso.DTO.MessageGenerationRequest;
import com.isovalidator.iso.DTO.MessageGenerationResponse;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class Pacs004Generator implements MessageGenerator {
    private final IsoValidateService validator;
    private final Pacs008SchemaCatalog schemaCatalog;
    private final XsdMessageBuilder xsdMessageBuilder;

    public Pacs004Generator(IsoValidateService validator, Pacs008SchemaCatalog schemaCatalog,
                            XsdMessageBuilder xsdMessageBuilder) {
        this.validator = validator;
        this.schemaCatalog = schemaCatalog;
        this.xsdMessageBuilder = xsdMessageBuilder;
    }

    @Override public String getMessageType() { return "pacs.004"; }

    @Override
    public MessageGenerationResponse generate(MessageGenerationRequest request, String geography) {
        MessageGenerationResponse response = new MessageGenerationResponse();
        response.setMessageType("pacs.004");
        response.setGeography(geography);
        try {
            if (request == null) throw new IllegalArgumentException("Generation request cannot be null");
            String profile = geography == null ? "ISO20022" : geography.trim().toUpperCase(Locale.ROOT);
            if (profile.equals("ISO")) profile = "ISO20022";
            if (!profile.equals("ISO20022") && !profile.equals("CBPR_PLUS")) {
                throw new IllegalArgumentException("Unsupported pacs.004 generation profile: " + geography);
            }
            String version = schemaCatalog.resolveVersion("pacs.004", profile, request.getVersion());
            response.setVersion(version);
            Map<String, String> inputs = request.getFields() == null ? Map.of() : request.getFields();
            String currency = value(inputs, "currency", "EUR").trim().toUpperCase(Locale.ROOT);
            String amount = value(inputs, "returnAmount", "1250.00").trim();
            if (!currency.matches("[A-Z]{3}")) throw new IllegalArgumentException("Currency must be a three-letter currency code");
            if (new BigDecimal(amount).signum() <= 0) throw new IllegalArgumentException("Returned amount must be greater than zero");

            String root = "/Document/PmtRtr";
            String header = root + "/GrpHdr";
            String tx = root + "/TxInf";
            Map<String, String> values = new HashMap<>();
            values.put(header + "/MsgId", "RTR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 28));
            values.put(header + "/CreDtTm", OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            values.put(header + "/NbOfTxs", "1");
            values.put(header + "/SttlmInf/SttlmMtd", profile.equals("CBPR_PLUS") ? "INDA" : "CLRG");
            values.put(tx + "/RtrId", "RETURN-" + UUID.randomUUID().toString().substring(0, 12));
            values.put(tx + "/OrgnlGrpInf/OrgnlMsgId", value(inputs, "originalMessageId", "ORIGINAL-MSG-001"));
            values.put(tx + "/OrgnlGrpInf/OrgnlMsgNmId", value(inputs, "originalMessageNameId", "pacs.008.001.14"));
            values.put(tx + "/OrgnlEndToEndId", value(inputs, "originalEndToEndId", "E2E-ORIGINAL-001"));
            values.put(tx + "/RtrdIntrBkSttlmAmt", new BigDecimal(amount).stripTrailingZeros().toPlainString());
            values.put(tx + "/RtrdIntrBkSttlmAmt/@Ccy", currency);
            values.put(tx + "/IntrBkSttlmDt", java.time.LocalDate.now(ZoneOffset.UTC).toString());
            String reason = value(inputs, "returnReasonCode", "AC01").trim();
            if (!reason.isEmpty()) values.put(tx + "/RtrRsnInf/Rsn/Cd", reason);
            String reasonText = value(inputs, "returnReasonText", "").trim();
            if (!reasonText.isEmpty()) values.put(tx + "/RtrRsnInf/AddtlInf", reasonText);

            String schemaPath = schemaCatalog.schemaResourcePath("pacs.004", profile, version);
            String xml;
            try (InputStream xsd = getClass().getResourceAsStream(schemaPath)) {
                if (xsd == null) throw new IllegalStateException("XSD schema not found: " + schemaPath);
                xml = xsdMessageBuilder.build(xsd, values);
            }
            IsoResponseDTO validation = validator.validateMsg(xml, profile);
            if (!validation.isValid()) {
                String details = validation.getErrors() == null ? validation.getMessage()
                        : validation.getErrors().stream().map(error -> {
                            String technical = error.getTechnicalMessage();
                            return technical == null || technical.isBlank() ? error.getMessage() : error.getMessage() + ": " + technical;
                        }).filter(message -> message != null && !message.isBlank()).collect(Collectors.joining("; "));
                throw new IllegalStateException("Generated XML did not pass " + profile + " XSD validation: " + details);
            }
            response.setSuccess(true);
            response.setXml(xml);
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError(e.getMessage());
        }
        return response;
    }

    private String value(Map<String, String> values, String key, String fallback) {
        String value = values.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
