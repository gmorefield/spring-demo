package com.example.springdemo.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonConverterTest {
    private JsonConverter jsonConverter = new JsonConverter();

    @ParameterizedTest
    @CsvSource(value = {
            "[]| <array name=\"root\"/>| <root/>",
            "{}| <root/>| <root/>"
    }, delimiter = '|')
    public void testRootObjectSupportsContainers(String json, String expected, String expectedCompact) {
        runTest(json, expected, expectedCompact);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "[{\"fieldNm\":\"fieldVal\"}]| <array name=\"root\"><object name=\"idx0\"><value name=\"fieldNm\">fieldVal</value></object></array>| <root><idx0><fieldNm>fieldVal</fieldNm></idx0></root>",
            "[{\"id\":\"obj1\"},{\"id\":\"obj2\"}]| <array name=\"root\"><object name=\"idx0\"><value name=\"id\">obj1</value></object><object name=\"idx1\"><value name=\"id\">obj2</value></object></array>| <root><idx0><id>obj1</id></idx0><idx1><id>obj2</id></idx1></root>"
    }, delimiter = '|')
    public void testRootArrayCanContainObjects(String json, String expected, String expectedCompact) {
        runTest(json, expected, expectedCompact);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "[{\"fieldNm\":\"fieldVal\"}]| <array name=\"root\"><object name=\"idx0\"><value name=\"fieldNm\">fieldVal</value></object></array>",
            "{\"books\":[{\"id\":\"book1\"},{\"id\":\"book2\"}]}| <root><array name=\"books\"><object name=\"book\"><value name=\"id\">book1</value></object><object name=\"book\"><value name=\"id\">book2</value></object></array></root>"
    }, delimiter = '|')
    public void testArrayNaming(String json, String expected) {
        runTest(json, expected, null);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "[1, true, \"text\"]| <array name=\"root\"><value name=\"idx0\">1</value><value name=\"idx1\">true</value><value name=\"idx2\">text</value></array>| <root><idx0>1</idx0><idx1>true</idx1><idx2>text</idx2></root>",
            "{\"scalars\":[1,true,\"text\"]}| <root><array name=\"scalars\"><value name=\"idx0\">1</value><value name=\"idx1\">true</value><value name=\"idx2\">text</value></array></root>| <root><scalars><idx0>1</idx0><idx1>true</idx1><idx2>text</idx2></scalars></root>"
    }, delimiter = '|')
    public void testScalarArray(String json, String expected, String expectedCompact) {
        runTest(json, expected, expectedCompact);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "{\"user\":{\"userId\":\"jdoe1\"}}| <root><object name=\"user\"><value name=\"userId\">jdoe1</value></object></root>| <root><user><userId>jdoe1</userId></user></root>"
    }, delimiter = '|')
    public void testNestedObjects(String json, String expected, String expectedCompact) {
        runTest(json, expected, expectedCompact);
    }

    private void runTest(String json, String expected, String expectedCompact) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        jsonConverter.useCompactFormat(false).streamToXml(inputStream, outputStream);
        assertThat(outputStream.toString()).isEqualTo(expected);

        if (StringUtils.hasText(expectedCompact)) {
            inputStream.reset();
            outputStream = new ByteArrayOutputStream();
            jsonConverter.useCompactFormat(true).streamToXml(inputStream, outputStream);
            assertThat(outputStream.toString()).isEqualTo(expectedCompact);
        }
    }

}
