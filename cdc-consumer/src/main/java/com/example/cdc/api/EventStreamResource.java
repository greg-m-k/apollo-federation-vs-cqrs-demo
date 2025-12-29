package com.example.cdc.api;

import com.example.cdc.model.CdcEvent;
import com.example.cdc.service.CdcEventBroadcaster;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestStreamElementType;

import java.util.List;
import java.util.Map;

/**
 * REST API for CDC event streaming and querying.
 */
@Path("/events")
public class EventStreamResource {

    @Inject
    CdcEventBroadcaster broadcaster;

    /**
     * Server-Sent Events stream of CDC events.
     * Clients connect here to receive real-time updates.
     */
    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<CdcEvent> streamEvents() {
        return broadcaster.getEventStream();
    }

    /**
     * Get recent event history (last 100 events).
     */
    @GET
    @Path("/history")
    @Produces(MediaType.APPLICATION_JSON)
    public List<CdcEvent> getHistory() {
        return broadcaster.getEventHistory();
    }

    /**
     * Get event statistics.
     */
    @GET
    @Path("/stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getStats() {
        return Map.of(
            "totalEvents", broadcaster.getEventCount(),
            "historySize", broadcaster.getEventHistory().size()
        );
    }
}
