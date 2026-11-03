package com.isovalidator.iso.service;

import com.isovalidator.iso.DTO.IsoResponseDTO;
import com.isovalidator.iso.DTO.MessageGenerationRequest;
import com.isovalidator.iso.DTO.MessageGenerationResponse;
//import com.isovalidator.iso.DTO.ValidationError;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.io.InputStream;

@Component
public class Pacs008Generator implements MessageGenerator {

    private final IsoValidateService validator;
    private final Pacs008SchemaCatalog schemaCatalog;
    private final XsdMessageBuilder xsdMessageBuilder;

    public Pacs008Generator(IsoValidateService validator, Pacs008SchemaCatalog schemaCatalog,
                            XsdMessageBuilder xsdMessageBuilder) {
        this.validator = validator;
        this.schemaCatalog = schemaCatalog;
        this.xsdMessageBuilder = xsdMessageBuilder;
    }

    @Override
    public String getMessageType() {
        return "pacs.008";
    }

    @Override
    public MessageGenerationResponse generate(
            MessageGenerationRequest request,
            String geography) {

        MessageGenerationResponse response = new MessageGenerationResponse();
        response.setMessageType("pacs.008");
        response.setGeography(geography);

        try {
            validateRequest(request);
            GenerationProfile profile = resolveProfile(geography, request.getVersion());
            response.setVersion(profile.version());

            String xml = buildFromSchema(request, profile);

            IsoResponseDTO validation = validator.validateMsg(xml, profile.validationProfile());
            if (!validation.isValid()) {
                String details = validation.getErrors() == null
                        ? validation.getMessage()
                        : validation.getErrors().stream()
                                .map(error -> {
                                    String friendly = error.getMessage();
                                    String technical = error.getTechnicalMessage();
                                    String location = error.getLine() > 0 ? " (line " + error.getLine() + ")" : "";
                                    if (technical == null || technical.isBlank()) return friendly + location;
                                    return friendly + location + ": " + technical;
                                })
                                .filter(message -> message != null && !message.isBlank())
                                .collect(Collectors.joining("; "));
                throw new IllegalStateException(
                        "Generated XML did not pass " + profile.validationProfile()
                                + " XSD validation" + (isBlank(details) ? "." : ": " + details)
                );
            }

            response.setSuccess(true);
            response.setXml(xml);
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError(e.getMessage());
        }

        return response;
    }

    private String buildFromSchema(MessageGenerationRequest request, GenerationProfile profile) throws Exception {
        String base = "/Document/FIToFICstmrCdtTrf";
        String tx = base + "/CdtTrfTxInf";
        String currency = request.getCurrency().trim().toUpperCase(Locale.ROOT);
        String amount = normalizeAmount(request.getAmount());
        String endToEndId = isBlank(request.getEndToEndId()) ? "NOTPROVIDED" : request.getEndToEndId().trim();
        String createdAt = OffsetDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx"));
        String settlementDate = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE);
        Map<String, String> values = new HashMap<>();
        values.put(base + "/GrpHdr/MsgId", "MSG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 31));
        values.put(base + "/GrpHdr/CreDtTm", createdAt);
        values.put(base + "/GrpHdr/NbOfTxs", "1");
        values.put(base + "/GrpHdr/SttlmInf/SttlmMtd", profile.settlementMethod());
        values.put(tx + "/PmtId/InstrId", "I" + UUID.randomUUID().toString().replace("-", "").substring(0, 15));
        values.put(tx + "/PmtId/EndToEndId", endToEndId);
        values.put(tx + "/PmtId/UETR", UUID.randomUUID().toString());
        values.put(tx + "/IntrBkSttlmAmt", amount);
        values.put(tx + "/IntrBkSttlmAmt/@Ccy", currency);
        values.put(tx + "/IntrBkSttlmDt", settlementDate);
        values.put(tx + "/InstdAmt", amount);
        values.put(tx + "/InstdAmt/@Ccy", currency);
        values.put(tx + "/ChrgBr", profile.chargeBearer());
        values.put(tx + "/Dbtr/Nm", request.getDebtorName().trim());
        values.put(tx + "/DbtrAcct/Id/IBAN", request.getDebtorIban().trim().toUpperCase(Locale.ROOT));
        values.put(tx + "/DbtrAgt/FinInstnId/BICFI", request.getDebtorBic().trim().toUpperCase(Locale.ROOT));
        values.put(tx + "/CdtrAgt/FinInstnId/BICFI", request.getCreditorBic().trim().toUpperCase(Locale.ROOT));
        values.put(tx + "/Cdtr/Nm", request.getCreditorName().trim());
        values.put(tx + "/CdtrAcct/Id/IBAN", request.getCreditorIban().trim().toUpperCase(Locale.ROOT));
        if ("CBPR_PLUS".equals(profile.validationProfile())) {
            values.put(tx + "/InstgAgt/FinInstnId/BICFI", request.getDebtorBic().trim().toUpperCase(Locale.ROOT));
            values.put(tx + "/InstdAgt/FinInstnId/BICFI", request.getCreditorBic().trim().toUpperCase(Locale.ROOT));
        }

        try (InputStream xsd = getClass().getResourceAsStream(profile.schemaPath())) {
            if (xsd == null) throw new IllegalStateException("XSD schema not found: " + profile.schemaPath());
            return xsdMessageBuilder.build(xsd, values);
        }
    }

    private void validateRequest(MessageGenerationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        require(request.getDebtorName(), "Debtor name");
        require(request.getDebtorIban(), "Debtor IBAN");
        require(request.getDebtorBic(), "Debtor BIC");
        require(request.getCreditorName(), "Creditor name");
        require(request.getCreditorIban(), "Creditor IBAN");
        require(request.getCreditorBic(), "Creditor BIC");
        require(request.getAmount(), "Amount");
        require(request.getCurrency(), "Currency");

        try {
            if (new BigDecimal(request.getAmount().trim()).signum() < 0) {
                throw new IllegalArgumentException("Amount cannot be negative");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount must be a valid decimal number");
        }

        if (!request.getCurrency().trim().matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException("Currency must be a three-letter currency code");
        }
    }

    private GenerationProfile resolveProfile(String geography, String requestedVersion) {
        String normalizedGeography = geography == null ? "ISO" : geography.trim().toUpperCase(Locale.ROOT);
        String validationProfile;
        String version;
        String settlementMethod;
        String chargeBearer;

        switch (normalizedGeography) {
            case "ISO", "ISO20022" -> {
                validationProfile = "ISO20022";
                settlementMethod = "CLRG";
                chargeBearer = "SLEV";
            }
            case "CBPR_PLUS" -> {
                validationProfile = "CBPR_PLUS";
                settlementMethod = "INDA";
                chargeBearer = "SHAR";
            }
            case "SEPA" -> throw new IllegalArgumentException(
                    "SEPA generation is unavailable: this backend archive does not include a SEPA pacs.008 XSD."
            );
            default -> throw new IllegalArgumentException("Unsupported generation profile: " + geography);
        }

        version = schemaCatalog.resolveVersion(normalizedGeography, requestedVersion);

        String schemaPath = schemaCatalog.schemaResourcePath(normalizedGeography, version);
        return new GenerationProfile(validationProfile, version, settlementMethod, chargeBearer, schemaPath);
    }

    private String normalizeAmount(String amount) {
        return new BigDecimal(amount.trim()).stripTrailingZeros().toPlainString();
    }

    private void require(String value, String label) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(label + " is required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record GenerationProfile(
            String validationProfile,
            String version,
            String settlementMethod,
            String chargeBearer,
            String schemaPath) {
    }
}
