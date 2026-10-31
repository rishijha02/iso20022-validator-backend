package com.isovalidator.iso.service;

import com.isovalidator.iso.DTO.IsoResponseDTO;
import com.isovalidator.iso.DTO.MessageGenerationRequest;
import com.isovalidator.iso.DTO.MessageGenerationResponse;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class Pacs002Generator implements MessageGenerator {
    private final IsoValidateService validator;
    private final Pacs008SchemaCatalog catalog;
    private final XsdMessageBuilder builder;
    public Pacs002Generator(IsoValidateService validator, Pacs008SchemaCatalog catalog, XsdMessageBuilder builder) {
        this.validator = validator; this.catalog = catalog; this.builder = builder;
    }
    @Override public String getMessageType() { return "pacs.002"; }
    @Override public MessageGenerationResponse generate(MessageGenerationRequest request, String geography) {
        MessageGenerationResponse response = new MessageGenerationResponse(); response.setMessageType("pacs.002"); response.setGeography(geography);
        try {
            if (request == null) throw new IllegalArgumentException("Generation request cannot be null");
            String profile = geography == null || geography.equalsIgnoreCase("ISO") ? "ISO20022" : geography.trim().toUpperCase();
            if (!profile.equals("ISO20022")) throw new IllegalArgumentException("pacs.002 schemas are not bundled for " + geography + "; select ISO 20022.");
            String version = catalog.resolveVersion("pacs.002", profile, request.getVersion()); response.setVersion(version);
            Map<String,String> inputs = request.getFields() == null ? Map.of() : request.getFields();
            String root = "/Document/FIToFIPmtStsRpt";
            Map<String,String> values = new HashMap<>();
            values.put(root + "/GrpHdr/MsgId", "STS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 28));
            values.put(root + "/GrpHdr/CreDtTm", OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            values.put(root + "/OrgnlGrpInfAndSts/OrgnlMsgId", value(inputs,"originalMessageId","ORIGINAL-MSG-001"));
            values.put(root + "/OrgnlGrpInfAndSts/OrgnlMsgNmId", value(inputs,"originalMessageNameId","pacs.008.001.14"));
            values.put(root + "/OrgnlGrpInfAndSts/GrpSts", value(inputs,"groupStatus","ACCP"));
            String path = catalog.schemaResourcePath("pacs.002",profile,version); String xml;
            try (InputStream xsd = getClass().getResourceAsStream(path)) {
                if (xsd == null) throw new IllegalStateException("XSD schema not found: " + path);
                xml = builder.build(xsd,values);
            }
            IsoResponseDTO valid = validator.validateMsg(xml,profile);
            if (!valid.isValid()) throw new IllegalStateException("Generated XML did not pass " + profile + " XSD validation: " + valid.getMessage());
            response.setSuccess(true); response.setXml(xml);
        } catch (Exception e) { response.setSuccess(false); response.setError(e.getMessage()); }
        return response;
    }
    private String value(Map<String,String> values,String key,String fallback) { String v=values.get(key); return v==null||v.isBlank()?fallback:v; }
}
