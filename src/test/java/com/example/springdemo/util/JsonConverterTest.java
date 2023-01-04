package com.example.springdemo.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class JsonConverterTest {
    @ParameterizedTest
    @CsvSource({
        "[], <array name=\"root\"/>",
        "{}, <root/>"
    })
    public void testRootObjectSupportsContainers(String json, String expected) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new JsonConverter().streamToXml(inputStream, outputStream);
        Assertions.assertThat(outputStream.toString()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "[{\"fieldNm\":\"fieldVal\"}]| <array name=\"root\"><object name=\"root\"><value name=\"fieldNm\">fieldVal</value></object></array>",
        "[{\"id\":\"obj1\"},{\"id\":\"obj2\"}]| <array name=\"root\"><object name=\"root\"><value name=\"id\">obj1</value></object><object name=\"root\"><value name=\"id\">obj2</value></object></array>"
    }, delimiter = '|')
    public void testRootArrayCanContainObjects(String json, String expected) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new JsonConverter().streamToXml(inputStream, outputStream);
        Assertions.assertThat(outputStream.toString()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "[{\"fieldNm\":\"fieldVal\"}]| <array name=\"root\"><object name=\"root\"><value name=\"fieldNm\">fieldVal</value></object></array>",
        "{\"objects\":[{\"id\":\"obj1\"},{\"id\":\"obj2\"}]}| <root><array name=\"objects\"><object name=\"object\"><value name=\"id\">obj1</value></object><object name=\"object\"><value name=\"id\">obj2</value></object></array></root>"
    }, delimiter = '|')
    public void testArrayNaming(String json, String expected) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new JsonConverter().streamToXml(inputStream, outputStream);
        Assertions.assertThat(outputStream.toString()).isEqualTo(expected);
    }

}
