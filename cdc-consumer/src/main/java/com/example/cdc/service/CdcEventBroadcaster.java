package com.example.cdc.service;

import com.example.cdc.model.CdcEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Broadcasts CDC events to all SSE subscribers.
 * Also maintains an in-memory event log for replay.
 */
@ApplicationScoped
public class CdcEventBroadcaster {

    private static final Logger LOG = Logger.getLogger(CdcEventBroadcaster.class);
    private static final int MAX_EVENT_HISTORY = 100;

    private final BroadcastProcessor<CdcEvent> processor = BroadcastProcessor.create();
    private final List<CdcEvent> eventHistory = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong eventCount = new AtomicLong(0);

    /**
     * Broadcast a new CDC event to all subscribers.
     */
    public void broadcast(CdcEvent event) {
        event.setOffset(eventCount.incrementAndGet());

        // Add to history (keep last N events)
        eventHistory.add(event);
        if (eventHistory.size() > MAX_EVENT_HISTORY) {
            eventHistory.remove(0);
        }

        LOG.infof("Broadcasting CDC event: table=%s, op=%s, id=%s",
            event.getTable(), event.getOperation(), event.getId());

        processor.onNext(event);
    }

    /**
     * Get the event stream for SSE subscribers.
     */
    public Multi<CdcEvent> getEventStream() {
        return processor;
    }

    /**
     * Get recent event history for new subscribers.
     */
    public List<CdcEvent> getEventHistory() {
        return new ArrayList<>(eventHistory);
    }

    /**
     * Get total event count.
     */
    public long getEventCount() {
        return eventCount.get();
    }
}
