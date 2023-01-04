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
        "[{\"fieldNm\":\"fieldVal\"}]| <array name=\"root\"><object name=\"idx0\"><value name=\"fieldNm\">fieldVal</value></object></array>",
        "[{\"id\":\"obj1\"},{\"id\":\"obj2\"}]| <array name=\"root\"><object name=\"idx0\"><value name=\"id\">obj1</value></object><object name=\"idx1\"><value name=\"id\">obj2</value></object></array>"
    }, delimiter = '|')
    public void testRootArrayCanContainObjects(String json, String expected) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new JsonConverter().streamToXml(inputStream, outputStream);
        Assertions.assertThat(outputStream.toString()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "[{\"fieldNm\":\"fieldVal\"}]| <array name=\"root\"><object name=\"idx0\"><value name=\"fieldNm\">fieldVal</value></object></array>",
        "{\"books\":[{\"id\":\"book1\"},{\"id\":\"book2\"}]}| <root><array name=\"books\"><object name=\"book\"><value name=\"id\">book1</value></object><object name=\"book\"><value name=\"id\">book2</value></object></array></root>"
    }, delimiter = '|')
    public void testArrayNaming(String json, String expected) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new JsonConverter().streamToXml(inputStream, outputStream);
        Assertions.assertThat(outputStream.toString()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "[1, true, \"text\"]| <array name=\"root\"><value name=\"idx0\">1</value><value name=\"idx1\">true</value><value name=\"idx2\">text</value></array>",
        "{\"scalars\":[1,true,\"text\"]}| <root><array name=\"scalars\"><value name=\"idx0\">1</value><value name=\"idx1\">true</value><value name=\"idx2\">text</value></array></root>"
    }, delimiter = '|')
    public void testScalarArray(String json, String expected) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new JsonConverter().streamToXml(inputStream, outputStream);
        Assertions.assertThat(outputStream.toString()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "{\"user\":{\"userId\":\"jdoe1\"}}| <root><object name=\"user\"><value name=\"userId\">jdoe1</value></object></root>"
    }, delimiter = '|')
    public void testNestedObjects(String json, String expected) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new JsonConverter().streamToXml(inputStream, outputStream);
        Assertions.assertThat(outputStream.toString()).isEqualTo(expected);
    }

}
