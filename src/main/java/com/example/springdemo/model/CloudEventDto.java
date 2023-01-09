package com.example.springdemo.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CloudEventDto {
    private static List<String> propertiesToIgnore = List.of("data");

    @JsonAlias({"specversion","ce-specversion"})
    private String specversion;
    @JsonAlias({"type","ce-type"})
    private String type;
    @JsonAlias({"source","ce-source"})
    private String source;
    @JsonAlias({"subject","ce-subject"})
    private String subject;
    @JsonAlias({"id","ce-id"})
    private String id;
    // @JsonAlias({"time","ce-time"})
    // private LocalDateTime time;
    @JsonAlias({"datacontenttype","ce-datacontenttype"})
    private String datacontenttype;

    @JsonIgnore
    @JsonAlias({"data","ce-data"})
    private String data;

    @JsonIgnore
    Map<String, Object> other = new HashMap<>();

    /**
     * Jackson during deserialization will call this method for every unknown field
     */
    @JsonAnySetter
    public CloudEventDto addOther(String key, Object value) {
        if (key.startsWith("ce-")) {
            key = key.substring("ce-".length());
        }
        if (!propertiesToIgnore.contains(key)) {
            other.put(key, value);
        }

        return this;
    }

    /**
     * Jackson during will add all fields from other during serialization
     */
    @JsonAnyGetter
    public Map<String, Object> getOther() {
        return new HashMap<>(other); // return copy
    }
}
