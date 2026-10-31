package com.isovalidator.iso.service;

import com.isovalidator.iso.DTO.IsoResponseDTO;
import com.isovalidator.iso.DTO.MessageGenerationRequest;
import com.isovalidator.iso.DTO.MessageGenerationResponse;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class Pacs009Generator implements MessageGenerator {
    private final IsoValidateService validator;
    private final Pacs008SchemaCatalog catalog;
    private final XsdMessageBuilder builder;
    public Pacs009Generator(IsoValidateService validator, Pacs008SchemaCatalog catalog, XsdMessageBuilder builder) {
        this.validator = validator; this.catalog = catalog; this.builder = builder;
    }
    @Override public String getMessageType() { return "pacs.009"; }
    @Override public MessageGenerationResponse generate(MessageGenerationRequest request, String geography) {
        MessageGenerationResponse response = new MessageGenerationResponse(); response.setMessageType("pacs.009"); response.setGeography(geography);
        try {
            if (request == null) throw new IllegalArgumentException("Generation request cannot be null");
            String profile = geography == null || geography.equalsIgnoreCase("ISO") ? "ISO20022" : geography.trim().toUpperCase(Locale.ROOT);
            if (!profile.equals("ISO20022") && !profile.equals("CBPR_PLUS")) throw new IllegalArgumentException("Unsupported pacs.009 profile: " + geography);
            String version = catalog.resolveVersion("pacs.009",profile,request.getVersion()); response.setVersion(version);
            Map<String,String> inputs = request.getFields() == null ? Map.of() : request.getFields();
            String amount = value(inputs,"amount","1250.00"); String currency = value(inputs,"currency","EUR").trim().toUpperCase(Locale.ROOT);
            if (!currency.matches("[A-Z]{3}")) throw new IllegalArgumentException("Currency must be a three-letter currency code");
            String decimal = new BigDecimal(amount).stripTrailingZeros().toPlainString();
            String root = "/Document/FICdtTrf"; String header = root + "/GrpHdr"; String tx = root + "/CdtTrfTxInf";
            String debtorBic = value(inputs,"debtorBic","COBADEFFXXX").trim().toUpperCase(Locale.ROOT);
            String creditorBic = value(inputs,"creditorBic","AGRIFRPPXXX").trim().toUpperCase(Locale.ROOT);
            Map<String,String> values = new HashMap<>();
            values.put(header+"/MsgId","FI-"+UUID.randomUUID().toString().replace("-","").substring(0,28));
            values.put(header+"/CreDtTm",OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            values.put(header+"/NbOfTxs","1"); values.put(header+"/SttlmInf/SttlmMtd",profile.equals("CBPR_PLUS")?"INDA":"CLRG");
            values.put(tx+"/PmtId/InstrId","I"+UUID.randomUUID().toString().replace("-","").substring(0,15));
            values.put(tx+"/PmtId/EndToEndId",value(inputs,"endToEndId","E2E-FI-001"));
            values.put(tx+"/PmtId/UETR",UUID.randomUUID().toString());
            values.put(tx+"/IntrBkSttlmAmt",decimal); values.put(tx+"/IntrBkSttlmAmt/@Ccy",currency);
            values.put(tx+"/IntrBkSttlmDt",LocalDate.now(ZoneOffset.UTC).toString());
            values.put(tx+"/Dbtr/FinInstnId/BICFI",debtorBic); values.put(tx+"/Dbtr/FinInstnId/Nm",value(inputs,"debtorName","Debtor Bank"));
            values.put(tx+"/DbtrAgt/FinInstnId/BICFI",debtorBic);
            values.put(tx+"/CdtrAgt/FinInstnId/BICFI",creditorBic);
            values.put(tx+"/Cdtr/FinInstnId/BICFI",creditorBic); values.put(tx+"/Cdtr/FinInstnId/Nm",value(inputs,"creditorName","Creditor Bank"));
            if (profile.equals("CBPR_PLUS")) {
                values.put(tx+"/InstgAgt/FinInstnId/BICFI",debtorBic);
                values.put(tx+"/InstdAgt/FinInstnId/BICFI",creditorBic);
            }
            String path=catalog.schemaResourcePath("pacs.009",profile,version); String xml;
            try (InputStream xsd=getClass().getResourceAsStream(path)) {
                if (xsd==null) throw new IllegalStateException("XSD schema not found: "+path);
                xml=builder.build(xsd,values);
            }
            IsoResponseDTO valid=validator.validateMsg(xml,profile);
            if (!valid.isValid()) throw new IllegalStateException("Generated XML did not pass "+profile+" XSD validation: "+valid.getMessage());
            response.setSuccess(true); response.setXml(xml);
        } catch(Exception e) { response.setSuccess(false); response.setError(e.getMessage()); }
        return response;
    }
    private String value(Map<String,String> values,String key,String fallback) { String v=values.get(key); return v==null||v.isBlank()?fallback:v; }
}
