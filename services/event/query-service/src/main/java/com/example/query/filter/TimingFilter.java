package com.example.query.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS filter that measures request processing time and adds it as a response header.
 *
 * Headers added:
 * - X-Query-Time-Ms: Total request processing time
 */
@Provider
public class TimingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String START_TIME_KEY = "request-start-time";
    private static final String TIMING_HEADER = "X-Query-Time-Ms";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        requestContext.setProperty(START_TIME_KEY, System.currentTimeMillis());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Long startTime = (Long) requestContext.getProperty(START_TIME_KEY);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            responseContext.getHeaders().add(TIMING_HEADER, String.valueOf(duration));
        }
    }
}
