package com.isovalidator.iso.service;

import com.isovalidator.iso.DTO.MessageSummaryResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Service
@Slf4j
public class MessageSummaryServiceImpl
        implements MessageSummaryService {

    @Override
    public MessageSummaryResponse generateSummary(String xml) {

        MessageSummaryResponse response =
                new MessageSummaryResponse();

                System.out.println("XML is "+xml);

        try {

            if (xml == null || xml.isBlank()) {

                response.setSuccess(false);
                response.setErrorMessage(
                        "XML message cannot be empty"
                );

                return response;
            }


            // Step 1: Parse XML
            Document document = parseXml(xml);


            // Step 2: Get ISO 20022 namespace
            String namespace =
                    detectIsoNamespace(document);


            if (namespace == null) {

                response.setSuccess(false);
                response.setErrorMessage(
                        "Unable to detect ISO 20022 message namespace"
                );

                return response;
            }


            // Step 3: Extract message type and version
            String[] messageInfo =
                    extractMessageInfo(namespace);

            String messageType =
                    messageInfo[0];

            String version =
                    messageInfo[1];


            response.setMessageType(messageType);
            response.setVersion(version);


            // Step 4: Generate summary based on message type
            switch (messageType) {

                case "pacs.008":

                    generatePacs008Summary(
                            document,
                            response
                    );

                    break;

                    case "pacs.004":
                        generatePacs004Summary(
                                document,
                                response
                        );
                        break;

                case "pacs.009":
                        generatePacs009Summary(
                                document,
                                response
                        );
                        break;


                case "pacs.002":

                    generatePacs002Summary(
                            document,
                            response
                    );

                    break;


                case "pain.001":

                    generatePain001Summary(
                            document,
                            response
                    );

                    break;


                case "camt.053":

                    generateCamt053Summary(
                            document,
                            response
                    );

                    break;


                default:

                    response.setTitle(
                            "ISO 20022 Message"
                    );

                    response.getSummary().put(
                            "Message Type",
                            messageType
                    );

                    response.getSummary().put(
                            "Version",
                            version
                    );

                    response.getSummary().put(
                            "Status",
                            "Detailed summary is not yet supported for this message type"
                    );

                    break;
            }


            response.setSuccess(true);

            return response;

        } catch (Exception e) {

            response.setSuccess(false);

            response.setErrorMessage(
                    e.getMessage()
            );

            return response;
        }
    }



    private Document parseXml(String xml)
        throws Exception {

    DocumentBuilderFactory factory =
            DocumentBuilderFactory.newInstance();

    factory.setNamespaceAware(true);

    DocumentBuilder builder =
            factory.newDocumentBuilder();

    return builder.parse(
            new java.io.ByteArrayInputStream(
                    xml.getBytes(
                            java.nio.charset.StandardCharsets.UTF_8
                    )
            )
    );
}


private String detectIsoNamespace(Document document) {

    Element root =
            document.getDocumentElement();

    String rootNamespace =
            root.getNamespaceURI();


    if (isIsoBusinessNamespace(rootNamespace)) {

        return rootNamespace;
    }


    NodeList allElements =
            document.getElementsByTagName("*");


    for (int i = 0;
         i < allElements.getLength();
         i++) {

        Node node =
                allElements.item(i);

        String namespace =
                node.getNamespaceURI();


        if (isIsoBusinessNamespace(namespace)) {

            return namespace;
        }
    }


    return null;
}


private boolean isIsoBusinessNamespace(
        String namespace
) {

    if (namespace == null) {
        return false;
    }

    return namespace.contains(":pacs.")
            || namespace.contains(":pain.")
            || namespace.contains(":camt.");
}

private String[] extractMessageInfo(
        String namespace) {

    String[] parts =
            namespace.split(":");

    String lastPart =
            parts[parts.length - 1];

    String[] messageParts =
            lastPart.split("\\.");

    String messageType =
            messageParts[0]
                    + "."
                    + messageParts[1];

    String version =
            messageParts[2]
                    + "."
                    + messageParts[3];

    return new String[] {
            messageType,
            version
    };
}

private void generatePacs008Summary(
        Document document,
        MessageSummaryResponse response) {

    response.setTitle(
            "Financial Institution To Financial Institution Customer Credit Transfer"
    );


    // Group Header

    addIfPresent(
            response,
            "Message ID",
            getElementValue(
                    document,
                    "MsgId"
            )
    );


    addIfPresent(
            response,
            "Number of Transactions",
            getElementValue(
                    document,
                    "NbOfTxs"
            )
    );


    addIfPresent(
            response,
            "Creation Date",
            getElementValue(
                    document,
                    "CreDtTm"
            )
    );


    // Payment Identification

    addIfPresent(
            response,
            "End To End ID",
            getElementValue(
                    document,
                    "EndToEndId"
            )
    );


    addIfPresent(
            response,
            "Transaction ID",
            getElementValue(
                    document,
                    "TxId"
            )
    );


    addIfPresent(
            response,
            "UETR",
            getElementValue(
                    document,
                    "UETR"
            )
    );


    // Amount

    Element amountElement =
            getElement(
                    document,
                    "IntrBkSttlmAmt"
            );


    if (amountElement != null) {

        response.getSummary().put(
                "Amount",
                amountElement.getTextContent()
                        .trim()
        );

        response.getSummary().put(
                "Currency",
                amountElement.getAttribute(
                        "Ccy"
                )
        );
    }


    // Debtor

    Element debtor =
            getElement(
                    document,
                    "Dbtr"
            );


    if (debtor != null) {

        String debtorName =
                getChildElementValue(
                        debtor,
                        "Nm"
                );

        addIfPresent(
                response,
                "Debtor",
                debtorName
        );
    }


    // Creditor

    Element creditor =
            getElement(
                    document,
                    "Cdtr"
            );


    if (creditor != null) {

        String creditorName =
                getChildElementValue(
                        creditor,
                        "Nm"
                );

        addIfPresent(
                response,
                "Creditor",
                creditorName
        );
    }


    // Settlement Date

    addIfPresent(
            response,
            "Settlement Date",
            getElementValue(
                    document,
                    "IntrBkSttlmDt"
            )
    );


    // Charge Bearer

    addIfPresent(
            response,
            "Charge Bearer",
            getElementValue(
                    document,
                    "ChrgBr"
            )
    );
}

private Element getElement(
        Document document,
        String tagName) {

    NodeList nodes =
            document.getElementsByTagNameNS(
                    "*",
                    tagName
            );


    if (nodes.getLength() == 0) {

        return null;
    }


    return (Element) nodes.item(0);
}

private String getElementValue(
        Document document,
        String tagName) {

    Element element =
            getElement(
                    document,
                    tagName
            );


    if (element == null) {

        return null;
    }


    return element.getTextContent()
            .trim();
}


private String getChildElementValue(
        Element parent,
        String tagName) {

    NodeList nodes =
            parent.getElementsByTagNameNS(
                    "*",
                    tagName
            );


    if (nodes.getLength() == 0) {

        return null;
    }


    return nodes.item(0)
            .getTextContent()
            .trim();
}


private void addIfPresent(
        MessageSummaryResponse response,
        String key,
        String value
) {

    if (value != null
            && !value.isBlank()) {

        response.getSummary()
                .put(key, value);
    }
}

private void generatePacs002Summary(
        Document document,
        MessageSummaryResponse response) {

    response.setTitle(
            "Financial Institution To Financial Institution Payment Status Report"
    );

    // Group Header

    addIfPresent(
            response,
            "Message ID",
            getElementValue(document, "MsgId")
    );

    addIfPresent(
            response,
            "Creation Date",
            getElementValue(document, "CreDtTm")
    );


    // Original Group Information

    addIfPresent(
            response,
            "Original Message ID",
            getElementValue(document, "OrgnlMsgId")
    );

    addIfPresent(
            response,
            "Original Message Name",
            getElementValue(document, "OrgnlMsgNmId")
    );


    // Transaction Status

    addIfPresent(
            response,
            "Original Instruction ID",
            getElementValue(document, "OrgnlInstrId")
    );

    addIfPresent(
            response,
            "Original End To End ID",
            getElementValue(document, "OrgnlEndToEndId")
    );

    addIfPresent(
            response,
            "Original Transaction ID",
            getElementValue(document, "OrgnlTxId")
    );

    addIfPresent(
            response,
            "Transaction Status",
            getElementValue(document, "TxSts")
    );


    // Status Reason

    addIfPresent(
            response,
            "Status Reason",
            getElementValue(document, "Cd")
    );

    addIfPresent(
            response,
            "Additional Status Information",
            getElementValue(document, "AddtlInf")
    );
}

private void generatePain001Summary(
        Document document,
        MessageSummaryResponse response) {

    response.setTitle(
            "Customer Credit Transfer Initiation"
    );


    // Group Header

    addIfPresent(
            response,
            "Message ID",
            getElementValue(document, "MsgId")
    );

    addIfPresent(
            response,
            "Creation Date",
            getElementValue(document, "CreDtTm")
    );

    addIfPresent(
            response,
            "Number of Transactions",
            getElementValue(document, "NbOfTxs")
    );

    addIfPresent(
            response,
            "Control Sum",
            getElementValue(document, "CtrlSum")
    );


    // Payment Information

    addIfPresent(
            response,
            "Payment Information ID",
            getElementValue(document, "PmtInfId")
    );

    addIfPresent(
            response,
            "Payment Method",
            getElementValue(document, "PmtMtd")
    );

    addIfPresent(
            response,
            "Requested Execution Date",
            getElementValue(document, "ReqdExctnDt")
    );


    // Debtor

    Element debtor =
            getElement(document, "Dbtr");

    if (debtor != null) {

        addIfPresent(
                response,
                "Debtor",
                getChildElementValue(
                        debtor,
                        "Nm"
                )
        );
    }


    // Debtor Account

    addIfPresent(
            response,
            "Debtor IBAN",
            getElementValue(document, "IBAN")
    );


    // Creditor

    Element creditor =
            getElement(document, "Cdtr");

    if (creditor != null) {

        addIfPresent(
                response,
                "Creditor",
                getChildElementValue(
                        creditor,
                        "Nm"
                )
        );
    }


    // Payment Identification

    addIfPresent(
            response,
            "End To End ID",
            getElementValue(
                    document,
                    "EndToEndId"
            )
    );


    // Amount

    Element amountElement =
            getElement(
                    document,
                    "InstdAmt"
            );

    if (amountElement != null) {

        response.getSummary().put(
                "Amount",
                amountElement
                        .getTextContent()
                        .trim()
        );

        String currency =
                amountElement.getAttribute(
                        "Ccy"
                );

        if (currency != null
                && !currency.isBlank()) {

            response.getSummary().put(
                    "Currency",
                    currency
            );
        }
    }


    // Remittance Information

    addIfPresent(
            response,
            "Remittance Information",
            getElementValue(
                    document,
                    "Ustrd"
            )
    );
}

private void generateCamt053Summary(
        Document document,
        MessageSummaryResponse response) {

    response.setTitle(
            "Bank To Customer Statement"
    );


    // Group Header

    addIfPresent(
            response,
            "Message ID",
            getElementValue(
                    document,
                    "MsgId"
            )
    );

    addIfPresent(
            response,
            "Creation Date",
            getElementValue(
                    document,
                    "CreDtTm"
            )
    );


    // Statement Information

    addIfPresent(
            response,
            "Statement ID",
            getElementValue(
                    document,
                    "Id"
            )
    );

    addIfPresent(
            response,
            "Statement Creation Date",
            getElementValue(
                    document,
                    "CreDtTm"
            )
    );


    // Account

    Element account =
            getElement(
                    document,
                    "Acct"
            );

    if (account != null) {

        addIfPresent(
                response,
                "Account IBAN",
                getChildElementValue(
                        account,
                        "IBAN"
                )
        );

        addIfPresent(
                response,
                "Account Currency",
                getChildElementValue(
                        account,
                        "Ccy"
                )
        );

        Element owner =
                getChildElement(
                        account,
                        "Ownr"
                );

        if (owner != null) {

            addIfPresent(
                    response,
                    "Account Owner",
                    getChildElementValue(
                            owner,
                            "Nm"
                    )
            );
        }
    }


    // Statement Period

    addIfPresent(
            response,
            "Statement From Date",
            getElementValue(
                    document,
                    "FrDtTm"
            )
    );

    addIfPresent(
            response,
            "Statement To Date",
            getElementValue(
                    document,
                    "ToDtTm"
            )
    );


    // Opening Balance

    addIfPresent(
            response,
            "Opening Balance",
            getBalanceByType(
                    document,
                    "OPBD"
            )
    );


    // Closing Balance

    addIfPresent(
            response,
            "Closing Balance",
            getBalanceByType(
                    document,
                    "CLBD"
            )
    );


    // Transaction Information

    addIfPresent(
            response,
            "Number of Entries",
            String.valueOf(
                    document
                            .getElementsByTagNameNS(
                                    "*",
                                    "Ntry"
                            )
                            .getLength()
            )
    );


    // First Transaction Amount

    NodeList entries =
            document.getElementsByTagNameNS(
                    "*",
                    "Ntry"
            );

    if (entries.getLength() > 0) {

        Element firstEntry =
                (Element) entries.item(0);

        String amount =
                getChildElementValue(
                        firstEntry,
                        "Amt"
                );

        String creditDebit =
                getChildElementValue(
                        firstEntry,
                        "CdtDbtInd"
                );

        addIfPresent(
                response,
                "First Transaction Amount",
                amount
        );

        addIfPresent(
                response,
                "First Transaction Type",
                creditDebit
        );

        addIfPresent(
                response,
                "Booking Date",
                getChildElementValue(
                        firstEntry,
                        "Dt"
                )
        );
    }
}

private String getBalanceByType(
        Document document,
        String balanceType) {

    NodeList balances =
            document.getElementsByTagNameNS(
                    "*",
                    "Bal"
            );

    for (int i = 0;
         i < balances.getLength();
         i++) {

        Element balance =
                (Element) balances.item(i);

        String type =
                getChildElementValue(
                        balance,
                        "Cd"
                );

        if (balanceType.equals(type)) {

            return getChildElementValue(
                    balance,
                    "Amt"
            );
        }
    }

    return null;
}

private Element getChildElement(
        Element parent,
        String tagName) {

    NodeList nodes =
            parent.getElementsByTagNameNS(
                    "*",
                    tagName
            );

    if (nodes.getLength() == 0) {
        return null;
    }

    return (Element) nodes.item(0);
}

private void generatePacs004Summary(
        Document document,
        MessageSummaryResponse response) {

    response.setTitle(
            "Payment Return"
    );


    // =========================
    // GROUP HEADER
    // =========================

    addIfPresent(
            response,
            "Message ID",
            getElementValue(
                    document,
                    "MsgId"
            )
    );

    addIfPresent(
            response,
            "Creation Date",
            getElementValue(
                    document,
                    "CreDtTm"
            )
    );

    addIfPresent(
            response,
            "Number of Transactions",
            getElementValue(
                    document,
                    "NbOfTxs"
            )
    );


    // =========================
    // RETURN IDENTIFICATION
    // =========================

    addIfPresent(
            response,
            "Return ID",
            getElementValue(
                    document,
                    "RtrId"
            )
    );

    addIfPresent(
            response,
            "Original Message ID",
            getElementValue(
                    document,
                    "OrgnlMsgId"
            )
    );

    addIfPresent(
            response,
            "Original End To End ID",
            getElementValue(
                    document,
                    "OrgnlEndToEndId"
            )
    );

    addIfPresent(
            response,
            "Original Transaction ID",
            getElementValue(
                    document,
                    "OrgnlTxId"
            )
    );

    addIfPresent(
            response,
            "Original UETR",
            getElementValue(
                    document,
                    "OrgnlUETR"
            )
    );


    // =========================
    // RETURN AMOUNT
    // =========================

    Element amountElement =
            getElement(
                    document,
                    "RtrdIntrBkSttlmAmt"
            );

    if (amountElement != null) {

        response.getSummary().put(
                "Returned Amount",
                amountElement
                        .getTextContent()
                        .trim()
        );

        String currency =
                amountElement.getAttribute(
                        "Ccy"
                );

        if (currency != null
                && !currency.isBlank()) {

            response.getSummary().put(
                    "Currency",
                    currency
            );
        }
    }


    // =========================
    // RETURN REASON
    // =========================

    Element returnReason =
            getElement(
                    document,
                    "RtrRsnInf"
            );

    if (returnReason != null) {

        Element reason =
                getChildElement(
                        returnReason,
                        "Rsn"
                );

        if (reason != null) {

            addIfPresent(
                    response,
                    "Return Reason",
                    getChildElementValue(
                            reason,
                            "Cd"
                    )
            );

            addIfPresent(
                    response,
                    "Return Reason Details",
                    getChildElementValue(
                            reason,
                            "Prtry"
                    )
            );
        }

        addIfPresent(
                response,
                "Additional Information",
                getChildElementValue(
                        returnReason,
                        "AddtlInf"
                )
        );
    }


    // =========================
    // SETTLEMENT
    // =========================

    addIfPresent(
            response,
            "Settlement Date",
            getElementValue(
                    document,
                    "IntrBkSttlmDt"
            )
    );


    // =========================
    // RETURNING AGENT
    // =========================

    Element returningAgent =
            getElement(
                    document,
                    "RtrgAgt"
            );

    if (returningAgent != null) {

        addIfPresent(
                response,
                "Returning Agent BIC",
                getChildElementValue(
                        returningAgent,
                        "BICFI"
                )
        );
    }


    // =========================
    // ORIGINAL CREDITOR
    // =========================

    Element originalCreditor =
            getElement(
                    document,
                    "OrgnlCdtr"
            );

    if (originalCreditor != null) {

        addIfPresent(
                response,
                "Original Creditor",
                getChildElementValue(
                        originalCreditor,
                        "Nm"
                )
        );
    }


    // =========================
    // ORIGINAL DEBTOR
    // =========================

    Element originalDebtor =
            getElement(
                    document,
                    "OrgnlDbtr"
            );

    if (originalDebtor != null) {

        addIfPresent(
                response,
                "Original Debtor",
                getChildElementValue(
                        originalDebtor,
                        "Nm"
                )
        );
    }
}

private void generatePacs009Summary(
        Document document,
        MessageSummaryResponse response) {

    response.setTitle(
            "Financial Institution Credit Transfer"
    );


    // =========================
    // GROUP HEADER
    // =========================

    addIfPresent(
            response,
            "Message ID",
            getElementValue(
                    document,
                    "MsgId"
            )
    );

    addIfPresent(
            response,
            "Creation Date",
            getElementValue(
                    document,
                    "CreDtTm"
            )
    );

    addIfPresent(
            response,
            "Number of Transactions",
            getElementValue(
                    document,
                    "NbOfTxs"
            )
    );


    // =========================
    // PAYMENT IDENTIFICATION
    // =========================

    addIfPresent(
            response,
            "Instruction ID",
            getElementValue(
                    document,
                    "InstrId"
            )
    );

    addIfPresent(
            response,
            "End To End ID",
            getElementValue(
                    document,
                    "EndToEndId"
            )
    );

    addIfPresent(
            response,
            "Transaction ID",
            getElementValue(
                    document,
                    "TxId"
            )
    );

    addIfPresent(
            response,
            "UETR",
            getElementValue(
                    document,
                    "UETR"
            )
    );


    // =========================
    // SETTLEMENT AMOUNT
    // =========================

    Element amountElement =
            getElement(
                    document,
                    "IntrBkSttlmAmt"
            );

    if (amountElement != null) {

        response.getSummary().put(
                "Settlement Amount",
                amountElement
                        .getTextContent()
                        .trim()
        );

        String currency =
                amountElement.getAttribute(
                        "Ccy"
                );

        if (currency != null
                && !currency.isBlank()) {

            response.getSummary().put(
                    "Currency",
                    currency
            );
        }
    }


    // =========================
    // SETTLEMENT DETAILS
    // =========================

    addIfPresent(
            response,
            "Settlement Date",
            getElementValue(
                    document,
                    "IntrBkSttlmDt"
            )
    );


    // =========================
    // INSTRUCTING AGENT
    // =========================

    Element instructingAgent =
            getElement(
                    document,
                    "InstgAgt"
            );

    if (instructingAgent != null) {

        addIfPresent(
                response,
                "Instructing Agent BIC",
                getChildElementValue(
                        instructingAgent,
                        "BICFI"
                )
        );
    }


    // =========================
    // INSTRUCTED AGENT
    // =========================

    Element instructedAgent =
            getElement(
                    document,
                    "InstdAgt"
            );

    if (instructedAgent != null) {

        addIfPresent(
                response,
                "Instructed Agent BIC",
                getChildElementValue(
                        instructedAgent,
                        "BICFI"
                )
        );
    }


    // =========================
    // DEBTOR
    // =========================

    Element debtor =
            getElement(
                    document,
                    "Dbtr"
            );

    if (debtor != null) {

        addIfPresent(
                response,
                "Debtor",
                getChildElementValue(
                        debtor,
                        "Nm"
                )
        );
    }


    // =========================
    // DEBTOR AGENT
    // =========================

    Element debtorAgent =
            getElement(
                    document,
                    "DbtrAgt"
            );

    if (debtorAgent != null) {

        addIfPresent(
                response,
                "Debtor Agent BIC",
                getChildElementValue(
                        debtorAgent,
                        "BICFI"
                )
        );
    }


    // =========================
    // CREDITOR AGENT
    // =========================

    Element creditorAgent =
            getElement(
                    document,
                    "CdtrAgt"
            );

    if (creditorAgent != null) {

        addIfPresent(
                response,
                "Creditor Agent BIC",
                getChildElementValue(
                        creditorAgent,
                        "BICFI"
                )
        );
    }


    // =========================
    // CREDITOR
    // =========================

    Element creditor =
            getElement(
                    document,
                    "Cdtr"
            );

    if (creditor != null) {

        addIfPresent(
                response,
                "Creditor",
                getChildElementValue(
                        creditor,
                        "Nm"
                )
        );
    }


    // =========================
    // CHARGE BEARER
    // =========================

    addIfPresent(
            response,
            "Charge Bearer",
            getElementValue(
                    document,
                    "ChrgBr"
            )
    );


    // =========================
    // REMITTANCE INFORMATION
    // =========================

    addIfPresent(
            response,
            "Remittance Information",
            getElementValue(
                    document,
                    "Ustrd"
            )
    );
}



}