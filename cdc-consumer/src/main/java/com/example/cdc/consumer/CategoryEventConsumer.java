package com.example.cdc.consumer;

import com.example.cdc.model.CdcEvent;
import com.example.cdc.service.CdcEventBroadcaster;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import java.util.concurrent.CompletionStage;

/**
 * Consumes category CDC events from Kafka topic dbz.public.categories
 */
@ApplicationScoped
public class CategoryEventConsumer {

    private static final Logger LOG = Logger.getLogger(CategoryEventConsumer.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Inject
    CdcEventBroadcaster broadcaster;

    @Incoming("categories-in")
    public CompletionStage<Void> consume(org.eclipse.microprofile.reactive.messaging.Message<String> message) {
        try {
            String payload = message.getPayload();

            if (payload == null || payload.isEmpty() || payload.equals("null")) {
                LOG.debug("Received tombstone or empty message for categories");
                return message.ack();
            }

            LOG.debugf("Received category event: %s", payload);

            JsonNode json = mapper.readTree(payload);

            CdcEvent event = new CdcEvent();
            event.setTable("categories");
            event.setOperation(determineOperation(json));
            event.setId(extractId(json));
            event.setPayload(json);

            broadcaster.broadcast(event);

        } catch (Exception e) {
            LOG.errorf(e, "Error processing category event: %s", message.getPayload());
        }

        return message.ack();
    }

    private String determineOperation(JsonNode json) {
        if (json.has("__deleted") && json.get("__deleted").asBoolean()) {
            return "DELETE";
        }
        if (json.has("__op")) {
            String op = json.get("__op").asText();
            switch (op) {
                case "c": return "CREATE";
                case "u": return "UPDATE";
                case "d": return "DELETE";
                case "r": return "READ";
                default: return "UNKNOWN";
            }
        }
        return "UPSERT";
    }

    private String extractId(JsonNode json) {
        if (json.has("id")) {
            return json.get("id").asText();
        }
        return "unknown";
    }
}
