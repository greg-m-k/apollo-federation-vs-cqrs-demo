package com.example.cdc.consumer;

import com.example.cdc.DockerAvailableCondition;
import com.example.cdc.model.CdcEvent;
import com.example.cdc.service.CdcEventBroadcaster;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySource;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for CategoryEventConsumer using in-memory Kafka connector.
 * Tests verify that category CDC events are correctly consumed and broadcast.
 * Requires Docker for Kafka DevServices.
 */
@QuarkusTest
@ExtendWith(DockerAvailableCondition.class)
class CategoryEventConsumerTest {

    @Inject
    @Any
    InMemoryConnector connector;

    @Inject
    CdcEventBroadcaster broadcaster;

    private InMemorySource<String> categoriesChannel;

    @BeforeEach
    void setUp() {
        categoriesChannel = connector.source("categories-in");
    }

    @Test
    void testConsumeCreateOperation() {
        long initialCount = broadcaster.getEventCount();

        String payload = """
            {
                "id": "cat-1",
                "name": "Electronics",
                "description": "Electronic devices and gadgets",
                "__op": "c"
            }
            """;

        categoriesChannel.send(payload);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(broadcaster.getEventCount()).isGreaterThan(initialCount);

            List<CdcEvent> history = broadcaster.getEventHistory();
            CdcEvent lastEvent = history.get(history.size() - 1);

            assertThat(lastEvent.getTable()).isEqualTo("categories");
            assertThat(lastEvent.getOperation()).isEqualTo("CREATE");
            assertThat(lastEvent.getId()).isEqualTo("cat-1");
        });
    }

    @Test
    void testConsumeUpdateOperation() {
        long initialCount = broadcaster.getEventCount();

        String payload = """
            {
                "id": "cat-2",
                "name": "Updated Category",
                "description": "Updated description",
                "__op": "u"
            }
            """;

        categoriesChannel.send(payload);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(broadcaster.getEventCount()).isGreaterThan(initialCount);

            List<CdcEvent> history = broadcaster.getEventHistory();
            CdcEvent lastEvent = history.get(history.size() - 1);

            assertThat(lastEvent.getTable()).isEqualTo("categories");
            assertThat(lastEvent.getOperation()).isEqualTo("UPDATE");
            assertThat(lastEvent.getId()).isEqualTo("cat-2");
        });
    }

    @Test
    void testConsumeDeleteOperationWithOpField() {
        long initialCount = broadcaster.getEventCount();

        String payload = """
            {
                "id": "cat-3",
                "name": "Deleted Category",
                "__op": "d"
            }
            """;

        categoriesChannel.send(payload);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(broadcaster.getEventCount()).isGreaterThan(initialCount);

            List<CdcEvent> history = broadcaster.getEventHistory();
            CdcEvent lastEvent = history.get(history.size() - 1);

            assertThat(lastEvent.getTable()).isEqualTo("categories");
            assertThat(lastEvent.getOperation()).isEqualTo("DELETE");
            assertThat(lastEvent.getId()).isEqualTo("cat-3");
        });
    }

    @Test
    void testConsumeDeleteOperationWithDeletedField() {
        long initialCount = broadcaster.getEventCount();

        String payload = """
            {
                "id": "cat-4",
                "name": "Deleted Category",
                "__deleted": true
            }
            """;

        categoriesChannel.send(payload);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(broadcaster.getEventCount()).isGreaterThan(initialCount);

            List<CdcEvent> history = broadcaster.getEventHistory();
            CdcEvent lastEvent = history.get(history.size() - 1);

            assertThat(lastEvent.getTable()).isEqualTo("categories");
            assertThat(lastEvent.getOperation()).isEqualTo("DELETE");
        });
    }

    @Test
    void testConsumeReadOperation() {
        long initialCount = broadcaster.getEventCount();

        String payload = """
            {
                "id": "cat-5",
                "name": "Snapshot Category",
                "__op": "r"
            }
            """;

        categoriesChannel.send(payload);

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
                "id": "cat-6",
                "name": "Upsert Category"
            }
            """;

        categoriesChannel.send(payload);

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

        categoriesChannel.send("null");

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

        categoriesChannel.send("");

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
                "id": "extracted-cat-id-456",
                "name": "Test Category"
            }
            """;

        categoriesChannel.send(payload);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<CdcEvent> history = broadcaster.getEventHistory();
            CdcEvent lastEvent = history.get(history.size() - 1);

            assertThat(lastEvent.getId()).isEqualTo("extracted-cat-id-456");
        });
    }

    @Test
    void testExtractIdReturnsUnknownWhenMissing() {
        long initialCount = broadcaster.getEventCount();

        String payload = """
            {
                "name": "No ID Category"
            }
            """;

        categoriesChannel.send(payload);

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
                "id": "cat-7",
                "name": "Full Payload Category",
                "description": "A category with all fields",
                "parentId": null,
                "active": true
            }
            """;

        categoriesChannel.send(payload);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<CdcEvent> history = broadcaster.getEventHistory();
            CdcEvent lastEvent = history.get(history.size() - 1);

            assertThat(lastEvent.getPayload()).isNotNull();
            assertThat(lastEvent.getPayload().toString()).contains("Full Payload Category");
        });
    }

    @Test
    void testCategoryEventHasCategoriesTable() {
        long initialCount = broadcaster.getEventCount();

        String payload = """
            {
                "id": "cat-table-test",
                "name": "Table Test"
            }
            """;

        categoriesChannel.send(payload);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<CdcEvent> history = broadcaster.getEventHistory();
            CdcEvent lastEvent = history.get(history.size() - 1);

            assertThat(lastEvent.getTable()).isEqualTo("categories");
        });
    }

    @Test
    void testMultipleCategoryMessagesAreProcessed() {
        long initialCount = broadcaster.getEventCount();

        String payload1 = """
            {"id": "multi-cat-1", "name": "Category 1", "__op": "c"}
            """;
        String payload2 = """
            {"id": "multi-cat-2", "name": "Category 2", "__op": "c"}
            """;

        categoriesChannel.send(payload1);
        categoriesChannel.send(payload2);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(broadcaster.getEventCount()).isGreaterThanOrEqualTo(initialCount + 2);
        });

        List<CdcEvent> history = broadcaster.getEventHistory();
        int size = history.size();

        assertThat(history.get(size - 2).getId()).isEqualTo("multi-cat-1");
        assertThat(history.get(size - 1).getId()).isEqualTo("multi-cat-2");
    }
}
