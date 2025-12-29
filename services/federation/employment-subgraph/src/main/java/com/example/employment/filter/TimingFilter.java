package com.example.employment.filter;

import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;

/**
 * Vert.x route filter that measures request processing time and adds it as a response header.
 * This enables the Apollo Router and frontend to track per-subgraph latency.
 */
public class TimingFilter {

    private static final String START_TIME_KEY = "request-start-time";
    private static final String TIMING_HEADER = "X-Subgraph-Time-Ms";

    @RouteFilter(100)
    void startTimer(RoutingContext rc) {
        rc.put(START_TIME_KEY, System.currentTimeMillis());
        rc.addHeadersEndHandler(v -> {
            Long startTime = rc.get(START_TIME_KEY);
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                rc.response().putHeader(TIMING_HEADER, String.valueOf(duration));
            }
        });
        rc.next();
    }
}
