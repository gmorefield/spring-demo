package com.example.springdemo.util;

import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.END_OBJECT;
import static com.fasterxml.jackson.core.JsonToken.FIELD_NAME;
import static com.fasterxml.jackson.core.JsonToken.START_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.START_OBJECT;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class JsonConverter {
    private boolean omitXmlDeclaration = true;

    private String nodeNameRoot = "root";
    private String nodeNameObject = "object";
    private String nodeNameValue = "value";
    private String nodeNameArray = "array";

    public JsonConverter() {
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

            while (!parser.isClosed()) {
                JsonToken jsonToken = parser.nextToken();
                if (jsonToken == null) {
                    break;
                }

                if (!omitXmlDeclaration && parser.getParsingContext().getParent().inRoot()) {
                    writer.writeStartDocument("UTF-8", "1.0");
                }

                if (START_OBJECT.equals(jsonToken)) {
                    String objectName = parser.currentName();
                    if (parser.getParsingContext().getParent().inRoot()) {
                        objectName = nodeNameRoot;
                        writer.writeStartElement(objectName);
                    } else {
                        if (parser.getParsingContext().getParent().inArray()) {
                            objectName = Optional
                                    .ofNullable(parser.getParsingContext().getParent().getParent().getCurrentName())
                                    .orElse("idx" + parser.getParsingContext().getParent().getCurrentIndex());
                            if (objectName.endsWith("ses")) {
                                objectName = objectName.substring(0, objectName.length() - 2);
                            } else if (objectName.endsWith("s") && !objectName.endsWith("ss")) {
                                objectName = objectName.substring(0, objectName.length() - 1);
                            }
                        }
                        startElement(writer, nodeNameObject, objectName);
                    }
                } else if (END_OBJECT.equals(jsonToken) || END_ARRAY.equals(jsonToken)) {
                    writer.writeEndElement();
                } else if (FIELD_NAME.equals(jsonToken)) {
                    // will be handled by START_OBJECT, START_ARRAY, or scalar value so do nothing
                } else if (START_ARRAY.equals(jsonToken)) {
                    String arrayName = parser.currentName();
                    if (parser.getParsingContext().getParent().inRoot()) {
                        arrayName = nodeNameRoot;
                    }
                    startElement(writer, nodeNameArray, arrayName);
                } else if (jsonToken.isScalarValue()) {
                    String objectName = parser.currentName();
                    if (objectName == null) {
                        objectName = "idx" + parser.getParsingContext().getCurrentIndex();
                    }
                    startElement(writer, nodeNameValue, objectName);
                    writer.writeCharacters(parser.getValueAsString());
                    writer.writeEndElement();
                } else {
                    throw new UnsupportedOperationException(
                            "JSON Token " + jsonToken.toString() + " not currently supported");
                }
            }

            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to XML", e);
        }
    }

    private void startElement(XMLStreamWriter writer, String objectType, String objectName) throws XMLStreamException {
        writer.writeStartElement(objectType);
        writer.writeAttribute("name", objectName);
    }
}
