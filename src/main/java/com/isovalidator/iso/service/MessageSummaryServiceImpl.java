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
}

private void generatePain001Summary(
        Document document,
        MessageSummaryResponse response) {

    response.setTitle(
            "Customer Credit Transfer Initiation"
    );
}

private void generateCamt053Summary(
        Document document,
        MessageSummaryResponse response) {

    response.setTitle(
            "Bank To Customer Statement"
    );
}



}