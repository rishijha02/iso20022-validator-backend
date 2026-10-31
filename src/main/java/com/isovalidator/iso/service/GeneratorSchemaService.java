package com.isovalidator.iso.service;

import com.isovalidator.iso.DTO.GeneratorField;
import org.springframework.stereotype.Service;
import java.util.List;

/** Publishes input fields and bundled versions for supported XSD-driven generators. */
@Service
public class GeneratorSchemaService {
    private final Pacs008SchemaCatalog catalog;
    private final XsdMessageBuilder xsdMessageBuilder;
    public GeneratorSchemaService(Pacs008SchemaCatalog catalog, XsdMessageBuilder xsdMessageBuilder) {
        this.catalog = catalog; this.xsdMessageBuilder = xsdMessageBuilder;
    }

    public List<String> messageTypes(String family, String geography) { return catalog.messageTypes(family, geography); }
    public List<String> versions(String geography) { return versions("pacs.008", geography); }
    public List<String> versions(String messageType, String geography) { return catalog.versions(messageType, geography); }

    public List<GeneratorField> fields(String geography, String version) { return fields("pacs.008", geography, version); }
    public List<GeneratorField> fields(String messageType, String geography, String version) {
        String selectedVersion = catalog.resolveVersion(messageType, geography, version);
        String schemaPath = catalog.schemaResourcePath(messageType, geography, selectedVersion);
        try (var schema = getClass().getResourceAsStream(schemaPath)) {
            if (schema == null) throw new IllegalStateException("Bundled " + messageType + " XSD was not found: " + schemaPath);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Unable to read " + messageType + " XSD: " + schemaPath, e);
        }
        if (messageType != null && (messageType.startsWith("camt.") || messageType.startsWith("pain."))) {
            try (var schema = getClass().getResourceAsStream(schemaPath)) {
                if (schema == null) throw new IllegalStateException("Bundled XSD was not found: " + schemaPath);
                return xsdMessageBuilder.discoverRequiredFields(schema);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to inspect XSD: " + schemaPath, e);
            }
        }
        if ("pacs.002".equalsIgnoreCase(messageType)) {
            return List.of(
                new GeneratorField("originalMessageId", "Original message ID", "Original payment", true, "text", "Message being reported", "FIToFIPmtStsRpt/OrgnlGrpInfAndSts/OrgnlMsgId"),
                new GeneratorField("originalMessageNameId", "Original message type", "Original payment", true, "text", "pacs.008.001.14", "FIToFIPmtStsRpt/OrgnlGrpInfAndSts/OrgnlMsgNmId"),
                new GeneratorField("groupStatus", "Group status", "Status", true, "text", "ACCP", "FIToFIPmtStsRpt/OrgnlGrpInfAndSts/GrpSts")
            );
        }
        if ("pacs.009".equalsIgnoreCase(messageType)) {
            return List.of(
                new GeneratorField("debtorName", "Debtor financial institution", "Debtor agent", true, "text", "Debtor bank name", "FICdtTrf/CdtTrfTxInf/Dbtr/FinInstnId/Nm"),
                new GeneratorField("debtorBic", "Debtor BIC", "Debtor agent", true, "text", "COBADEFFXXX", "FICdtTrf/CdtTrfTxInf/DbtrAgt/FinInstnId/BICFI"),
                new GeneratorField("creditorName", "Creditor financial institution", "Creditor agent", true, "text", "Creditor bank name", "FICdtTrf/CdtTrfTxInf/Cdtr/FinInstnId/Nm"),
                new GeneratorField("creditorBic", "Creditor BIC", "Creditor agent", true, "text", "AGRIFRPPXXX", "FICdtTrf/CdtTrfTxInf/CdtrAgt/FinInstnId/BICFI"),
                new GeneratorField("amount", "Interbank settlement amount", "Payment", true, "number", "1250.00", "FICdtTrf/CdtTrfTxInf/IntrBkSttlmAmt"),
                new GeneratorField("currency", "Currency", "Payment", true, "currency", "EUR", "FICdtTrf/CdtTrfTxInf/IntrBkSttlmAmt/@Ccy")
            );
        }
        if ("pacs.004".equalsIgnoreCase(messageType)) {
            return List.of(
                new GeneratorField("returnAmount", "Returned interbank settlement amount", "Return payment", true, "number", "1250.00", "PmtRtr/TxInf/RtrdIntrBkSttlmAmt"),
                new GeneratorField("currency", "Currency", "Return payment", true, "currency", "EUR", "PmtRtr/TxInf/RtrdIntrBkSttlmAmt/@Ccy"),
                new GeneratorField("originalMessageId", "Original message ID", "Original payment", true, "text", "Original message reference", "PmtRtr/TxInf/OrgnlGrpInf/OrgnlMsgId"),
                new GeneratorField("originalMessageNameId", "Original message type", "Original payment", true, "text", "pacs.008.001.14", "PmtRtr/TxInf/OrgnlGrpInf/OrgnlMsgNmId"),
                new GeneratorField("originalEndToEndId", "Original end-to-end ID", "Original payment", true, "text", "Original transaction reference", "PmtRtr/TxInf/OrgnlEndToEndId"),
                new GeneratorField("returnReasonCode", "Return reason code", "Return reason", false, "text", "AC01", "PmtRtr/TxInf/RtrRsnInf/Rsn/Cd"),
                new GeneratorField("returnReasonText", "Additional return information", "Return reason", false, "text", "Optional", "PmtRtr/TxInf/RtrRsnInf/AddtlInf")
            );
        }
        return List.of(
            new GeneratorField("debtorName", "Debtor name", "Debtor", true, "text", "Name as it appears in the payment", "CdtTrfTxInf/Dbtr/Nm"),
            new GeneratorField("debtorIban", "Debtor IBAN", "Debtor", true, "text", "DE89370400440532013000", "CdtTrfTxInf/DbtrAcct/Id/IBAN"),
            new GeneratorField("debtorBic", "Debtor BIC", "Debtor", true, "text", "COBADEFFXXX", "CdtTrfTxInf/DbtrAgt/FinInstnId/BICFI"),
            new GeneratorField("creditorName", "Creditor name", "Creditor", true, "text", "Name as it appears in the payment", "CdtTrfTxInf/Cdtr/Nm"),
            new GeneratorField("creditorIban", "Creditor IBAN", "Creditor", true, "text", "FR7630006000011234567890189", "CdtTrfTxInf/CdtrAcct/Id/IBAN"),
            new GeneratorField("creditorBic", "Creditor BIC", "Creditor", true, "text", "AGRIFRPPXXX", "CdtTrfTxInf/CdtrAgt/FinInstnId/BICFI"),
            new GeneratorField("amount", "Interbank settlement amount", "Payment", true, "number", "1250.00", "CdtTrfTxInf/IntrBkSttlmAmt"),
            new GeneratorField("currency", "Currency", "Payment", true, "currency", "EUR", "CdtTrfTxInf/IntrBkSttlmAmt/@Ccy"),
            new GeneratorField("endToEndId", "End-to-end ID", "Payment", false, "text", "Optional", "CdtTrfTxInf/PmtId/EndToEndId")
        );
    }
}
