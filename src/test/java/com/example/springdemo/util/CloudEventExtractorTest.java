package com.example.springdemo.util;

import com.example.springdemo.model.CloudEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

public class CloudEventExtractorTest {
    @ParameterizedTest
    @CsvSource(value = {
            "[]| <array name=\"root\"/>",
            "{}| <root/>"
    }, delimiter = '|')
    public void testRootObjectSupportsContainers(String json, String expected) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CloudEventDto event = new CloudEventExtractor().streamToXml(inputStream, outputStream);
        Assertions.assertThat(outputStream.toString()).isEqualTo(json);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "[{\"stringField\":\"string\"}]",
            "[{\"intField\":5}]",
            "[{\"boolField\":true}]",
            "[{\"nullField\":null}]",
            "[{\"floatField\":5.6}]",
            "[{\"arrayField\":[null,true,5,5.6,\"string\",{},[]]}]",
            "[{\"nestedArrayField\":[[null,true]]}]",
            "[{\"nestedObjectArrayField\":[{\"nestedObjectField\":[null,true]}]}]",
            "[null,true,5,5.6,\"string\",{},[]]"
    }, delimiter = '|')
    public void testObjectArrayFields(String json) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CloudEventDto event = new CloudEventExtractor().streamToXml(inputStream, outputStream);
        Assertions.assertThat(outputStream.toString()).isEqualTo(json);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "{\"specversion\":\"1.0\"}| "
    }, delimiter = '|')
    public void testExtractedFields(String json, String expected) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CloudEventDto event = new CloudEventExtractor().streamToXml(inputStream, outputStream);
        Assertions.assertThat(outputStream.toString()).isEqualTo(json);
        Assertions.assertThat(event).usingRecursiveComparison()
                .isEqualTo(new ObjectMapper().readValue(new StringReader(json), CloudEventDto.class));
    }

}
