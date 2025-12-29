package com.example.cdc.api;

import com.example.cdc.DockerAvailableCondition;
import com.example.cdc.model.CdcEvent;
import com.example.cdc.service.CdcEventBroadcaster;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for EventStreamResource REST API.
 * These tests verify the REST endpoints work correctly with the broadcaster.
 * Requires Docker for Kafka DevServices.
 */
@QuarkusTest
@ExtendWith(DockerAvailableCondition.class)
class EventStreamResourceTest {

    @Inject
    CdcEventBroadcaster broadcaster;

    @Test
    void testGetHistoryEndpoint() {
        // Broadcast some events first
        broadcaster.broadcast(new CdcEvent("products", "CREATE", Map.of("id", "p1")));
        broadcaster.broadcast(new CdcEvent("categories", "UPDATE", Map.of("id", "c1")));

        given()
            .when()
            .get("/events/history")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", not(empty()))
            .body("size()", greaterThan(0));
    }

    @Test
    void testGetHistoryReturnsValidEvents() {
        CdcEvent event = new CdcEvent("products", "DELETE", Map.of("id", "test-product"));
        event.setId("unique-id-123");
        broadcaster.broadcast(event);

        given()
            .when()
            .get("/events/history")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("findAll { it.id == 'unique-id-123' }.size()", greaterThan(0));
    }

    @Test
    void testGetStatsEndpoint() {
        // Broadcast an event to ensure non-zero stats
        broadcaster.broadcast(new CdcEvent("products", "CREATE", Map.of("id", "stat-test")));

        given()
            .when()
            .get("/events/stats")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("totalEvents", greaterThan(0))
            .body("historySize", greaterThan(0));
    }

    @Test
    void testStatsMatchesBroadcasterState() {
        long eventCount = broadcaster.getEventCount();
        int historySize = broadcaster.getEventHistory().size();

        given()
            .when()
            .get("/events/stats")
        .then()
            .statusCode(200)
            .body("totalEvents", is((int) eventCount))
            .body("historySize", is(historySize));
    }

    @Test
    void testStreamEndpointReturnsServerSentEvents() {
        // Note: Full SSE testing requires async client,
        // this test just verifies the endpoint is configured correctly
        given()
            .when()
            .get("/events/stream")
        .then()
            .statusCode(200)
            .contentType("text/event-stream");
    }

    @Test
    void testHistoryContainsExpectedEventFields() {
        CdcEvent event = new CdcEvent("categories", "CREATE", Map.of("name", "Electronics"));
        event.setId("field-test-id");
        broadcaster.broadcast(event);

        given()
            .when()
            .get("/events/history")
        .then()
            .statusCode(200)
            // Find our specific event and verify its fields
            .body("find { it.id == 'field-test-id' }.table", is("categories"))
            .body("find { it.id == 'field-test-id' }.operation", is("CREATE"))
            .body("find { it.id == 'field-test-id' }.offset", notNullValue())
            .body("find { it.id == 'field-test-id' }.timestamp", notNullValue());
    }

    @Test
    void testHistoryEventsHavePayload() {
        Map<String, Object> payload = Map.of(
            "id", "payload-test",
            "name", "Test Product",
            "price", 99.99
        );
        CdcEvent event = new CdcEvent("products", "UPDATE", payload);
        event.setId("payload-event-id");
        broadcaster.broadcast(event);

        given()
            .when()
            .get("/events/history")
        .then()
            .statusCode(200)
            .body("find { it.id == 'payload-event-id' }.payload", notNullValue())
            .body("find { it.id == 'payload-event-id' }.payload.id", is("payload-test"));
    }

    @Test
    void testStatsUpdateAfterBroadcast() {
        // Get initial stats
        int initialTotalEvents = given()
            .when()
            .get("/events/stats")
        .then()
            .statusCode(200)
            .extract()
            .path("totalEvents");

        // Broadcast a new event
        broadcaster.broadcast(new CdcEvent("products", "CREATE", Map.of("id", "new")));

        // Verify stats increased
        given()
            .when()
            .get("/events/stats")
        .then()
            .statusCode(200)
            .body("totalEvents", is(initialTotalEvents + 1));
    }
}
