package com.example.springdemo.util;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;

import java.io.IOException;

public class FruitResolver extends TypeIdResolverBase {
    public FruitResolver() {
    }

    @Override
    public String idFromValue(Object value) {
        return Object.class.getSimpleName();
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        throw new UnsupportedOperationException("Unimplemented method 'idFromValueAndType'");
    }

    @Override
    public Id getMechanism() {
        throw new UnsupportedOperationException("Unimplemented method 'getMechanism'");
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) throws IOException {
        return super.typeFromId(context, id);
    }
}
