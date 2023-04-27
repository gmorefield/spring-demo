package com.example.springdemo.util;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.Data;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import static javax.xml.bind.Marshaller.JAXB_FRAGMENT;
import static org.assertj.core.api.Assertions.*;

public class XmlMapperTests {

    @Test
    public void testMapSerialization() throws JsonProcessingException, JAXBException {
        String expectedXml = "<Author><first>Mark</first><last>Twain</last></Author>";

        XmlMapper mapper = XmlMapper.builder().build();
        Map<String, String> author = mapper.readValue(expectedXml, Map.class);
        assertThat(author).contains(
                entry("first", "Mark"),
                entry("last", "Twain")
        );

        String actualXml = mapper.writer().withRootName("Author").writeValueAsString(author);
        assertThat(actualXml).isEqualTo(expectedXml);

        // wrapped list
        JAXBContext context = JAXBContext.newInstance(MapList.class, MapWrapper.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(JAXB_FORMATTED_OUTPUT, false);
        marshaller.setProperty(JAXB_FRAGMENT, true);

        MapList mapList = new MapList();
        mapList.authors.add(author);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        marshaller.marshal(mapList, baos);
        assertThat(baos.toString(StandardCharsets.UTF_8)).isEqualTo("<AuthorList>" + expectedXml + "</AuthorList>");
    }

    @Test
    public void testAuthorSerialization() throws JsonProcessingException {
        String expectedXml = "<Author><first>Mark</first><last>Twain</last></Author>";

        XmlMapper mapper = XmlMapper.builder().build();
        Author author = mapper.readValue(expectedXml, Author.class);
        assertThat(author).satisfies(a -> {
            assertThat(a.getFirst()).isEqualTo("Mark");
            assertThat(a.getLast()).isEqualTo("Twain");
        });

        String actualXml = mapper.writer().withRootName("Author").writeValueAsString(author);
        assertThat(actualXml).isEqualTo(expectedXml);
    }

    @Test
    public void testAuthorUnknownPropSerialization() throws JsonProcessingException {
        String expectedXml = "<Author><first>Mark</first><title>Mr</title><last>Twain</last></Author>";

        XmlMapper mapper = XmlMapper.builder().configure(FAIL_ON_UNKNOWN_PROPERTIES, true).build();
        assertThatThrownBy(() -> mapper.readValue(expectedXml, Author.class))
                .hasMessageContaining("Unrecognized field \"title\"");
    }

    @Test
    public void testAuthorMapSerialization() throws JsonProcessingException, JAXBException {
        String expectedXml = "<Author><first>Mark</first><last>Twain</last></Author>";

        XmlMapper mapper = XmlMapper.builder().build();
        AuthorMap author = mapper.readValue(expectedXml, AuthorMap.class);
        assertThat(author).contains(
                entry("first", "Mark"),
                entry("last", "Twain")
        );

        String actualXml = mapper.writer().withRootName("Author").writeValueAsString(author);
        assertThat(actualXml).isEqualTo(expectedXml);

        JAXBContext context = JAXBContext.newInstance(AuthorMap.class, AuthorMapList.class, MapWrapper.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(JAXB_FORMATTED_OUTPUT, false);
        marshaller.setProperty(JAXB_FRAGMENT, true);

        AuthorMapList authorList = new AuthorMapList();
        authorList.authors.add(author);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        marshaller.marshal(authorList, baos);
        assertThat(baos.toString(StandardCharsets.UTF_8)).isEqualTo("<AuthorList>" + expectedXml + "</AuthorList>");
    }

    @Test
    public void testAuthorHybridSerialization() throws JsonProcessingException {
        String expectedXml = "<Author><first>Mark</first><last>Twain</last></Author>";

        XmlMapper mapper = XmlMapper.builder().build();
        AuthorHybrid author = mapper.readValue(expectedXml, AuthorHybrid.class);
        assertThat(author).contains(
                entry("first", "Mark"),
                entry("last", "Twain")
        );
        assertThat(author.getFirst()).isEqualTo("Mark");

        String actualXml = mapper.writeValueAsString(author);
        assertThat(actualXml).isEqualTo(expectedXml);
    }

    @XmlRootElement(name = "AuthorList")
    public static class MapList {
        @XmlElement(name = "Author")
        @XmlJavaTypeAdapter(MapAdapter.class)
        public List<Map<String, String>> authors = new ArrayList<>();
    }

    @Data
    public static class Author {
        private String first;
        private String last;
    }

    @XmlRootElement(name = "AuthorList")
    public static class AuthorMapList {
        @XmlElement(name = "Author")
        @XmlJavaTypeAdapter(MapAdapter.class)
        public List<AuthorMap> authors = new ArrayList<>();
    }

    @XmlRootElement(name = "Author")
    public static class AuthorMap extends LinkedHashMap<String, String> {
    }

    @JsonRootName("Author")
    public static class AuthorHybrid extends LinkedHashMap<String, String> {
        private String first;

        public String getFirst() {
            return this.get("first");
        }

        public void setFirst(String first) {
            this.put("first", first);
        }
    }

}
