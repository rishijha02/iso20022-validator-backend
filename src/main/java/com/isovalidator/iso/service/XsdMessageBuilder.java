package com.isovalidator.iso.service;

import org.springframework.stereotype.Component;
import com.isovalidator.iso.DTO.GeneratorField;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/** Builds a message by walking the selected XSD's element and type declarations. */
@Component
public class XsdMessageBuilder {
    private static final String XSD_NS = XMLConstants.W3C_XML_SCHEMA_NS_URI;

    public String build(InputStream schemaStream, Map<String, String> values) throws Exception {
        DocumentBuilderFactory factory = secureFactory();
        Document schema = factory.newDocumentBuilder().parse(schemaStream);
        Element schemaRoot = schema.getDocumentElement();
        String targetNamespace = schemaRoot.getAttribute("targetNamespace");

        Map<String, Element> namedTypes = new HashMap<>();
        Element rootDeclaration = null;
        for (Element child : childElements(schemaRoot)) {
            if (isXsd(child, "complexType") || isXsd(child, "simpleType")) {
                namedTypes.put(child.getAttribute("name"), child);
            } else if (isXsd(child, "element") && "Document".equals(child.getAttribute("name"))) {
                rootDeclaration = child;
            }
        }
        if (rootDeclaration == null) throw new IllegalArgumentException("Selected XSD has no Document root element");

        Document output = factory.newDocumentBuilder().newDocument();
        Element root = output.createElementNS(targetNamespace, "Document");
        output.appendChild(root);
        Element rootType = namedTypes.get(localTypeName(rootDeclaration.getAttribute("type")));
        if (rootType == null) throw new IllegalArgumentException("Document root type is missing from the selected XSD");
        buildComplexType(output, root, rootType, "/Document", namedTypes, values, targetNamespace, 0);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        var transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(output), new StreamResult(writer));
        return writer.toString();
    }

    private boolean buildElement(Document output, Element parent, Element declaration, String path,
                                 Map<String, Element> namedTypes, Map<String, String> values,
                                 String targetNamespace, int depth) throws Exception {
        if (depth > 64) throw new IllegalStateException("XSD nesting exceeds the supported depth");
        String name = declaration.getAttribute("name");
        if (name.isBlank()) return false;
        Element valueElement = output.createElementNS(targetNamespace, name);
        parent.appendChild(valueElement);

        Element type = findInlineType(declaration);
        String typeName = declaration.getAttribute("type");
        if (type == null && !typeName.isBlank() && !typeName.startsWith("xs:")) {
            type = namedTypes.get(localTypeName(typeName));
        }

        if (type != null && isXsd(type, "complexType")) {
            buildComplexType(output, valueElement, type, path, namedTypes, values, targetNamespace, depth + 1);
        } else {
            String text = values.get(path);
            if (text == null) text = sampleValue(type, typeName, path, namedTypes);
            valueElement.setTextContent(text);
        }

        if (type != null && isXsd(type, "complexType")) {
            addRequiredAttributes(output, valueElement, type, path, values);
        }
        return true;
    }

    private void buildComplexType(Document output, Element parent, Element complexType, String path,
                                  Map<String, Element> namedTypes, Map<String, String> values,
                                  String targetNamespace, int depth) throws Exception {
        Element simpleContent = firstChild(complexType, "simpleContent");
        if (simpleContent != null) {
            Element extension = firstChild(simpleContent, "extension");
            if (extension != null) {
                String text = values.get(path);
                if (text == null) text = sampleValue(namedTypes.get(localTypeName(extension.getAttribute("base"))), extension.getAttribute("base"), path, namedTypes);
                parent.setTextContent(text);
                addRequiredAttributes(output, parent, extension, path, values);
            }
            return;
        }

        for (Element child : childElements(complexType)) {
            if (isXsd(child, "sequence") || isXsd(child, "all")) {
                buildParticle(output, parent, child, path, namedTypes, values, targetNamespace, depth + 1, false);
            } else if (isXsd(child, "choice")) {
                buildParticle(output, parent, child, path, namedTypes, values, targetNamespace, depth + 1, false);
            }
        }
    }

    private void buildParticle(Document output, Element parent, Element particle, String parentPath,
                               Map<String, Element> namedTypes, Map<String, String> values,
                               String targetNamespace, int depth, boolean selectedByChoice) throws Exception {
        if (depth > 64) throw new IllegalStateException("XSD nesting exceeds the supported depth");
        if (isXsd(particle, "choice")) {
            Element selected = null;
            for (Element option : childElements(particle)) {
                if (!isXsd(option, "element")) continue;
                String candidatePath = parentPath + "/" + option.getAttribute("name");
                if (hasValueAtOrBelow(candidatePath, values)) { selected = option; break; }
                if (selected == null) selected = option;
            }
            if (selected != null) {
                String childPath = parentPath + "/" + selected.getAttribute("name");
                buildElement(output, parent, selected, childPath, namedTypes, values, targetNamespace, depth + 1);
            }
            return;
        }

        for (Element child : childElements(particle)) {
            if (isXsd(child, "element")) {
                String childPath = parentPath + "/" + child.getAttribute("name");
                boolean required = !"0".equals(child.getAttribute("minOccurs"));
                if (required || hasValueAtOrBelow(childPath, values)) {
                    int minimum = parseOccurs(child.getAttribute("minOccurs"), 1);
                    int count = Math.max(minimum, 1);
                    for (int i = 0; i < count; i++) {
                        buildElement(output, parent, child, childPath, namedTypes, values, targetNamespace, depth + 1);
                    }
                }
            } else if (isXsd(child, "sequence") || isXsd(child, "all") || isXsd(child, "choice")) {
                boolean required = !"0".equals(child.getAttribute("minOccurs"));
                if (required || hasValueAtOrBelow(parentPath, values)) {
                    buildParticle(output, parent, child, parentPath, namedTypes, values, targetNamespace, depth + 1, false);
                }
            }
        }
    }

    private void addRequiredAttributes(Document output, Element element, Element type,
                                       String path, Map<String, String> values) {
        for (Element child : childElements(type)) {
            if (!isXsd(child, "attribute") || !"required".equals(child.getAttribute("use"))) continue;
            String name = child.getAttribute("name");
            String value = values.get(path + "/@" + name);
            if (value == null && "Ccy".equals(name)) value = values.getOrDefault("/Document/FIToFICstmrCdtTrf/CdtTrfTxInf/IntrBkSttlmAmt/@Ccy", "EUR");
            if (value == null) value = sampleValue(null, child.getAttribute("type"), path + "/@" + name, Map.of());
            element.setAttribute(name, value);
        }
    }

    public List<GeneratorField> discoverRequiredFields(InputStream schemaStream) throws Exception {
        DocumentBuilderFactory factory = secureFactory();
        Document schema = factory.newDocumentBuilder().parse(schemaStream);
        Element schemaRoot = schema.getDocumentElement();
        Map<String, Element> namedTypes = namedTypes(schemaRoot);
        Element documentElement = null;
        for (Element child : childElements(schemaRoot)) {
            if (isXsd(child, "element") && "Document".equals(child.getAttribute("name"))) { documentElement = child; break; }
        }
        if (documentElement == null) throw new IllegalArgumentException("Selected XSD has no Document root element");
        Element rootType = namedTypes.get(localTypeName(documentElement.getAttribute("type")));
        if (rootType == null) throw new IllegalArgumentException("Document root type is missing from selected XSD");
        List<GeneratorField> fields = new ArrayList<>();
        discoverComplexFields(rootType, "/Document", namedTypes, fields, 0);
        return fields;
    }

    private void discoverComplexFields(Element complexType, String path, Map<String, Element> namedTypes,
                                       List<GeneratorField> fields, int depth) {
        if (depth > 64) return;
        Element simpleContent = firstChild(complexType, "simpleContent");
        if (simpleContent != null) {
            Element extension = firstChild(simpleContent, "extension");
            if (extension != null) {
                fields.add(field(path, pathLeaf(path), true, "number", sampleValue(null, extension.getAttribute("base"), path, namedTypes)));
                discoverAttributes(extension, path, fields, namedTypes);
            }
            return;
        }
        for (Element child : childElements(complexType)) {
            if (isXsd(child, "sequence") || isXsd(child, "all")) discoverParticleFields(child, path, namedTypes, fields, depth + 1);
            else if (isXsd(child, "choice")) discoverChoiceFields(child, path, namedTypes, fields, depth + 1);
            else if (isXsd(child, "attribute") && "required".equals(child.getAttribute("use"))) {
                String attributePath = path + "/@" + child.getAttribute("name");
                fields.add(field(attributePath, humanize(child.getAttribute("name")), true, "text", sampleValue(null, child.getAttribute("type"), attributePath, namedTypes)));
            }
        }
    }

    private void discoverParticleFields(Element particle, String parentPath, Map<String, Element> namedTypes,
                                        List<GeneratorField> fields, int depth) {
        if (depth > 64) return;
        for (Element child : childElements(particle)) {
            if (isXsd(child, "element")) {
                if ("0".equals(child.getAttribute("minOccurs"))) continue;
                String name = child.getAttribute("name");
                if (name.isBlank()) continue;
                String path = parentPath + "/" + name;
                Element type = findInlineType(child);
                String typeName = child.getAttribute("type");
                if (type == null && !typeName.isBlank() && !typeName.startsWith("xs:")) type = namedTypes.get(localTypeName(typeName));
                if (type != null && isXsd(type, "complexType")) discoverComplexFields(type, path, namedTypes, fields, depth + 1);
                else fields.add(field(path, humanize(name), true, isNumeric(typeName, type) ? "number" : "text", sampleValue(type, typeName, path, namedTypes)));
            } else if (isXsd(child, "sequence") || isXsd(child, "all")) {
                if (!"0".equals(child.getAttribute("minOccurs"))) discoverParticleFields(child, parentPath, namedTypes, fields, depth + 1);
            } else if (isXsd(child, "choice")) discoverChoiceFields(child, parentPath, namedTypes, fields, depth + 1);
        }
    }

    private void discoverChoiceFields(Element choice, String parentPath, Map<String, Element> namedTypes,
                                      List<GeneratorField> fields, int depth) {
        for (Element option : childElements(choice)) {
            if (isXsd(option, "element")) {
                Element oneOption = option.getOwnerDocument().createElementNS(XSD_NS, "xs:sequence");
                oneOption.appendChild(oneOption.getOwnerDocument().importNode(option, true));
                discoverParticleFields(oneOption, parentPath, namedTypes, fields, depth + 1);
                return;
            }
        }
    }

    private void discoverAttributes(Element type, String path, List<GeneratorField> fields, Map<String, Element> namedTypes) {
        for (Element child : childElements(type)) {
            if (isXsd(child, "attribute") && "required".equals(child.getAttribute("use"))) {
                String attributePath = path + "/@" + child.getAttribute("name");
                fields.add(field(attributePath, humanize(child.getAttribute("name")), true, "text", sampleValue(null, child.getAttribute("type"), attributePath, namedTypes)));
            }
        }
    }

    private GeneratorField field(String path, String label, boolean required, String inputType, String placeholder) {
        String displayPath = path.startsWith("/Document/") ? path.substring("/Document/".length()) : path;
        return new GeneratorField(path, label, "Required message fields", required, inputType, placeholder, displayPath);
    }

    private String sampleValue(Element type, String typeName, String path, Map<String, Element> namedTypes) {
        String lowerPath = path.toLowerCase(java.util.Locale.ROOT);
        if (lowerPath.endsWith("/@ccy") || lowerPath.endsWith("/ccy")) return "EUR";
        if (lowerPath.contains("bicfi") || lowerPath.endsWith("/bic")) return "COBADEFFXXX";
        if (lowerPath.contains("iban")) return "DE89370400440532013000";
        if (lowerPath.contains("uetr")) return UUID.randomUUID().toString();
        if (lowerPath.endsWith("/country") || lowerPath.endsWith("/ctry")) return "DE";
        if (lowerPath.contains("credtm")) return OffsetDateTime.now(ZoneOffset.UTC).toString();
        if (lowerPath.endsWith("/msgid")) return "MSG-" + UUID.randomUUID().toString().substring(0, 12);
        String enumValue = enumerationDefault(type, namedTypes);
        if (enumValue != null) return enumValue;
        String localType = typeName == null ? "" : localTypeName(typeName).toLowerCase(java.util.Locale.ROOT);
        Element restriction = type == null ? null : firstChild(type, "restriction");
        String base = restriction == null ? localType : localTypeName(restriction.getAttribute("base")).toLowerCase(java.util.Locale.ROOT);
        if (base.contains("datetime") || base.endsWith("dateandtime")) return OffsetDateTime.now(ZoneOffset.UTC).toString();
        if (base.endsWith("date") || lowerPath.endsWith("/dt")) return LocalDate.now(ZoneOffset.UTC).toString();
        if (base.contains("time")) return "12:00:00Z";
        if (base.contains("boolean")) return "false";
        if (base.contains("decimal") || base.contains("amount") || base.contains("integer") || base.contains("numeric") || base.equals("int") || base.equals("long")) return "1";
        if (base.contains("uuid")) return UUID.randomUUID().toString();
        if (base.contains("countrycode")) return "DE";
        if (base.contains("currency")) return "EUR";
        if (restriction != null) {
            String pattern = firstChild(restriction, "pattern") == null ? "" : firstChild(restriction, "pattern").getAttribute("value");
            if (pattern.contains("[A-Z]{2") && pattern.contains("[0-9]")) return "DE12A";
            if (pattern.contains("[A-Z]{3")) return "EUR";
            if (pattern.contains("[0-9]") || pattern.contains("\\d")) {
                java.util.regex.Matcher length = java.util.regex.Pattern.compile("\\{(\\d+)(?:,(\\d*))?}").matcher(pattern);
                int minimum = length.find() ? parseOccurs(length.group(1), 1) : 1;
                return "1".repeat(Math.min(Math.max(minimum, 1), 20));
            }
            Element minLength = firstChild(restriction, "minLength");
            if (minLength != null) {
                int length = parseOccurs(minLength.getAttribute("value"), 1);
                return "X".repeat(Math.min(Math.max(length, 1), 20));
            }
        }
        String fallback = "SAMPLE";
        Element maxLength = restriction == null ? null : firstChild(restriction, "maxLength");
        if (maxLength != null) {
            int maximum = parseOccurs(maxLength.getAttribute("value"), fallback.length());
            fallback = fallback.substring(0, Math.min(Math.max(maximum, 1), fallback.length()));
        }
        Element minLength = restriction == null ? null : firstChild(restriction, "minLength");
        if (minLength != null) {
            int minimum = parseOccurs(minLength.getAttribute("value"), fallback.length());
            if (minimum > fallback.length()) fallback = fallback + "X".repeat(Math.min(minimum - fallback.length(), 20));
        }
        return fallback;
    }

    private boolean isNumeric(String typeName, Element type) {
        String name = typeName == null ? "" : localTypeName(typeName).toLowerCase(java.util.Locale.ROOT);
        if (name.matches(".*(decimal|amount|integer|numeric|number|int|long|float|double).*")) return true;
        Element restriction = type == null ? null : firstChild(type, "restriction");
        if (restriction == null) return false;
        String base = localTypeName(restriction.getAttribute("base")).toLowerCase(java.util.Locale.ROOT);
        return base.matches(".*(decimal|integer|numeric|number|int|long|float|double).*");
    }

    private String pathLeaf(String path) {
        int slash = path.lastIndexOf('/');
        return humanize(slash < 0 ? path : path.substring(slash + 1));
    }

    private String humanize(String value) {
        String readable = value.replace("_", " ").replaceAll("([a-z])([A-Z])", "$1 $2");
        return readable.isEmpty() ? "Field" : readable.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + readable.substring(1);
    }

    private Map<String, Element> namedTypes(Element schemaRoot) {
        Map<String, Element> types = new HashMap<>();
        for (Element child : childElements(schemaRoot)) {
            if (isXsd(child, "complexType") || isXsd(child, "simpleType")) types.put(child.getAttribute("name"), child);
        }
        return types;
    }

    private DocumentBuilderFactory secureFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory;
    }

    private String enumerationDefault(Element type, Map<String, Element> namedTypes) {
        if (type == null) return null;
        Element restriction = firstChild(type, "restriction");
        if (restriction == null) {
            Element simpleContent = firstChild(type, "simpleContent");
            Element extension = simpleContent == null ? null : firstChild(simpleContent, "extension");
            if (extension != null) return enumerationDefault(namedTypes.get(localTypeName(extension.getAttribute("base"))), namedTypes);
            return null;
        }
        Element enumeration = firstChild(restriction, "enumeration");
        if (enumeration != null) return enumeration.getAttribute("value");
        String base = localTypeName(restriction.getAttribute("base"));
        return enumerationDefault(namedTypes.get(base), namedTypes);
    }

    private boolean hasValueAtOrBelow(String path, Map<String, String> values) {
        String prefix = path.endsWith("/") ? path : path + "/";
        return values.keySet().stream().anyMatch(key -> key.equals(path) || key.startsWith(prefix));
    }

    private Element findInlineType(Element declaration) {
        for (Element child : childElements(declaration)) {
            if (isXsd(child, "complexType") || isXsd(child, "simpleType")) return child;
        }
        return null;
    }

    private Element firstChild(Element parent, String name) {
        for (Element child : childElements(parent)) if (isXsd(child, name)) return child;
        return null;
    }

    private java.util.List<Element> childElements(Element parent) {
        java.util.List<Element> children = new java.util.ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element) children.add(element);
        }
        return children;
    }

    private boolean isXsd(Element element, String localName) {
        return XSD_NS.equals(element.getNamespaceURI()) && localName.equals(element.getLocalName());
    }

    private int parseOccurs(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (Exception ignored) { return fallback; }
    }

    private String localTypeName(String qname) {
        int colon = qname.indexOf(':');
        return colon < 0 ? qname : qname.substring(colon + 1);
    }
}
