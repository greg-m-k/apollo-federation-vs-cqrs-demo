package com.example.cdc.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CdcEvent model.
 */
class CdcEventTest {

    @Test
    void testDefaultConstructorSetsTimestamp() {
        CdcEvent event = new CdcEvent();

        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getTimestamp()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void testParameterizedConstructor() {
        Object payload = Map.of("id", "123", "name", "Test");
        CdcEvent event = new CdcEvent("products", "CREATE", payload);

        assertThat(event.getTable()).isEqualTo("products");
        assertThat(event.getOperation()).isEqualTo("CREATE");
        assertThat(event.getPayload()).isEqualTo(payload);
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    void testSettersAndGetters() {
        CdcEvent event = new CdcEvent();

        event.setId("event-123");
        event.setTable("categories");
        event.setOperation("UPDATE");
        event.setPayload("test payload");
        event.setOffset(42L);

        Instant now = Instant.now();
        event.setTimestamp(now);

        assertThat(event.getId()).isEqualTo("event-123");
        assertThat(event.getTable()).isEqualTo("categories");
        assertThat(event.getOperation()).isEqualTo("UPDATE");
        assertThat(event.getPayload()).isEqualTo("test payload");
        assertThat(event.getOffset()).isEqualTo(42L);
        assertThat(event.getTimestamp()).isEqualTo(now);
    }

    @Test
    void testPayloadCanBeAnyObject() {
        CdcEvent event = new CdcEvent();

        // Test with Map
        Map<String, Object> mapPayload = Map.of("key", "value");
        event.setPayload(mapPayload);
        assertThat(event.getPayload()).isEqualTo(mapPayload);

        // Test with String
        event.setPayload("string payload");
        assertThat(event.getPayload()).isEqualTo("string payload");

        // Test with null
        event.setPayload(null);
        assertThat(event.getPayload()).isNull();
    }

    @Test
    void testOperationTypes() {
        CdcEvent createEvent = new CdcEvent("products", "CREATE", null);
        CdcEvent updateEvent = new CdcEvent("products", "UPDATE", null);
        CdcEvent deleteEvent = new CdcEvent("products", "DELETE", null);
        CdcEvent upsertEvent = new CdcEvent("products", "UPSERT", null);

        assertThat(createEvent.getOperation()).isEqualTo("CREATE");
        assertThat(updateEvent.getOperation()).isEqualTo("UPDATE");
        assertThat(deleteEvent.getOperation()).isEqualTo("DELETE");
        assertThat(upsertEvent.getOperation()).isEqualTo("UPSERT");
    }
}
