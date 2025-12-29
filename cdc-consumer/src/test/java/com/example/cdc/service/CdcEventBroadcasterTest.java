package com.example.cdc.service;

import com.example.cdc.DockerAvailableCondition;
import com.example.cdc.model.CdcEvent;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CdcEventBroadcaster service.
 * Requires Docker for Kafka DevServices.
 */
@QuarkusTest
@ExtendWith(DockerAvailableCondition.class)
class CdcEventBroadcasterTest {

    @Inject
    CdcEventBroadcaster broadcaster;

    @Test
    void testBroadcastSetsOffset() {
        long initialCount = broadcaster.getEventCount();

        CdcEvent event = new CdcEvent("products", "CREATE", Map.of("id", "1"));
        broadcaster.broadcast(event);

        assertThat(event.getOffset()).isEqualTo(initialCount + 1);
        assertThat(broadcaster.getEventCount()).isEqualTo(initialCount + 1);
    }

    @Test
    void testBroadcastAddsToHistory() {
        int initialSize = broadcaster.getEventHistory().size();

        CdcEvent event = new CdcEvent("categories", "UPDATE", Map.of("id", "cat-1"));
        broadcaster.broadcast(event);

        List<CdcEvent> history = broadcaster.getEventHistory();
        assertThat(history).hasSizeGreaterThan(initialSize);
        assertThat(history.get(history.size() - 1).getTable()).isEqualTo("categories");
    }

    @Test
    void testGetEventHistoryReturnsDefensiveCopy() {
        broadcaster.broadcast(new CdcEvent("products", "CREATE", Map.of("id", "1")));

        List<CdcEvent> history1 = broadcaster.getEventHistory();
        List<CdcEvent> history2 = broadcaster.getEventHistory();

        // Should be different list instances
        assertThat(history1).isNotSameAs(history2);
    }

    @Test
    void testEventStreamReceivesBroadcastedEvents() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<CdcEvent> receivedEvent = new AtomicReference<>();

        // Subscribe to the stream
        broadcaster.getEventStream()
            .subscribe().with(event -> {
                receivedEvent.set(event);
                latch.countDown();
            });

        // Broadcast an event
        CdcEvent testEvent = new CdcEvent("products", "DELETE", Map.of("id", "999"));
        testEvent.setId("test-id");
        broadcaster.broadcast(testEvent);

        // Wait for the event to be received
        boolean received = latch.await(2, TimeUnit.SECONDS);

        assertThat(received).isTrue();
        assertThat(receivedEvent.get()).isNotNull();
        assertThat(receivedEvent.get().getId()).isEqualTo("test-id");
        assertThat(receivedEvent.get().getTable()).isEqualTo("products");
        assertThat(receivedEvent.get().getOperation()).isEqualTo("DELETE");
    }

    @Test
    void testMultipleSubscribersReceiveEvents() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        List<CdcEvent> subscriber1Events = new ArrayList<>();
        List<CdcEvent> subscriber2Events = new ArrayList<>();
        List<CdcEvent> subscriber3Events = new ArrayList<>();

        // Create three subscribers
        broadcaster.getEventStream()
            .subscribe().with(event -> {
                subscriber1Events.add(event);
                latch.countDown();
            });
        broadcaster.getEventStream()
            .subscribe().with(event -> {
                subscriber2Events.add(event);
                latch.countDown();
            });
        broadcaster.getEventStream()
            .subscribe().with(event -> {
                subscriber3Events.add(event);
                latch.countDown();
            });

        // Broadcast an event
        CdcEvent testEvent = new CdcEvent("categories", "CREATE", Map.of("name", "Test"));
        broadcaster.broadcast(testEvent);

        // Wait for all subscribers to receive
        boolean received = latch.await(2, TimeUnit.SECONDS);

        assertThat(received).isTrue();
        assertThat(subscriber1Events).hasSize(1);
        assertThat(subscriber2Events).hasSize(1);
        assertThat(subscriber3Events).hasSize(1);
    }

    @Test
    void testEventCountIncrements() {
        long startCount = broadcaster.getEventCount();

        broadcaster.broadcast(new CdcEvent("products", "CREATE", null));
        broadcaster.broadcast(new CdcEvent("categories", "UPDATE", null));
        broadcaster.broadcast(new CdcEvent("products", "DELETE", null));

        assertThat(broadcaster.getEventCount()).isEqualTo(startCount + 3);
    }

    @Test
    void testHistoryPreservesEventOrder() {
        // Clear existing history by broadcasting many events to push old ones out
        // Note: MAX_EVENT_HISTORY is 100

        // Get initial size to calculate offset
        int initialSize = broadcaster.getEventHistory().size();

        CdcEvent event1 = new CdcEvent("products", "CREATE", Map.of("order", 1));
        CdcEvent event2 = new CdcEvent("categories", "UPDATE", Map.of("order", 2));
        CdcEvent event3 = new CdcEvent("products", "DELETE", Map.of("order", 3));

        broadcaster.broadcast(event1);
        broadcaster.broadcast(event2);
        broadcaster.broadcast(event3);

        List<CdcEvent> history = broadcaster.getEventHistory();

        // Get the last 3 events
        int size = history.size();
        assertThat(size).isGreaterThanOrEqualTo(3);

        CdcEvent lastEvent = history.get(size - 1);
        CdcEvent secondLastEvent = history.get(size - 2);
        CdcEvent thirdLastEvent = history.get(size - 3);

        assertThat(thirdLastEvent.getTable()).isEqualTo("products");
        assertThat(thirdLastEvent.getOperation()).isEqualTo("CREATE");

        assertThat(secondLastEvent.getTable()).isEqualTo("categories");
        assertThat(secondLastEvent.getOperation()).isEqualTo("UPDATE");

        assertThat(lastEvent.getTable()).isEqualTo("products");
        assertThat(lastEvent.getOperation()).isEqualTo("DELETE");
    }
}
