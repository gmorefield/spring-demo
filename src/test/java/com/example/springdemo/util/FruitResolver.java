package com.example.springdemo.util;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;

public class FruitResolver extends TypeIdResolverBase {
    public FruitResolver() {
    }

    @Override
    public String idFromValue(Object value) {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'idFromValue'");
        return Object.class.getSimpleName();
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'idFromValueAndType'");
    }

    @Override
    public Id getMechanism() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getMechanism'");
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) throws IOException {
        // TODO Auto-generated method stub
        return super.typeFromId(context, id);
    }
}
