package com.example.springdemo.util;

import com.example.springdemo.model.CloudEventDto;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.END_OBJECT;
import static com.fasterxml.jackson.core.JsonToken.FIELD_NAME;
import static com.fasterxml.jackson.core.JsonToken.START_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.START_OBJECT;

public class CloudEventExtractor {
    public CloudEventExtractor() {
    }

    public CloudEventDto streamToXml(final InputStream source, final OutputStream copy) {
        Map<String, String> eventData = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonFactory factory = new JsonFactory();
            JsonParser parser = factory.createParser(source);
            JsonGenerator generator = factory.createGenerator(copy, JsonEncoding.UTF8);

            while (!parser.isClosed()) {
                JsonToken jsonToken = parser.nextToken();
                if (jsonToken == null) {
                    break;
                }

                if (START_OBJECT.equals(jsonToken)) {
                    generator.writeStartObject();

                } else if (END_ARRAY.equals(jsonToken)) {
                    generator.writeEndArray();
                } else if (END_OBJECT.equals(jsonToken)) {
                    generator.writeEndObject();
                } else if (FIELD_NAME.equals(jsonToken)) {
                    // will be handled by START_OBJECT, START_ARRAY, or scalar value so do nothing
                } else if (START_ARRAY.equals(jsonToken)) {
                    if (parser.getParsingContext().getParent().hasCurrentName()) {
                        generator.writeArrayFieldStart(parser.getParsingContext().getParent().getCurrentName());
                    } else {
                        generator.writeStartArray();
                    }
                } else if (jsonToken.isScalarValue() && parser.getParsingContext().inArray()) {
                    if (jsonToken.isBoolean()) {
                        generator.writeBoolean(parser.getBooleanValue());
                    } else if (JsonToken.VALUE_NUMBER_FLOAT.equals(jsonToken)) {
                        generator.writeNumber(parser.getFloatValue());
                    } else if (JsonToken.VALUE_NUMBER_INT.equals(jsonToken)) {
                        generator.writeNumber(parser.getLongValue());
                    } else if (JsonToken.VALUE_STRING.equals(jsonToken)) {
                        generator.writeString(parser.getValueAsString());
                    } else if (JsonToken.VALUE_NULL.equals(jsonToken)) {
                        generator.writeNull();
                    }
                } else if (jsonToken.isScalarValue()) {
                    String fieldName = parser.currentName();
                    if (parser.getParsingContext().getParent().inRoot() && !"data".equals(fieldName)) {
                        JsonNode fieldValue = mapper.readTree(parser);
                        eventData.put(fieldName, fieldValue.asText());

                        generator.writeFieldName(fieldName);
                        mapper.writeValue(generator, fieldValue);
                    } else if (jsonToken.isBoolean()) {
                        generator.writeBooleanField(fieldName, parser.getBooleanValue());
                    } else if (JsonToken.VALUE_NUMBER_FLOAT.equals(jsonToken)) {
                        generator.writeNumberField(fieldName, parser.getFloatValue());
                    } else if (JsonToken.VALUE_NUMBER_INT.equals(jsonToken)) {
                        generator.writeNumberField(fieldName, parser.getLongValue());
                    } else if (JsonToken.VALUE_STRING.equals(jsonToken)) {
                        generator.writeStringField(fieldName, parser.getValueAsString());
                    } else if (JsonToken.VALUE_NULL.equals(jsonToken)) {
                        generator.writeNullField(fieldName);
                    }
                } else {
                    throw new UnsupportedOperationException(
                            "JSON Token " + jsonToken.toString() + " not currently supported");
                }
            }

            generator.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract fields from JSON", e);
        }

        return mapper.convertValue(eventData, CloudEventDto.class);
    }
}
