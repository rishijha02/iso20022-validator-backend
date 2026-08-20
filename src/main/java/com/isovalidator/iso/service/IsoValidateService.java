package com.isovalidator.iso.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.springframework.stereotype.Service;

import com.isovalidator.iso.DTO.IsoResponseDTO;
//import com.isovalidator.iso.DTO.IsorequestDTO;
import com.isovalidator.iso.DTO.ValidationError;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;


import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IsoValidateService {

    public IsoResponseDTO validateMsg(String xml)
    {
        log.info("Entering Service Layer");

        IsoResponseDTO responseDTO= new IsoResponseDTO();

        String MessageXml=xml;

        System.out.println("Message is" + MessageXml);

        String messageXml = xml;

        if (messageXml == null || messageXml.isBlank()) {
            // set error response
            return responseDTO;
        }


        try {

            // Step 1: Parse XML
            Document document = parseXml(messageXml);

            // Step 2: Detect ISO 20022 message
            String namespace = detectIsoNamespace(document);

            log.info("ISO 20022 Namespace: {}", namespace);

            // Step 3: Extract message type/version
            String[] messageInfo = extractMessageInfo(namespace);

            String messageType = messageInfo[0];
            String version = messageInfo[1];

            log.info("Message Type: {}", messageType);
            log.info("Version: {}", version);

            // Step 4: Validate against XSD

            List<ValidationError> errors =
                validateAgainstXsd(
                        messageXml,
                        messageType,
                        version
                );
            

            // Step 5: Build successful response

            responseDTO.setMessageTyp(messageType);
            responseDTO.setVersion(version);
            responseDTO.setNamespace(namespace);

            if (errors.isEmpty()) {

            responseDTO.setValid(true);
            responseDTO.setMessage(
                    "ISO 20022 message is valid"
            );

        } else {

            responseDTO.setValid(false);
            responseDTO.setMessage(
                    "ISO 20022 message validation failed"
            );

            responseDTO.setErrors(errors);
        }

            return responseDTO;

        } catch (Exception e) {

            log.error("ISO 20022 validation failed", e);

             responseDTO.setValid(false);
            responseDTO.setMessage("Unable to process ISO 20022 message");

            return responseDTO;
        }

        
    }


    private Document parseXml(String xml) throws Exception {


    DocumentBuilderFactory factory =
            DocumentBuilderFactory.newInstance();

    factory.setNamespaceAware(true);

    DocumentBuilder builder = factory.newDocumentBuilder();

    try (InputStream inputStream =
                 new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {

        return builder.parse(inputStream);
    }
}

private String[] extractMessageInfo(String namespace) {

    if (namespace == null || namespace.isBlank()) {
        throw new IllegalArgumentException(
                "ISO 20022 namespace is missing");
    }

    String prefix =
            "urn:iso:std:iso:20022:tech:xsd:";

    if (!namespace.startsWith(prefix)) {
        throw new IllegalArgumentException(
                "Invalid ISO 20022 namespace");
    }

    String message = namespace.substring(prefix.length());

    // pacs.008.001.13

    String[] parts = message.split("\\.");

    if (parts.length < 4) {
        throw new IllegalArgumentException(
                "Invalid ISO 20022 message namespace");
    }

    String messageType =
            parts[0] + "." + parts[1];

    String version =
            parts[2] + "." + parts[3];

    return new String[] {
            messageType,
            version
    };
}

private List<ValidationError> validateAgainstXsd(
        String  messagexml,
        String messageType,
        String version) throws Exception {

            List<ValidationError> errors= new ArrayList<>();

            try
            {
                String message[]=messageType.split("\\.");
                // System.out.println("message array length "+ message.length);
                String msString=message[0]+message[1];
                System.out.println("message folder "+ msString);

                String schemaPath =
                    "/schemas/pacs/" +
                    msString + "/" +
                    messageType + "." +
                    version + ".xsd";

                    log.info("Loading schema: {}", schemaPath);

                    InputStream xsdStream =
                            getClass().getResourceAsStream(schemaPath);

                    if (xsdStream == null) {
                        throw new IllegalArgumentException(
                                "XSD schema not found: " + schemaPath);
                    }

                    SchemaFactory schemaFactory =
                            SchemaFactory.newInstance(
                                    XMLConstants.W3C_XML_SCHEMA_NS_URI);

                    Schema schema =
                            schemaFactory.newSchema(
                                    new StreamSource(xsdStream));

                    Validator validator =
                            schema.newValidator();
                    
                    validator.setErrorHandler(new DefaultHandler(){
                        @Override
                        public void error(SAXParseException e) {

                        errors.add(createValidationError(e));
                        }

                        @Override
                        public void fatalError(SAXParseException e) {

                            errors.add(createValidationError(e));
                        }

                        @Override
                        public void warning(SAXParseException e) {

                            // You can collect warnings later
                        }
                    }
                
                );

                    validator.validate( new StreamSource(new StringReader(messagexml)));
                            
            }
            catch (SAXException | IOException e)
            {
                log.error("Unable to perform XSD validation", e);

                // This is a technical/server problem,
                // not an ISO validation error.
                throw new RuntimeException("Unable to validate XML against XSD", e);
            }

            return errors;
            
            
}

private String detectIsoNamespace(Document document) {

    Element root = document.getDocumentElement();

    NamedNodeMap attributes = root.getAttributes();

    for (int i = 0; i < attributes.getLength(); i++) {

        Node attribute = attributes.item(i);

        String value = attribute.getNodeValue();

        if (value != null &&
            value.matches(
                "urn:iso:std:iso:20022:tech:xsd:(pacs|pain|camt|acmt|reda|semt|seev)\\..+")) {

            return value;
        }
    }

    return null;
}

private ValidationError createValidationError(
        SAXParseException e) {

    ValidationError error = new ValidationError();

    String message = e.getMessage();
    error.setCode(extractErrorCode(message));
    error.setMessage(e.getMessage());
    error.setLine(e.getLineNumber());
    error.setColumn(e.getColumnNumber());

    return error;
}

private String extractErrorCode(String message) {

    if (message == null) {
        return null;
    }

    int colonIndex = message.indexOf(':');

    if (colonIndex > 0) {
        return message.substring(0, colonIndex);
    }

    return null;
}

}
