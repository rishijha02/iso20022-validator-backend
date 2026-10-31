package com.isovalidator.iso.service;

import com.isovalidator.iso.DTO.IsoResponseDTO;
import com.isovalidator.iso.DTO.MessageGenerationRequest;
import com.isovalidator.iso.DTO.MessageGenerationResponse;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Generates bundled CAMT and PAIN messages from their selected XSD. */
@Component
public class CamtPainGenerator implements MessageGenerator {
    private final IsoValidateService validator;
    private final Pacs008SchemaCatalog catalog;
    private final XsdMessageBuilder builder;

    public CamtPainGenerator(IsoValidateService validator, Pacs008SchemaCatalog catalog, XsdMessageBuilder builder) {
        this.validator = validator;
        this.catalog = catalog;
        this.builder = builder;
    }

    @Override public String getMessageType() { return "generic-camt-pain"; }

    @Override
    public MessageGenerationResponse generate(MessageGenerationRequest request, String geography) {
        MessageGenerationResponse response = new MessageGenerationResponse();
        String messageType = request == null ? null : request.getMessageType();
        response.setMessageType(messageType);
        response.setGeography(geography);
        try {
            if (request == null || messageType == null || messageType.isBlank()) {
                throw new IllegalArgumentException("A CAMT or PAIN message type is required");
            }
            String family = messageType.substring(0, messageType.indexOf('.')).toLowerCase(Locale.ROOT);
            if (!family.equals("camt") && !family.equals("pain")) throw new IllegalArgumentException("Only CAMT and PAIN messages are handled by this generator");
            if (geography != null && !geography.equalsIgnoreCase("ISO") && !geography.equalsIgnoreCase("ISO20022")) {
                throw new IllegalArgumentException("Bundled " + family.toUpperCase(Locale.ROOT) + " generation currently supports ISO 20022 profile only.");
            }
            String version = catalog.resolveVersion(messageType, "ISO20022", request.getVersion());
            response.setVersion(version);
            String schemaPath = catalog.schemaResourcePath(messageType, "ISO20022", version);
            Map<String, String> values = new HashMap<>();
            if (request.getFields() != null) values.putAll(request.getFields());
            String xml;
            try (InputStream schema = getClass().getResourceAsStream(schemaPath)) {
                if (schema == null) throw new IllegalStateException("XSD schema not found: " + schemaPath);
                xml = builder.build(schema, values);
            }
            IsoResponseDTO validation = validator.validateMsg(xml, "ISO20022");
            if (!validation.isValid()) {
                String details = validation.getErrors() == null ? validation.getMessage()
                        : validation.getErrors().stream().map(error -> {
                            String technical = error.getTechnicalMessage();
                            return technical == null || technical.isBlank() ? error.getMessage() : error.getMessage() + ": " + technical;
                        }).filter(message -> message != null && !message.isBlank()).collect(Collectors.joining("; "));
                throw new IllegalStateException("Generated XML did not pass ISO20022 XSD validation" + (details == null || details.isBlank() ? "." : ": " + details));
            }
            response.setSuccess(true);
            response.setXml(xml);
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError(e.getMessage());
        }
        return response;
    }
}
