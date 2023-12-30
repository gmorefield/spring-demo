package com.example.springdemo.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class JsonTypeTest {

    @Test
    public void testTypeInfo() throws JsonMappingException, JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc,
                    JsonDeserializer<?> deserializer) {
                if (beanDesc.getBeanClass() == Fruit.class)
                    return new FruitDeserializer(deserializer);
                return deserializer;
            }
        });
        om.registerModule(module);

        new SimpleFruitDeserializer();

        Container source = new Container();
        source.setFruits(Collections.singletonMap("apple", new Apple()));
        System.out.println(om.writeValueAsString(source));

        String json = "{\"fruits\":{\"apple\":{\"species\":null,\"color\":\"red\"},\"banana\":{\"species\":null,\"length\":\"3\"}}}";

        Container c = om.readValue(json, Container.class);
        System.out.println(om.writeValueAsString(c));
    }

    public static class Container {

        @JsonDeserialize(contentUsing = SimpleFruitDeserializer.class)
        private Map<String, Fruit> fruits;

        public Map<String, Fruit> getFruits() {
            return fruits;
        }

        public void setFruits(Map<String, Fruit> fruits) {
            this.fruits = fruits;
        }

    }

    public enum FRUITS {

        APPLE(Apple.class), BANANA(Banana.class);

        private Class<? extends Fruit> subtype;

        private FRUITS(Class<? extends Fruit> subtype) {
            this.subtype = subtype;
        }

        public Class<? extends Fruit> getSubType() {
            return subtype;
        }


    }
    // @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, visible = false)
    // @JsonTypeIdResolver(value = FruitResolver.class)
    // @JsonSubTypes(value = {
    // @JsonSubTypes.Type(value = Apple.class, name = "apple"),
    // @JsonSubTypes.Type(value = Banana.class, name = "banana")
    // })
    public static class Fruit {
        private String species;

        public String getSpecies() {
            return species;
        }

        public void setSpecies(String species) {
            this.species = species;
        }

    }

    public static class Apple extends Fruit {
        private String color = "red";

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

    }

    public static class Banana extends Fruit {
        private String length;

        public String getLength() {
            return length;
        }

        public void setLength(String length) {
            this.length = length;
        }

    }

    public static class SimpleFruitDeserializer extends StdDeserializer<Fruit> {

        public SimpleFruitDeserializer() {
            super(Fruit.class);
        }

        @Override
        public Fruit deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            Fruit deserializedFruit = jp.readValueAs( FRUITS.valueOf(jp.currentName().toUpperCase()).getSubType());
            return deserializedFruit;
        }

    }

    public static class FruitDeserializer extends StdDeserializer<Fruit> implements ResolvableDeserializer {
        private static final long serialVersionUID = 7923585097068641765L;

        private final JsonDeserializer<Fruit> defaultDeserializer;

        public FruitDeserializer(JsonDeserializer<?> defaultDeserializer) {
            super(Fruit.class);
            this.defaultDeserializer = (JsonDeserializer<Fruit>) defaultDeserializer;
        }

        @Override
        public Fruit deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {

            // Fruit deserializedFruit = (Apple) defaultDeserializer.deserialize(jp, ctxt,
            // (Fruit) new Apple());
            Fruit deserializedFruit = jp.readValueAs(Apple.class);

            // Special logic

            return deserializedFruit;
        }

        // for some reason you have to implement ResolvableDeserializer when modifying
        // BeanDeserializer
        // otherwise deserializing throws JsonMappingException??
        @Override
        public void resolve(DeserializationContext ctxt) throws JsonMappingException {
            ((ResolvableDeserializer) defaultDeserializer).resolve(ctxt);
        }
    }
}
