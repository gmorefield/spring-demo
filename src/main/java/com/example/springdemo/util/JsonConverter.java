package com.example.springdemo.util;

import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.END_OBJECT;
import static com.fasterxml.jackson.core.JsonToken.FIELD_NAME;
import static com.fasterxml.jackson.core.JsonToken.START_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.START_OBJECT;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Stack;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class JsonConverter {
    private boolean useNodeName = false;
    private boolean omitXmlDeclaration = true;

    private String nodeNameRoot = "root";
    private String nodeNameObject = "object";
    private String nodeNameValue = "value";
    private String nodeNameArray = "array";

    public JsonConverter() {}

    public JsonConverter useNodeName() {
        this.useNodeName = true;
        return this;
    }

    public JsonConverter root(String rootName) {
        this.nodeNameRoot = rootName;
        return this;
    }

    public void streamToXml(final InputStream source, final OutputStream dest) {
        try {
            JsonFactory factory = new JsonFactory();
            JsonParser parser = factory.createParser(source);

            XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
            XMLStreamWriter writer = outputFactory.createXMLStreamWriter(dest);

            Stack<String> navPath = new Stack<>();
            Stack<JsonToken> tokenPath = new Stack<>();
            while (!parser.isClosed()) {
                JsonToken jsonToken = parser.nextToken();
                if (jsonToken == null) {
                    break;
                }

                if (!omitXmlDeclaration && navPath.isEmpty()) {
                    writer.writeStartDocument("UTF-8", "1.0");
                }

                if (START_OBJECT.equals(jsonToken)) {
                    String objectName = parser.currentName();
                    if (navPath.isEmpty()) {
                        objectName = nodeNameRoot;
                        writer.writeStartElement(objectName);
                    } else {
                        if (START_ARRAY.equals(tokenPath.peek())) {
                            objectName = navPath.peek();
                            if (objectName.endsWith("ses")) {
                                objectName = objectName.substring(0, objectName.length() - 2);
                            } else if (objectName.endsWith("s") && !objectName.endsWith("ss")) {
                                objectName = objectName.substring(0, objectName.length() - 1);
                            }
                        }
                        startElement(writer, nodeNameObject, objectName, useNodeName);
                    }

                    tokenPath.push(jsonToken);
                    // if (objectName != null) {
                        navPath.push(objectName);
                    // }
                } else if (END_OBJECT.equals(jsonToken) || END_ARRAY.equals(jsonToken)) {
                    writer.writeEndElement();
                    navPath.pop();
                    tokenPath.pop();
                } else if (FIELD_NAME.equals(jsonToken)) {
                    // will be handled by START_OBJECT, START_ARRAY, or scalar value so do nothing
                } else if (START_ARRAY.equals(jsonToken)) {
                    String arrayName = parser.currentName();
                    if (navPath.isEmpty()) {
                        arrayName = nodeNameRoot;
                        // writer.writeStartElement(arrayName);
                    } else {
                    }
                    startElement(writer, nodeNameArray, arrayName, useNodeName);
                    navPath.push(arrayName);
                    tokenPath.push(jsonToken);
                } else if (jsonToken.isScalarValue()) {
                    startElement(writer, nodeNameValue, parser.currentName(), useNodeName);
                    writer.writeCharacters(parser.getValueAsString());
                    writer.writeEndElement();
                } else {
                    throw new UnsupportedOperationException("JSON Token " + jsonToken.toString() + " not currently supported");
                }
            }

            writer.flush();
            // writer.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to XML", e);
        }
    }

    private void startElement(XMLStreamWriter writer, String objectType, String objectName, boolean useNodeName)
            throws XMLStreamException {
        if (useNodeName) {
            writer.writeStartElement(cleanElementName(objectName));
        } else {
            writer.writeStartElement(objectType);
            writer.writeAttribute("name", objectName);
        }
    }

    private String cleanElementName(final String nodeName) {
        return nodeName.replace("$", "_").replace(":", "_");
    }
}
