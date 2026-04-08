package com.claudecode.session;

import com.claudecode.core.message.ContentBlock;
import com.claudecode.core.message.Message;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Factory for a pre-configured Jackson ObjectMapper used by the session module.
 * <p>
 * Configuration:
 * <ul>
 *   <li>NON_NULL serialization inclusion</li>
 *   <li>FAIL_ON_UNKNOWN_PROPERTIES = false</li>
 *   <li>Jdk8Module and JavaTimeModule registered</li>
 *   <li>Dates written as ISO-8601 strings, not timestamps</li>
 * </ul>
 * Polymorphic subtypes for {@link Message} and {@link ContentBlock} are handled
 * by their {@code @JsonTypeInfo} / {@code @JsonSubTypes} annotations on the sealed interfaces.
 */
public final class SessionObjectMapper {

    private static final ObjectMapper INSTANCE = createMapper();

    private SessionObjectMapper() {
        // utility class
    }

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }

    /**
     * Returns the shared, pre-configured ObjectMapper instance.
     */
    public static ObjectMapper get() {
        return INSTANCE;
    }
}
