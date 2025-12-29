package com.example.cdc.consumer;

import com.example.cdc.DockerAvailableCondition;
import com.example.cdc.model.CdcEvent;
import com.example.cdc.service.CdcEventBroadcaster;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for ProductEventConsumer using in-memory Kafka connector.
 * Tests verify that product CDC events are correctly consumed and broadcast.
 * Requires Docker for Kafka DevServices.
 */
@QuarkusTest
@ExtendWith(DockerAvailableCondition.class)
class ProductEventConsumerTest {

    @Inject
    @Any
    InMemoryConnector connector;

    @Inject
    CdcEventBroadcaster broadcaster;

    private InMemorySource<String> productsChannel;

    @BeforeEach
    void setUp() {
        productsChannel = connector.source("products-in");
    }

    @Test
    void testConsumeCreateOperation() {
        long initialCount = broadcaster.getEventCount();

        String payload = """
            {
                "id": "prod-1",
                "name": "Test Product",
                "price": 29.99,
                "__op": "c"
            }
            """;

        productsChannel.send(payload);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(broadcaster.getEventCount()).isGreaterThan(initialCount);

            List<CdcEvent> history = broadcaster.getEventHistory();
            CdcEvent lastEvent = history.get(history.size() - 1);

            assertThat(lastEvent.getTable()).isEqualTo("products");
            assertThat(lastEvent.getOperation()).isEqualTo("CREATE");
            assertThat(lastEvent.getId()).isEqualTo("prod-1");
        });
    }

    @Test
    void testConsumeUpdateOperation() {
        long initialCount = broadcaster.getEventCount();

        String payload = """
            {
                "id": "prod-2",
                "name": "Updated Product",
                "price": 39.99,
                "__op": "u"
            }
            """;

        productsChannel.send(payload);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(broadcaster.getEventCount()).isGreaterThan(initialCount);

            List<CdcEvent> history = broadcaster.getEventHistory();
            CdcEvent lastEvent = history.get(history.size() - 1);

            assertThat(lastEvent.getTable()).isEqualTo("products");
            assertThat(lastEvent.getOperation()).isEqualTo("UPDATE");
            assertThat(lastEvent.getId()).isEqualTo("prod-2");
        });
    }

    @Test
    void testConsumeDeleteOperationWithOpField() {
        long initialCount = broadcaster.getEventCount();

        String payload = """
            {
                "id": "prod-3",
                "name": "Deleted Product",
                "__op": "d"
            }
            """;

        productsChannel.send(payload);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(broadcaster.getEventCount()).isGreaterThan(initialCount);

            List<CdcEvent> history = broadcaster.getEventHistory();
            CdcEvent lastEvent = history.get(history.size() - 1);

            assertThat(lastEvent.getTable()).isEqualTo("products");
            assertThat(lastEvent.getOperation()).isEqualTo("DELETE");
            assertThat(lastEvent.getId()).isEqualTo("prod-3");
        });
    }

    @Test
    void testConsumeDeleteOperationWithDeletedField() {
        long initialCount = broadcaster.getEventCount();

        String payload = """
            {
                "id": "prod-4",
                "name": "Deleted Product",
                "__deleted": true
            }
            """;

        productsChannel.send(payload);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(broadcaster.getEventCount()).isGreaterThan(initialCount);

            List<CdcEvent> history = broadcaster.getEventHistory();
            CdcEvent lastEvent = history.get(history.size() - 1);

            assertThat(lastEvent.getTable()).isEqualTo("products");
            assertThat(lastEvent.getOperation()).isEqualTo("DELETE");
        });
    }

    @Test
    void testConsumeReadOperation() {
        long initialCount = broadcaster.getEventCount();

        String payload = """
            {
                "id": "prod-5",
                "name": "Snapshot Product",
                "__op": "r"
            }
            """;

        productsChannel.send(payload);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(broadcaster.getEventCount()).isGreaterThan(initialCount);

            List<CdcEvent> history = broadcaster.getEventHistory();
            CdcEvent lastEvent = history.get(history.size() - 1);

            assertThat(lastEvent.getOperation()).isEqualTo("READ");
        });
    }

    @Test
    void testConsumeUpsertWhenNoOpField() {
        long initialCount = broadcaster.getEventCount();

        String payload = """
            {
                "id": "prod-6",
                "name": "Upsert Product",
                "price": 19.99
            }
            """;

        productsChannel.send(payload);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(broadcaster.getEventCount()).isGreaterThan(initialCount);

            List<CdcEvent> history = broadcaster.getEventHistory();
            CdcEvent lastEvent = history.get(history.size() - 1);

            assertThat(lastEvent.getOperation()).isEqualTo("UPSERT");
        });
    }

    @Test
    void testConsumeTombstoneMessageIsIgnored() {
        long initialCount = broadcaster.getEventCount();

        // Tombstone message (null or empty payload)
        productsChannel.send("null");

        // Wait a bit and verify no event was broadcast
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(broadcaster.getEventCount()).isEqualTo(initialCount);
    }

    @Test
    void testConsumeEmptyMessageIsIgnored() {
        long initialCount = broadcaster.getEventCount();

        productsChannel.send("");

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(broadcaster.getEventCount()).isEqualTo(initialCount);
    }

    @Test
    void testExtractIdFromPayload() {
        long initialCount = broadcaster.getEventCount();

        String payload = """
            {
                "id": "extracted-id-123",
                "name": "Test"
            }
            """;

        productsChannel.send(payload);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<CdcEvent> history = broadcaster.getEventHistory();
            CdcEvent lastEvent = history.get(history.size() - 1);

            assertThat(lastEvent.getId()).isEqualTo("extracted-id-123");
        });
    }

    @Test
    void testExtractIdReturnsUnknownWhenMissing() {
        long initialCount = broadcaster.getEventCount();

        String payload = """
            {
                "name": "No ID Product",
                "price": 9.99
            }
            """;

        productsChannel.send(payload);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(broadcaster.getEventCount()).isGreaterThan(initialCount);

            List<CdcEvent> history = broadcaster.getEventHistory();
            CdcEvent lastEvent = history.get(history.size() - 1);

            assertThat(lastEvent.getId()).isEqualTo("unknown");
        });
    }

    @Test
    void testPayloadIsPreservedInEvent() {
        long initialCount = broadcaster.getEventCount();

        String payload = """
            {
                "id": "prod-7",
                "name": "Full Payload Product",
                "description": "A product with all fields",
                "price": 49.99,
                "categoryId": "cat-1"
            }
            """;

        productsChannel.send(payload);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<CdcEvent> history = broadcaster.getEventHistory();
            CdcEvent lastEvent = history.get(history.size() - 1);

            assertThat(lastEvent.getPayload()).isNotNull();
            assertThat(lastEvent.getPayload().toString()).contains("Full Payload Product");
            assertThat(lastEvent.getPayload().toString()).contains("49.99");
        });
    }

    @Test
    void testMultipleMessagesAreProcessedInOrder() {
        long initialCount = broadcaster.getEventCount();

        String payload1 = """
            {"id": "order-1", "name": "First", "__op": "c"}
            """;
        String payload2 = """
            {"id": "order-2", "name": "Second", "__op": "c"}
            """;
        String payload3 = """
            {"id": "order-3", "name": "Third", "__op": "c"}
            """;

        productsChannel.send(payload1);
        productsChannel.send(payload2);
        productsChannel.send(payload3);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(broadcaster.getEventCount()).isGreaterThanOrEqualTo(initialCount + 3);
        });

        List<CdcEvent> history = broadcaster.getEventHistory();
        int size = history.size();

        // Verify last 3 events are in order
        assertThat(history.get(size - 3).getId()).isEqualTo("order-1");
        assertThat(history.get(size - 2).getId()).isEqualTo("order-2");
        assertThat(history.get(size - 1).getId()).isEqualTo("order-3");
    }
}
