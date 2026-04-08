package com.claudecode.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilsTest {

    record SampleRecord(String name, int value) {}

    @Test
    void toJson_andFromJson_roundTrip() {
        SampleRecord original = new SampleRecord("test", 42);
        String json = JsonUtils.toJson(original);
        SampleRecord restored = JsonUtils.fromJson(json, SampleRecord.class);
        assertEquals(original.name(), restored.name());
        assertEquals(original.value(), restored.value());
    }

    @Test
    void toPrettyJson_containsNewlines() {
        SampleRecord record = new SampleRecord("test", 1);
        String pretty = JsonUtils.toPrettyJson(record);
        assertTrue(pretty.contains("\n"));
    }

    @Test
    void tryFromJson_invalidReturnsEmpty() {
        Optional<SampleRecord> result = JsonUtils.tryFromJson("not json", SampleRecord.class);
        assertTrue(result.isEmpty());
    }

    @Test
    void tryFromJson_validReturnsPresent() {
        Optional<SampleRecord> result = JsonUtils.tryFromJson(
                "{\"name\":\"x\",\"value\":1}", SampleRecord.class);
        assertTrue(result.isPresent());
        assertEquals("x", result.get().name());
    }

    @Test
    void parseTree_basic() {
        JsonNode node = JsonUtils.parseTree("{\"key\":\"val\"}");
        assertEquals("val", node.get("key").asText());
    }

    @Test
    void nullFieldsOmitted() {
        record WithNull(String present, String absent) {}
        String json = JsonUtils.toJson(new WithNull("yes", null));
        assertFalse(json.contains("absent"));
        assertTrue(json.contains("present"));
    }
}
