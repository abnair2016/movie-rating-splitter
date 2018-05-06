package com.anair.demo.component.movieratingsplitter.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * JSON parsing and conversion utilities.
 */
public final class JSONUtil {

    private static final Supplier<ObjectMapper> OBJECT_MAPPER_SUPPLIER = () -> {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    };

    private static final ObjectMapper MAPPER = OBJECT_MAPPER_SUPPLIER.get();

    private JSONUtil() {
    }

    public static <T> String toJsonString(final T sourceItem) throws IOException {
        return MAPPER.writeValueAsString(sourceItem);
    }

    public static <T> T fromJsonString(final String sourceString, final Class<T> objectType) throws IOException {
        return !objectType.equals(String.class) ? MAPPER.readValue(sourceString, objectType) : objectType.cast(new String(sourceString));
    }

}
